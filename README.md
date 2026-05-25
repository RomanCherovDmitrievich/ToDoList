# ToDoList (JavaFX 25.0.2)

Кроссплатформенный desktop ToDo-планировщик на Java + JavaFX с SQLite/PostgreSQL/MySQL/Firebird, аккаунтами пользователей, системными уведомлениями, большим календарём и циклическим плейлистом.

## О проекте

ToDoList состоит из нескольких слоёв:

- `src/app` запускает JavaFX-приложение.
- `src/view` и `src/viewmodel` отвечают за интерфейс, вкладки и поведение экрана.
- `src/model` хранит доменные сущности задач, пользователей, тегов и статистики.
- `src/repository` управляет SQLite/PostgreSQL/MySQL/Firebird, миграциями, DAO-слоем и сохранением данных.
- `src/util` содержит вспомогательные сервисы: авторизация, email, уведомления, темы, музыка, CLI и генерация тестовых отчётов.

Как это работает в целом:

- при старте приложение поднимает локальную среду, подключается к SQLite или PostgreSQL по `data/db.properties`;
- пользователь входит по логину или email, и дальше видит только свои задачи;
- задачи можно смотреть списком, по календарю, по статистике и экспортировать в `.ics`;
- уведомления, музыка, темы и часть интеграций работают через отдельные сервисы в `src/util`;
- тесты компилируют проект, запускают JUnit и формируют HTML-отчёт в `tests/reports/`.

Статус по Windows:

- в коде есть Windows-ветка для путей данных и системных команд;
- для Windows оставлены только `setup.bat` и `run.bat`;
- `run.bat` ищет JDK через `JAVA_HOME`, `where javac` и типовые папки `Program Files`, а `JavaFX SDK` ищет через `JAVAFX_HOME` или `javafx-sdk-*` рядом с проектом.

## Требования

- `JDK 21+`.
- Для desktop-части рекомендован `JavaFX SDK 25.0.2`.
- Для Android mobile-сборки лучше использовать `JDK 21`, потому что текущий Android Gradle Plugin стабильно собирался именно на нём.
- Для iPhone-сборки нужен `Xcode` и `xcodebuild`.
- Для Android-сборки нужны `Android SDK`, `platform-tools`, `build-tools` и `Gradle wrapper`.

Нужные JAR-файлы для desktop-приложения:

- `lib/gson-2.10.1.jar`
- `lib/sqlite-jdbc-3.45.1.0.jar`
- `lib/slf4j-api-2.0.12.jar`
- `lib/slf4j-nop-2.0.12.jar`
- `lib/postgresql-42.7.4.jar`
- `lib/mysql-connector-j-9.x.x.jar` или совместимый `mysql-connector-j-8.x.x.jar` для MySQL-режима
- `lib/jaybird.jar` или совместимый `jaybird-4.x/5.x.jar` для Firebird / Ред БД
- `lib/jakarta.mail-api-2.1.5.jar`
- `lib/angus-mail-2.0.5.jar`
- `lib/jakarta.activation-api-2.1.4.jar`
- `lib/angus-activation-2.0.3.jar`

Что нужно подготовить заранее:

- распаковать `javafx-sdk-25.0.2` в корень проекта или выставить `JAVAFX_HOME`;
- для Linux/RedOS/macOS используются корневые shell-скрипты;
- для Windows используются только `setup.bat` и `run.bat`;
- для отправки email через SMTP при необходимости заполнить `data/email.properties`.

## Запуск

### Подготовка зависимостей

macOS / Linux / RedOS:

```bash
./setup.sh
```

Windows CMD:

```bat
setup.bat
```

### Запуск desktop-приложения

macOS / Linux / RedOS:

```bash
./run.sh
```

Windows CMD:

```bat
run.bat
```

Скрипт `run.bat` запускается напрямую через `cmd.exe` и не требует `PowerShell`.

Если нужно указать собственную папку с музыкой:

macOS / Linux / RedOS:

```bash
export TODOLIST_AUDIO_DIR="/audio"
./run.sh
```

### Запуск тестов

macOS / Linux / RedOS:

```bash
./run_test.sh
```

После выполнения тестов:

- текстовый лог попадает в `tests/reports/test_results_<timestamp>.txt`;
- JUnit XML лежит в `tests/reports/junit-xml-<timestamp>/`;
- красивый HTML-отчёт генерируется в `tests/reports/test_report_<timestamp>.html`.

## Что реализовано

- SQLite/PostgreSQL/MySQL/Firebird хранилище (6 таблиц): `users`, `tasks`, `tags`, `task_tags`, `task_statistics`, `app_settings`.
- Авторизация:
  - вход по логину или email и паролю;
  - один и тот же email можно использовать у нескольких аккаунтов;
  - если email повторяется у нескольких аккаунтов, для входа и сброса пароля нужно использовать логин;
  - отдельные задачи для каждого пользователя;
  - восстановление доступа по коду на email;
  - если системная отправка почты не настроена, код попадает в `notifications_outbox.log`.
