# Результаты проверки приложений

Успешный запуск: [GitHub Actions, 27.11.2026](https://github.com/hi-ji-ww/oop-cinema-management-system-java/actions/runs/45977922835).

Проверенный коммит: `1770f7dda8759def078e3e2df54558f83ece820f`.
Файл `application-results.json` скопирован без изменения результатов
из артефакта этого запуска. Все проверки завершились успешно.

- Maven clean install: восемь WAR и корневой модуль.
- Генерация maven-archetype-webapp 1.4 и Javadoc.
- JUnit/Mockito: 6 тестов, 0 ошибок, 0 пропусков.
- HTTP: сервлеты, локализация, JSP-форма и второй порт.
- HTTPS: 401 без входа, 200 для разрешённой роли, 403 для запрещённой.
- Chromium: формы, Cookie, счётчик сессии и GWT RPC.

Снимки экрана и исходные журналы находятся в артефакте `web-labs-evidence`.
Интеграция Eclipse/Tomcat проверяется отдельным workflow в ветке
`web-labs-eclipse-check`.
