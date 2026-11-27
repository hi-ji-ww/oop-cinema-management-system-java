from pathlib import Path
import subprocess,urllib.request,tarfile,zipfile,os,time,json,traceback
ROOT=Path(__file__).resolve().parents[2];os.chdir(ROOT)
OUT=ROOT/'evidence-ide';OUT.mkdir(exist_ok=True)
R=ROOT/'runtime-ide';R.mkdir(exist_ok=True)
result={'platform':'Eclipse JEE 2025-06 on GitHub Actions Ubuntu','passed':False}
def download(url,dest):
 print('Downloading',dest.name,flush=True)
 urllib.request.urlretrieve(url,dest)
 with tarfile.open(dest) as tar:tar.extractall(R,filter='data')
def main():
 download('https://archive.eclipse.org/technology/epp/downloads/release/2025-06/R/eclipse-jee-2025-06-R-linux-gtk-x86_64.tar.gz',R/'eclipse.tar.gz')
 download('https://dlcdn.apache.org/tomcat/tomcat-9/v9.0.124/bin/apache-tomcat-9.0.124.tar.gz',R/'tomcat.tar.gz')
 eclipse=R/'eclipse';tomcat=R/'apache-tomcat-9.0.124'
 # HTTP-only configuration for lab 1, separate from the TLS checks in lab 6.
 server_xml='''<?xml version="1.0" encoding="UTF-8"?>
<Server port="8005" shutdown="SHUTDOWN"><Service name="Catalina">
<Connector port="8080" protocol="HTTP/1.1" URIEncoding="UTF-8"/>
<Connector port="8081" protocol="HTTP/1.1" URIEncoding="UTF-8"/>
<Engine name="Catalina" defaultHost="localhost"><Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="true"/></Engine>
</Service></Server>'''
 (tomcat/'conf/server.xml').write_text(server_xml)
 (OUT/'server-lab01.xml').write_text(server_xml)
 # The helper bundle drives the normal Eclipse server APIs and records actual UI images.
 classes=R/'helper-classes';classes.mkdir(exist_ok=True)
 cp=str(eclipse/'plugins/*')
 p=subprocess.run(['javac','-proc:none','-encoding','UTF-8','-cp',cp,'-d',str(classes),str(ROOT/'ci/ide/Bootstrap.java')],capture_output=True,text=True)
 (OUT/'helper-compile.log').write_text(p.stdout+p.stderr)
 if p.returncode:raise RuntimeError(p.stderr)
 manifest='''Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: Cinema IDE integration check
Bundle-SymbolicName: ru.etu.labcheck;singleton:=true
Bundle-Version: 1.0.0
Bundle-RequiredExecutionEnvironment: JavaSE-17
Require-Bundle: org.eclipse.core.runtime,org.eclipse.core.resources,
 org.eclipse.ui,org.eclipse.jface,org.eclipse.swt,
 org.eclipse.wst.server.core,org.eclipse.jst.server.tomcat.core,
 org.eclipse.debug.core,org.eclipse.jdt.launching,org.eclipse.jdt.core
Bundle-ActivationPolicy: lazy

'''
 plugin='<plugin><extension point="org.eclipse.ui.startup"><startup class="ru.etu.labcheck.Bootstrap"/></extension></plugin>'
 drop=eclipse/'dropins/ru.etu.labcheck_1.0.0.jar';drop.parent.mkdir(exist_ok=True)
 with zipfile.ZipFile(drop,'w',zipfile.ZIP_DEFLATED) as z:
  z.writestr('META-INF/MANIFEST.MF',manifest);z.writestr('plugin.xml',plugin)
  for f in classes.rglob('*.class'):z.write(f,f.relative_to(classes).as_posix())
 workspace=Path(os.environ['RUNNER_TEMP'])/'cinema-eclipse-workspace';workspace.mkdir(exist_ok=True)
 env=os.environ.copy();env.update(LAB_EVIDENCE=str(OUT),LAB_TOMCAT=str(tomcat),LAB_SOURCE=str(ROOT))
 log=open(OUT/'eclipse-console.log','w')
 process=subprocess.Popen([str(eclipse/'eclipse'),'-nosplash','-clean','-data',str(workspace),'-vm',os.environ['JAVA_HOME']+'/bin/java','-vmargs','-Xmx1536m','-Dorg.eclipse.swt.internal.gtk.cairoGraphics=false'],env=env,stdout=log,stderr=subprocess.STDOUT)
 try:
  deadline=time.time()+480
  while not (OUT/'eclipse.ready').exists():
   if (OUT/'eclipse.failed').exists():raise RuntimeError((OUT/'eclipse.failed').read_text())
   if process.poll() is not None:raise RuntimeError('Eclipse exited '+str(process.returncode))
   if time.time()>deadline:
    subprocess.run(['import','-window','root',str(OUT/'eclipse-timeout.png')]);raise TimeoutError('Eclipse not ready')
   time.sleep(1)
  result['ports']={}
  for port in [8080,8081]:
   with urllib.request.urlopen('http://127.0.0.1:'+str(port)+'/',timeout=15) as response:result['ports'][str(port)]=response.status
  (OUT/'eclipse.stop').write_text('stop')
  deadline=time.time()+90
  while not (OUT/'eclipse.done').exists():
   if (OUT/'eclipse.failed').exists():raise RuntimeError((OUT/'eclipse.failed').read_text())
   if time.time()>deadline:raise TimeoutError('Eclipse did not stop Tomcat')
   time.sleep(1)
  result['passed']=all(v==200 for v in result['ports'].values())
 finally:
  process.terminate()
  try:process.wait(timeout=15)
  except subprocess.TimeoutExpired:process.kill()
  log.close()
  metadata=workspace/'.metadata/.log'
  if metadata.exists():(OUT/'workspace.log').write_bytes(metadata.read_bytes())
try:main()
except Exception as error:result['error']=str(error);traceback.print_exc();raise
finally:(OUT/'ide-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
