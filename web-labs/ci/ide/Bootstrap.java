package ru.etu.labcheck;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.wst.server.core.*;
import org.eclipse.jst.server.tomcat.core.internal.TomcatServer;

/** Reproducible IDE integration check on an isolated CI workspace. */
public final class Bootstrap implements IStartup {
 private static final java.nio.file.Path OUT=Paths.get(System.getenv("LAB_EVIDENCE"));
 private static final String TOMCAT=System.getenv("LAB_TOMCAT");
 private static volatile IServer server;
 private static void log(String text) {
  try {Files.writeString(OUT.resolve("eclipse-check.log"),text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ignored){}
 }
 private static void snap(String name) {
  Display d=Display.getDefault();Rectangle bounds=d.getBounds();
  Image image=new Image(d,bounds.width,bounds.height);GC gc=new GC(d);
  try {gc.copyArea(image,0,0);ImageLoader loader=new ImageLoader();loader.data=new ImageData[]{image.getImageData()};loader.save(OUT.resolve(name).toString(),SWT.IMAGE_PNG);}
  finally {gc.dispose();image.dispose();}
 }
 private static void ui(Runnable action) {Display.getDefault().syncExec(action);}
 @Override public void earlyStartup() {
  Display.getDefault().timerExec(8000,()->new Thread(()->{
   try {execute();}catch(Throwable e){StringWriter s=new StringWriter();e.printStackTrace(new PrintWriter(s));log(s.toString());ui(()->snap("lab01_eclipse_error.png"));try{Files.writeString(OUT.resolve("eclipse.failed"),s.toString());}catch(Exception ignored){}}
  },"Lab IDE verification").start());
 }
 private void execute() throws Exception {
  Files.createDirectories(OUT);
  ui(()->{
   try {
    IWorkbench wb=PlatformUI.getWorkbench();IWorkbenchWindow window=wb.getActiveWorkbenchWindow();
    if(wb.getIntroManager().getIntro()!=null)wb.getIntroManager().closeIntro(wb.getIntroManager().getIntro());
    window.getShell().setBounds(0,0,1500,940);
    wb.showPerspective("org.eclipse.jst.j2ee.J2EEPerspective",window);
    window.getActivePage().showView("org.eclipse.wst.server.ui.ServersView");
   }catch(Exception e){throw new RuntimeException(e);}
  });
  // A regular resource project displays the exact Maven source files in the IDE.
  IWorkspace workspace=ResourcesPlugin.getWorkspace();
  IProject project=workspace.getRoot().getProject("cinema-web-labs");
  if(!project.exists()){
   IProjectDescription description=workspace.newProjectDescription(project.getName());
   description.setLocation(new org.eclipse.core.runtime.Path(System.getenv("LAB_SOURCE")));
   project.create(description,new NullProgressMonitor());project.open(new NullProgressMonitor());
  }
  IRuntimeType rt=ServerCore.findRuntimeType("org.eclipse.jst.server.tomcat.runtime.90");
  if(rt==null)throw new IllegalStateException("Tomcat 9 runtime adapter missing");
  IRuntimeWorkingCopy rw=rt.createRuntime("cinema-tomcat9-runtime",new NullProgressMonitor());
  rw.setName("Apache Tomcat 9 Cinema");rw.setLocation(new org.eclipse.core.runtime.Path(TOMCAT));
  IRuntime runtime=rw.save(true,new NullProgressMonitor());
  log("RUNTIME "+runtime.getName()+" "+runtime.getLocation());
  IProject servers=workspace.getRoot().getProject("Servers");if(!servers.exists())servers.create(null);if(!servers.isOpen())servers.open(null);
  IFolder conf=servers.getFolder("Tomcat9-config");if(!conf.exists())conf.create(true,true,null);
  try(var files=Files.list(Paths.get(TOMCAT,"conf"))){
   for(java.nio.file.Path file:files.toList())if(Files.isRegularFile(file))Files.copy(file,Paths.get(conf.getLocation().toOSString(),file.getFileName().toString()),StandardCopyOption.REPLACE_EXISTING);
  }
  conf.refreshLocal(IResource.DEPTH_INFINITE,null);
  IServerType type=ServerCore.findServerType("org.eclipse.jst.server.tomcat.90");
  IServerWorkingCopy sw=type.createServer("cinema-tomcat9-server",null,new NullProgressMonitor());
  sw.setName("Tomcat 9 Cinema at localhost");sw.setHost("localhost");sw.setRuntime(runtime);sw.setServerConfiguration(conf);
  TomcatServer tomcat=(TomcatServer)sw.loadAdapter(TomcatServer.class,null);
  tomcat.setTestEnvironment(false);tomcat.setInstanceDirectory(TOMCAT);tomcat.setDeployDirectory("webapps");
  server=sw.save(true,new NullProgressMonitor());
  server.publish(IServer.PUBLISH_FULL,new NullProgressMonitor());
  log("SERVER configured "+server.getId());
  showPreference("runtime", "lab01_eclipse_runtime.png");
  showPreference("maven", "lab01_eclipse_maven.png");
  server.start(ILaunchManager.RUN_MODE,new NullProgressMonitor());
  long deadline=System.currentTimeMillis()+120000;
  while(server.getServerState()!=IServer.STATE_STARTED&&System.currentTimeMillis()<deadline)Thread.sleep(500);
  if(server.getServerState()!=IServer.STATE_STARTED)throw new IllegalStateException("WTP server did not reach STARTED");
  log("STARTED through Eclipse WTP API");
  Thread.sleep(3000);ui(()->snap("lab01_eclipse_started.png"));
  Files.writeString(OUT.resolve("eclipse.ready"),"STARTED");
  deadline=System.currentTimeMillis()+180000;
  while(!Files.exists(OUT.resolve("eclipse.stop"))&&System.currentTimeMillis()<deadline)Thread.sleep(500);
  server.stop(false);deadline=System.currentTimeMillis()+60000;
  while(server.getServerState()!=IServer.STATE_STOPPED&&System.currentTimeMillis()<deadline)Thread.sleep(500);
  if(server.getServerState()!=IServer.STATE_STOPPED)throw new IllegalStateException("WTP server did not stop");
  Thread.sleep(2000);ui(()->snap("lab01_eclipse_stopped.png"));log("STOPPED through Eclipse WTP API");
  Files.writeString(OUT.resolve("eclipse.done"),"STARTED and STOPPED");
 }
 private static void showPreference(String kind,String picture) throws Exception {
  final String[] id={null};
  for(IConfigurationElement c:Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.ui.preferencePages")){
   String clazz=Objects.toString(c.getAttribute("class"),"");String name=Objects.toString(c.getAttribute("name"),"");
   if(kind.equals("runtime")&&clazz.endsWith("RuntimePreferencePage")&&c.getContributor().getName().contains("server"))id[0]=c.getAttribute("id");
   if(kind.equals("maven")&&clazz.contains("MavenInstallationsPreferencePage"))id[0]=c.getAttribute("id");
  }
  log("PREFERENCE "+kind+" "+id[0]);
  if(id[0]==null)throw new IllegalStateException("Preference page not found: "+kind);
  ui(()->{
   PreferenceDialog dialog=PreferencesUtil.createPreferenceDialogOn(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),id[0],null,null);
   Display.getDefault().timerExec(3000,()->{snap(picture);dialog.close();});dialog.open();
  });
 }
}
