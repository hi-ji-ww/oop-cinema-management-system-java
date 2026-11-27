from pathlib import Path
import os,subprocess,time,json,shutil,tarfile,urllib.request,urllib.parse,urllib.error,ssl,base64,shlex,html,traceback,secrets
import xml.etree.ElementTree as ET
import zipfile
from playwright.sync_api import sync_playwright

ROOT=Path(__file__).resolve().parents[1];os.chdir(ROOT)
E=ROOT/'evidence';E.mkdir(exist_ok=True)
RESULT={'platform':'GitHub Actions Ubuntu 24.04','checks':[],'screenshots':[]}
VIEWER_PASSWORD=secrets.token_urlsafe(24)
ADMIN_PASSWORD=secrets.token_urlsafe(24)
STORE_PASSWORD=secrets.token_urlsafe(24)
os.environ['CINEMA_STORE_PASSWORD']=STORE_PASSWORD
for transient in [VIEWER_PASSWORD,ADMIN_PASSWORD,STORE_PASSWORD]:
 print('::add-mask::'+transient,flush=True)
def record(name,passed,detail=''):
 RESULT['checks'].append(dict(name=name,passed=bool(passed),detail=str(detail)))
 print(('PASS ' if passed else 'FAIL ')+name+' '+str(detail),flush=True)
def screenshot_root(name):
 subprocess.run(['import','-window','root',str(E/name)],check=True)
 RESULT['screenshots'].append(name)
def terminal(name,command,picture,timeout=800):
 marker=E/(name+'.exit');marker.unlink(missing_ok=True)
 log=E/(name+'.log')
 shell='set -o pipefail; '+command+' 2>&1 | tee '+shlex.quote(str(log))+'; printf "%s" "${PIPESTATUS[0]}" > '+shlex.quote(str(marker))+'; sleep 300'
 proc=subprocess.Popen(['xterm','-title',name,'-geometry','168x48+0+0','-fa','DejaVu Sans Mono','-fs','11','-e','bash','-c',shell])
 try:
  deadline=time.time()+timeout
  while not marker.exists():
   if time.time()>deadline:raise TimeoutError(name)
   time.sleep(1)
  time.sleep(1);screenshot_root(picture)
  status=int(marker.read_text())
  record(name,status==0,'exit '+str(status))
  print(log.read_text(errors='replace')[-7000:],flush=True)
  if status:raise RuntimeError(name+' failed')
 finally:proc.terminate();proc.wait(timeout=10)

def request(path,data=None,auth=None,context=None):
 url=path if path.startswith('https:') else 'http://127.0.0.1:8080/'+path
 headers={}
 if auth:headers['Authorization']='Basic '+base64.b64encode(auth.encode()).decode()
 req=urllib.request.Request(url,urllib.parse.urlencode(data).encode() if data is not None else None,headers)
 try:
  r=urllib.request.urlopen(req,context=context,timeout=15);return r.status,r.read().decode('utf-8'),dict(r.headers)
 except urllib.error.HTTPError as x:return x.code,x.read().decode('utf-8'),dict(x.headers)
def http_check(name,path,needle='',status=200,**kw):
 actual,body,headers=request(path,**kw)
 passed=actual==status and needle in body
 record(name,passed,dict(status=actual,expected=status))
 assert passed,(name,actual,body[:800])
 return body

def capture(page,name):
 page.screenshot(path=str(E/name),full_page=True,animations='disabled')
 RESULT['screenshots'].append(name)

