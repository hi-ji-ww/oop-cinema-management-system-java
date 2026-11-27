# Лабораторные работы по Web-программированию

Ласков Сергей, группа 4311. Предметная область — «Кинотеатр Север».
Девять работ по приложенной методичке ЛЭТИ.

| Каталог | Работа | Адрес на учебном сервере |
|---|---|---|
| lab01 | Tomcat, Maven, интеграция Eclipse, HTTP 8080/8081 | конфигурация в config/ |
| lab02 | Maven webapp, JSP, WAR | /lab02/ |
| lab03 | Сервлет, init-param cinemaName, параметр genre | /lab03/catalog |
| lab04 | ResourceBundle, русский и английский интерфейсы | /lab04/catalog?lang=en |
| lab05 | JSP-форма расчёта билетов и проверка ввода | /lab05/ |
| lab06 | BASIC, viewer/admin, HTTPS | /lab06/protected/account.jsp |
| lab07 | Cookie и HttpSession | /lab07/ |
| lab08 | GWT и серверный RPC | /lab08/ |
| lab09 | JUnit и Mockito для сервлета из lab03 | тесты src/test/ |

## Сборка и запуск

Используйте JDK 17, Maven 3.9+ и Apache Tomcat 9. Используется javax.servlet
API 4.0; для Tomcat 10+ потребуется миграция API. Eclipse JEE 2025-06
запускается с JDK 21; Java-проекты можно собирать Maven с JDK 17.

Из каталога web-labs:

```sh
mvn clean install
mvn javadoc:javadoc -Ddoclint=none
```

Скопируйте `labXX/target/labXX.war` в `webapps` Tomcat 9 и запустите сервер.
Например, откройте `http://localhost:8080/lab03/catalog?genre=drama`.
В Eclipse импортируйте каталог через Existing Maven Projects и добавьте
Tomcat 9 в Preferences → Server → Runtime Environments.

GWT 2.10.0 компилируется Maven-плагином той же версии. Для проверенного
браузера Chromium выбрана перестановка `user.agent=safari`.

## HTTPS и учебные пользователи

`lab01/config/` содержит шаблоны `server.xml` и `tomcat-users.xml`.
Замените маркеры `__GENERATE_..._PASSWORD__` собственными случайными паролями
в локальной копии. Создайте хранилище `conf/cinema.p12` командой keytool
с alias `cinema`, RSA 2048, SAN `localhost` и `127.0.0.1`.
Не публикуйте заполненную конфигурацию и закрытый ключ.

Роль viewer открывает `/lab06/protected/account.jsp`; роль admin также
открывает `/lab06/admin/panel.jsp`. Защищённые страницы используют HTTPS
на порту 8443. BASIC обрабатывается контейнером Tomcat.

## Воспроизводимая проверка

Workflow `.github/workflows/web-labs.yml` в ветке `web-programming-labs`
собирает все WAR, генерирует Javadoc и проверяет приложения по HTTP/HTTPS
и в Chromium. Пароли стенда генерируются при запуске и скрываются в логе.
Артефакт `web-labs-evidence` содержит снимки экрана, журналы, результат
Surefire, фактический список содержимого WAR и `results.json`.

Отдельная ветка `web-labs-eclipse-check` содержит workflow
`.github/workflows/web-labs-ide.yml` и `ci/ide/` для проверки установки
Tomcat в Eclipse, страниц настроек Maven и запуска/остановки Tomcat через
WTP. Артефакт `web-labs-ide-evidence` содержит реальные снимки IDE.

Это исходники и воспроизводимый учебный стенд. Постоянный публичный
Java-сервер не развёрнут; для проверки преподавателю доступен репозиторий.