- Темы: **Summer**, **Dark**, **Ocean**, **Custom** (сохранение кастомной темы в файл).
- Вкладки:
  - **Задачи** — основной список с редактированием на месте.
  - **Календарь** — большая месячная сетка + отдельный список задач на выбранный день + экспорт в `.ics`.
  - **Статистика** — графики по статусам, категориям, приоритетам и 7 дням.
  - **Настройки** — темы, уведомления, музыка, виджет.
- Редактирование задач на месте (title/description/priority/category).
- Контекстное меню в таблице: редактирование, копирование, напоминание, удаление.
- Drag-and-drop для ручного порядка задач.
- Повторяющиеся задачи: ежедневные/еженедельные/по будням/ежемесячные + RRULE.
- Настраиваемые уведомления: всплывающее окно, звук, email, глобальное/персональное время напоминаний.
- Виджет поверх окон с топ-5 срочными задачами.
- Циклический плейлист:
  - читает все треки из папки `/audio` или `audio` в проекте;
  - автоматически переключает по кругу;
  - добавленные треки подхватываются при следующем запуске.
- Оптимизация нагрузки:
  - удалена тяжёлая кадровая анимация;
  - напоминания проверяются 1 раз в минуту в отдельном планировщике;
  - минимум лишнего I/O.

## Структура

- `src/app` — запуск JavaFX.
- `src/model` — модели и менеджер задач.
- `src/repository` — DAO, фабрика соединений и конфиг подключения для SQLite/PostgreSQL/MySQL/Firebird.
- `src/view`, `src/viewmodel` — UI и ViewModel.
- `src/util` — JSON, аудио, уведомления, напоминания, темы, авторизация, CLI.
- `tables.sql` — SQL схема БД.
- Скрипты проекта:
  - `setup.sh`, `run.sh`, `run_test.sh`, `generate_doxygen.sh`
  - `setup.bat`, `run.bat`
- `phone.md` — план переноса на Android/iPhone и оптимизации под телефоны.
- `mobile/android` — buildable Android-клиент на Kotlin/Compose.
- `mobile/ios` — buildable iPhone-клиент на Swift/SwiftUI через `XcodeGen`.
- `mobile/shared/ARCHITECTURE.md` — общий контракт аккаунтов, задач и offline-first sync.

## Подключение к серверной БД

По умолчанию приложение работает с локальной SQLite. Для PostgreSQL, MySQL, Firebird и других JDBC-подключений используйте файл `data/db.properties`:

```bash
cp data/db.properties.example data/db.properties
```

Что и где менять:

- `data/db.properties.example`: здесь задаются `db.type`, JDBC-параметры PostgreSQL/MySQL/Firebird и резервный `custom`-режим.
- `src/repository/DatabaseConfig.java`: читает файл/ENV, валидирует параметры и собирает JDBC URL для `sqlite`, `postgresql`, `mysql`, `firebird`.
- `src/repository/ConnectionFactory.java`: единая фабрика JDBC-соединений.
- `src/repository/SqlDialect.java`: различия SQL между SQLite, PostgreSQL и MySQL.
- `src/repository/TaskDao.java`: единый DAO-интерфейс для задач.
- `src/repository/DatabaseManager.java`: реализация DAO, создание таблиц, миграции, UPSERT задач, пользователи и настройки.
- `src/util/DatabaseCli.java`: CLI-утилита для проверки подключения и генерации данных.

Подробная инструкция и разбор ошибок: `подключение_к_БД.md`

## Режим двух СУБД для курса БД

Для учебного задания проект подготовлен под две серверные реляционные СУБД:

- `PostgreSQL`
- `MySQL`

Дополнительно добавлен режим `Firebird / Ред БД`, чтобы на защите можно было переключать СУБД одной строкой в конфиге.

Для этого `DatabaseConfig` поддерживает отдельные профили:

- `db.postgresql.*`
- `db.mysql.*`
- `db.firebird.*`

Если они заполнены, то на практике для переключения достаточно менять только `db.type`.

Как это устроено:

- один интерфейс доступа к задачам задаётся через `src/repository/TaskDao.java`;
- подключение к конкретной СУБД выбирается в `data/db.properties`;
- `ConnectionFactory` открывает JDBC-соединение;
- `SqlDialect` изолирует различия в `UPSERT`, `INSERT IGNORE`, автоинкременте и миграциях;
- `DatabaseManager` остаётся общей DAO-реализацией для всех поддержанных режимов.

Готовые SQL-схемы лежат отдельно:

- `sql/postgresql_schema.sql`
- `sql/mysql_schema.sql`
- `sql/firebird_schema.sql`

Главные различия между PostgreSQL и MySQL в проекте:

- автоинкремент: `BIGSERIAL` в PostgreSQL и `BIGINT AUTO_INCREMENT` в MySQL;
- upsert: `ON CONFLICT ... DO UPDATE` в PostgreSQL и `ON DUPLICATE KEY UPDATE` в MySQL;
- вставка без дублей: `ON CONFLICT DO NOTHING` / `INSERT IGNORE`;
- текстовые типы: `TEXT` в PostgreSQL и `LONGTEXT` в MySQL;
- временные поля в учебных SQL-скриптах: `TIMESTAMP` в PostgreSQL и `DATETIME` в MySQL;
- зарезервированные слова: в MySQL поле `key` экранируется как `` `key` ``.

## Аккаунты пользователей

- При первом запуске приложение просит создать первый аккаунт.
- Потом можно входить:
  - по логину;
  - по email, если этот email привязан только к одному аккаунту.
- У каждого пользователя свой набор задач.
- Кнопка `Сменить пользователя` находится в верхней панели.

### Восстановление пароля

- В приложении реализован **сброс пароля по коду**, а не отправка старого пароля из БД.
- Это сделано намеренно: пароль хранится в виде хеша и не должен отправляться в открытом виде.
- Для стабильной отправки теперь можно использовать отдельный SMTP-аккаунт через `data/email.properties`.
- Шаблон лежит в `data/email.properties.example`.
- Режимы отправки:
  - `auto` — сначала SMTP, потом системный канал;
  - `smtp` — только SMTP;
  - `system` — только `Mail.app`/`mail`;
  - `outbox` — только запись в `notifications_outbox.log`.
- Практический вариант для рассылки:
  - создать отдельный ящик вроде `noreply@...`;
  - вписать его SMTP-настройки в `data/email.properties`;
  - не использовать личную почту и не зависеть от `Mail.app`.
- Если почтовая отправка не настроена, код восстановления сохраняется в `notifications_outbox.log`.

## Календарь и синхронизация

- На вкладке **Календарь** теперь есть:
  - большая месячная сетка;
  - быстрый переход по месяцам;
  - отдельный список задач на выбранный день;
  - экспорт в `todolist_calendar.ics`.
- Импортируйте файл в системный календарь:
  - macOS Calendar: **File → Import…**
  - Windows Outlook/Calendar: импорт `.ics`
  - Linux GNOME Calendar: импорт `.ics`

Это базовая интеграция без прямого API, но позволяет быстро синхронизировать задачи.

## Повторяющиеся задачи (RRULE)

Примеры:
- `RRULE:FREQ=MONTHLY;BYDAY=TU;BYSETPOS=2` — каждый второй вторник месяца
- `RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE,FR` — каждые две недели по пн/ср/пт

Если RRULE не указан — задача одноразовая.

## Уведомления

- Всплывающие уведомления работают кроссплатформенно (на macOS через `osascript`).
- Звук — системный beep.
- Email:
  - сначала может использоваться выделенный SMTP-аккаунт из `data/email.properties`
  - macOS: запасной системный канал через `Mail.app`
  - Linux/RedOS: запасной системный канал через `mail`
  - Windows: без отдельной настройки SMTP/Outlook автоматическая отправка не гарантируется
  - если отправка недоступна, письмо пишется в `notifications_outbox.log`.

## Android и iPhone

- Отдельный план мобильного переноса находится в `phone.md`.
- Фактически собранные артефакты лежат здесь:
  - Android debug APK: `mobile/android/app/build/outputs/apk/debug/app-debug.apk`
  - iPhone Simulator `.app`: `mobile/ios/build/ios-sim/Build/Products/Debug-iphonesimulator/ToDoListMobile.app`
  - iPhone device `.app`: `mobile/ios/build/ios-device/Build/Products/Debug-iphoneos/ToDoListMobile.app`
  - iPhone unsigned `.ipa`: `mobile/ios/build/ToDoListMobile-unsigned.ipa`
- Что можно сделать прямо сейчас:
  - `app-debug.apk` можно перенести на Android и установить вручную;
  - iPhone unsigned `.ipa` и `.app` полезны для проверки сборки, но не ставятся на физический iPhone без Apple-подписи.
- Общее правило для обеих платформ:
  - аккаунты и задачи должны идти через общий sync API, а не через прямое подключение к PostgreSQL;
  - локальная БД на телефоне должна быть первым источником данных;
  - серверная БД нужна для синхронизации между устройствами.
- Общая схема синхронизации описана в `mobile/shared/ARCHITECTURE.md`.

## Пути хранения данных

По умолчанию используются системные директории, но если в проекте есть папка `data/`, используется она.

- macOS: `~/Library/Application Support/ToDoList`
- RedOS/Linux: `~/.local/share/ToDoList`
- Windows: `%APPDATA%\ToDoList`
- fallback: `~/ToDoList`

## Doxygen

```bash
./generate_doxygen.sh
```

Важные страницы:

- `src/mainpage.dox`
- `src/database.dox`

## Примечания

- БД по умолчанию хранится в `todolist.db` в директории данных.
- JSON бэкап задач остаётся в `tasks.json` для совместимости.
- Кастомная тема хранится в `custom-theme.css` в директории данных.
- В репозиторий безопаснее коммитить только `data/db.properties.example`; файл `data/db.properties` лучше держать локально из-за пароля.