def run():
 terminal('tools','java -version; mvn -v','lab01_tools.png',60)
 terminal('maven-install','mvn -B -Dstyle.color=never clean install','lab02_maven_install.png')
 with zipfile.ZipFile(ROOT/'lab02/target/lab02.war') as war:
  (E/'war-lab02.txt').write_text('\n'.join(war.namelist()),encoding='utf-8')
 scaffold=Path(os.environ['RUNNER_TEMP'])/'cinema-archetype-check';scaffold.mkdir(exist_ok=True)
 terminal('webapp-archetype','cd '+shlex.quote(str(scaffold))+' && mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.3.1:generate -DarchetypeGroupId=org.apache.maven.archetypes -DarchetypeArtifactId=maven-archetype-webapp -DarchetypeVersion=1.4 -DgroupId=ru.etu.laskov -DartifactId=webapp-template -Dversion=1.0.0 -DinteractiveMode=false','lab02_archetype.png',150)
 terminal('javadoc','mvn -B -Dstyle.color=never javadoc:javadoc -Ddoclint=none','lab03_javadoc_build.png',300)
 reports=list((ROOT/'lab09/target/surefire-reports').glob('TEST-*.xml'))
 assert reports,'Surefire reports missing'
 for path in reports:
  xml=ET.parse(path).getroot();RESULT['junit']={k:xml.attrib.get(k) for k in ['tests','failures','errors','skipped','time']}
  shutil.copy2(path,E/path.name)
  record('Mockito test suite',xml.attrib.get('failures')=='0' and xml.attrib.get('errors')=='0',RESULT['junit'])
 terminal('surefire-report','cat lab09/target/surefire-reports/ru.etu.cinema.CatalogServletTest.txt','lab09_mockito.png',30)
 for i in [3,4,5,6,7,8,9]:
  docs=ROOT/('lab%02d/target/site/apidocs'%i)
  dest=E/'javadoc'/('lab%02d'%i)
  for file in docs.rglob('*.html'):
   if 'ru/etu/cinema' in file.as_posix() and not any(v in file.parts for v in ['class-use']):
    out=dest/file.relative_to(docs);out.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(file,out)
 RESULT['wars']={('lab%02d'%i):[str(p.relative_to(ROOT)) for p in (ROOT/('lab%02d/target'%i)).glob('*.war')] for i in range(2,10)}
 # Isolated runtime; no production resources or user account credentials.
 runtime=ROOT/'runtime-ci';runtime.mkdir(exist_ok=True)
 url='https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.124/bin/apache-tomcat-9.0.124.tar.gz'
 archive=runtime/'tomcat.tar.gz';urllib.request.urlretrieve(url,archive)
 with tarfile.open(archive) as tar:tar.extractall(runtime,filter='data')
 tomcat=runtime/'apache-tomcat-9.0.124'
 server_xml=(ROOT/'lab01/config/server.xml').read_text().replace('__GENERATE_STORE_PASSWORD__',STORE_PASSWORD)
 users_xml=(ROOT/'lab01/config/tomcat-users.xml').read_text().replace('__GENERATE_VIEWER_PASSWORD__',VIEWER_PASSWORD).replace('__GENERATE_ADMIN_PASSWORD__',ADMIN_PASSWORD)
 (tomcat/'conf/server.xml').write_text(server_xml)
 (tomcat/'conf/tomcat-users.xml').write_text(users_xml)
 for i in range(2,10):shutil.copy2(ROOT/('lab%02d/target/lab%02d.war'%(i,i)),tomcat/'webapps'/('lab%02d.war'%i))
 command='keytool -genkeypair -alias cinema -keyalg RSA -keysize 2048 -validity 365 -storetype PKCS12 -keystore '+shlex.quote(str(tomcat/'conf/cinema.p12'))+' -storepass:env CINEMA_STORE_PASSWORD -keypass:env CINEMA_STORE_PASSWORD -dname "CN=localhost, OU=Study, O=Cinema, C=RU" -ext "SAN=dns:localhost,ip:127.0.0.1"'
 terminal('keytool',command,'lab06_keytool.png',60)
 pem=runtime/'localhost.pem'
 subprocess.run(['keytool','-exportcert','-rfc','-alias','cinema','-keystore',str(tomcat/'conf/cinema.p12'),'-storepass:env','CINEMA_STORE_PASSWORD','-file',str(pem)],check=True)
 # Trust only the certificate generated for this ephemeral localhost test.
 nss=Path.home()/'.pki/nssdb';nss.mkdir(parents=True,exist_ok=True)
 if not (nss/'cert9.db').exists():subprocess.run(['certutil','-N','--empty-password','-d','sql:'+str(nss)],check=True)
 subprocess.run(['certutil','-A','-d','sql:'+str(nss),'-n','Cinema CI localhost','-t','C,,','-i',str(pem)],check=True)
 env=os.environ.copy();env.update(CATALINA_HOME=str(tomcat),CATALINA_BASE=str(tomcat))
 server_log=open(E/'tomcat.log','w')
 server=subprocess.Popen(['bash',str(tomcat/'bin/catalina.sh'),'run'],env=env,stdout=server_log,stderr=subprocess.STDOUT)
 try:
  for attempt in range(90):
   try:
    status,_,_=request('lab02/')
    if status==200:break
   except Exception:pass
   time.sleep(1)
  else:raise RuntimeError('Tomcat not ready')
  http_check('JSP home','lab02/','Кинотеатр Север')
  http_check('Servlet all','lab03/catalog','Найдено сеансов: 3')
  http_check('Servlet drama','lab03/catalog?genre=drama','Найдено сеансов: 2')
  http_check('Servlet comedy','lab03/catalog?genre=comedy','Найдено сеансов: 1')
  http_check('Servlet invalid','lab03/catalog?genre=bad',status=400)
  http_check('Russian resources','lab04/catalog?lang=ru','Афиша кинотеатра Север')
  http_check('English resources','lab04/catalog?lang=en','Sever Cinema Listings')
  http_check('Ordinary tickets','lab05/','1200.00',data={'count':'3'})
  http_check('Student tickets','lab05/','960.00',data={'count':'3','student':'on'})
  http_check('Invalid ticket count','lab05/','от 1 до 10',data={'count':'0'})
  http_check('Non-numeric ticket count','lab05/','Введите целое',data={'count':'abc'})
  tls=ssl.create_default_context(cafile=str(pem));secure='https://127.0.0.1:8443/lab06/'
  http_check('HTTPS unauthenticated',secure+'protected/account.jsp',status=401,context=tls)
  http_check('Viewer cabinet',secure+'protected/account.jsp','Личный кабинет зрителя',auth='viewer:'+VIEWER_PASSWORD,context=tls)
  http_check('Viewer forbidden admin',secure+'admin/panel.jsp',status=403,auth='viewer:'+VIEWER_PASSWORD,context=tls)
  http_check('Admin panel',secure+'admin/panel.jsp','Панель администратора',auth='admin:'+ADMIN_PASSWORD,context=tls)
  with urllib.request.urlopen('http://127.0.0.1:8081/lab02/') as r:record('Second HTTP port',r.status==200)
  with sync_playwright() as pw:
   browser=pw.chromium.launch(headless=True,args=['--disable-gpu','--disable-dev-shm-usage'])
   context=browser.new_context(viewport={'width':1100,'height':800},device_scale_factor=1)
   page=context.new_page()
   for url,name in [('http://127.0.0.1:8080/lab02/','lab02_app.png'),('http://127.0.0.1:8081/lab02/','lab01_port8081.png'),('http://127.0.0.1:8080/lab03/catalog','lab03_all.png'),('http://127.0.0.1:8080/lab03/catalog?genre=drama','lab03_drama.png'),('http://127.0.0.1:8080/lab04/catalog?lang=ru','lab04_ru.png'),('http://127.0.0.1:8080/lab04/catalog?lang=en','lab04_en.png')]:
    page.goto(url);capture(page,name)
   page.goto('http://127.0.0.1:8080/lab05/');page.locator('[name=count]').fill('3');page.locator('[name=student]').check();page.get_by_role('button',name='Рассчитать').click();page.get_by_text('960.00 руб.').wait_for();capture(page,'lab05_success.png')
   page.locator('[name=count]').fill('0');page.get_by_role('button',name='Рассчитать').click();page.get_by_text('Количество билетов должно быть от 1 до 10').wait_for();capture(page,'lab05_error.png')
   auth=browser.new_context(viewport={'width':1100,'height':800},http_credentials={'username':'viewer','password':VIEWER_PASSWORD})
   secured=auth.new_page();secured.goto(secure+'protected/account.jsp');secured.get_by_text('Личный кабинет зрителя').wait_for();capture(secured,'lab06_viewer.png')
   response=secured.goto(secure+'admin/panel.jsp');record('Browser viewer authorization',response.status==403);capture(secured,'lab06_forbidden.png');auth.close()
   admin=browser.new_context(viewport={'width':1100,'height':800},http_credentials={'username':'admin','password':ADMIN_PASSWORD})
   ap=admin.new_page();ap.goto(secure+'admin/panel.jsp');ap.get_by_text('Панель администратора').wait_for();capture(ap,'lab06_admin.png');admin.close()
   page.goto('http://127.0.0.1:8080/lab07/');page.locator('[name=name]').fill('Сергей');page.locator('[name=color]').fill('#dceeff');page.get_by_role('button',name='Сохранить').click();page.get_by_text('Количество отправок формы в сессии: 1').wait_for();capture(page,'lab07_first.png')
   page.get_by_role('link',name='Изменить настройки').click();page.locator('[name=name]').fill('Иван');page.locator('[name=color]').fill('#ffddcc');page.get_by_role('button',name='Сохранить').click();page.get_by_text('Количество отправок формы в сессии: 2').wait_for();capture(page,'lab07_second.png');page.reload();assert 'сессии: 2' in page.inner_text('body');record('Browser session counter',True)
   cookies=[{k:v for k,v in cookie.items() if k in ['name','value','domain','path','httpOnly','secure','expires']} for cookie in context.cookies() if cookie['name'] in ['cinemaName','cinemaColor']]
   (E/'cookies.json').write_text(json.dumps(cookies,ensure_ascii=False,indent=2),encoding='utf-8')
   terminal('browser-cookie-store','cat evidence/cookies.json','lab07_browser_cookies.png',30)
   record('Browser persistent cookies',len(cookies)==2 and any(c['name']=='cinemaName' and urllib.parse.unquote_plus(c['value'])=='Иван' for c in cookies))
   page.goto('http://127.0.0.1:8080/lab08/');page.get_by_text('Найдено сеансов: 3').wait_for(timeout=60000);capture(page,'lab08_all.png')
   page.locator('select').select_option('drama');page.get_by_role('button',name='Показать').click();page.get_by_text('Найдено сеансов: 2').wait_for();capture(page,'lab08_drama.png');record('GWT RPC drama',True)
   page.locator('select').select_option('comedy');page.get_by_role('button',name='Показать').click();page.get_by_text('Найдено сеансов: 1').wait_for();capture(page,'lab08_comedy.png');record('GWT RPC comedy',True)
   browser.close()
 finally:
  server.terminate();server.wait(timeout=20);server_log.close()
 record('All HTTP and browser checks',True)

try:run()
except Exception as error:
 RESULT['error']=str(error);traceback.print_exc();raise
finally:
 (E/'results.json').write_text(json.dumps(RESULT,ensure_ascii=False,indent=2),encoding='utf-8')
