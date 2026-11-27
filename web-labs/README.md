# Кинотеатр Лабораторные работы по web программированию

Ласков Сергей, группа 4311. Девять работ по пособию ЛЭТИ 2022 года.

## Состав

- lab01 — конфигурация локального Tomcat, два HTTP порта и HTTPS.
- lab02 — web проект Maven и JSP стартовая страница.
- lab03 — сервлет афиши, init-param cinemaName, параметр genre.
- lab04 — русский и английский интерфейсы из ResourceBundle.
- lab05 — JSP форма расчета стоимости билетов и проверка ввода.
- lab06 — BASIC аутентификация, роли viewer/admin, HTTPS.
- lab07 — сохранение настроек в Cookie и счетчик HttpSession.
- lab08 — Java клиент GWT, RPC и серверная фильтрация афиши.
- lab09 — шесть JUnit тестов сервлета на Mockito.

## Сборка

Нужны JDK 11 или выше, Maven 3.9 и Tomcat 9. Для Mockito 5 требуется Java 11 или выше.
Используются javax.servlet, поэтому Tomcat 10 и выше без миграции не подходят.

Из корня: `mvn clean install`.
Документация: `mvn javadoc:javadoc -Ddoclint=none`.
Скопировать `labXX/target/labXX.war` в `webapps` Tomcat 9.
Открыть `http://localhost:8080/lab03/catalog?genre=drama`.
GWT собирается плагином в lab08; включен браузерный вариант safari для Chromium.

Учебные учетные записи приведены в `lab01/config/tomcat-users.xml`.
Они предназначены исключительно для локального стенда.
Хранилище сертификата создается локально командой keytool; приватный ключ не публикуется.
В server.xml Certificate используется `conf/cinema.p12` с паролем учебного стенда.

Для запуска из Eclipse импортировать Existing Maven Projects. JDK и Maven задать
в Preferences, Tomcat 9 добавить в Server Runtime Environments в Eclipse Enterprise Java.
При использовании Eclipse Java Developers можно установить официальный Web Tools Platform.

Результаты фактических проверок и ограничения фиксируются отдельно в verification.json.
