# Мобильная версия ToDoList

## Что уже подготовлено в проекте

- Локальная SQLite и серверная PostgreSQL уже поддерживаются в одном коде.
- Добавлены пользовательские аккаунты: логин/email + пароль, разделение задач по владельцу, восстановление доступа по коду.
- Календарная вкладка переделана в большую месячную сетку с отдельным списком задач по выбранному дню.
- Базовая offline-first идея уже подходит проекту: локальная БД может быть главным источником данных, а сервер использоваться для синхронизации.
- Добавлены отдельные каталоги для нативных клиентов:
  - `mobile/android`
  - `mobile/ios`
  - `mobile/shared/ARCHITECTURE.md`
- Добавлены скрипты сборки:
  - `scripts/build_android_mobile.sh`
  - `scripts/build_ios_mobile.sh`

## Что важно понимать честно

Текущий репозиторий всё ещё является **desktop JavaFX-приложением**, но мобильный контур теперь смещён в сторону **отдельных нативных клиентов**. Это означает:

- Android/iPhone версия не может считаться "полностью готовой" без отдельного backend sync API.
- Мобильные клиенты уже имеют стартовую структуру, но им всё ещё нужен реальный серверный слой для общей авторизации и синхронизации задач.
- В текущем окружении уже подтверждено:
  - `Xcode 26.2` установлен;
  - `xcodegen` установлен и проект `mobile/ios` реально собирается;
  - Android SDK platform tools / build tools установлены;
  - проект `mobile/android` реально собирается в `apk`;
  - проект `mobile/ios` реально собирается в `.app`, а также упаковывается в unsigned `.ipa`.

Итог: мобильный контур уже не теоретический, а реально собираемый, но это пока **прототип клиентов**, а не готовый production-релиз с полной серверной синхронизацией.

## Какие файлы уже собраны

- Android:
  - `mobile/android/app/build/outputs/apk/debug/app-debug.apk`
- iPhone:
  - `mobile/ios/build/ios-sim/Build/Products/Debug-iphonesimulator/ToDoListMobile.app`
  - `mobile/ios/build/ios-device/Build/Products/Debug-iphoneos/ToDoListMobile.app`
  - `mobile/ios/build/ToDoListMobile-unsigned.ipa`

Важно:

- `app-debug.apk` можно установить на Android вручную.
- `ToDoListMobile-unsigned.ipa` не установится на физический iPhone без Apple-подписи, даже если просто перенести файл на телефон.
- Для реальной установки на iPhone нужен Apple ID / Team, provisioning profile и подписанная сборка через Xcode.

## Реалистичный путь переноса

### Вариант 1. Сохраняем Java/JavaFX стек

Это самый близкий к текущему проекту путь.

1. Вынести ядро приложения в отдельный модуль:
   - `model`
   - `repository`
   - часть `util`, не завязанную на desktop-only API
2. Перевести сборку на Maven.
3. Добавить `gluonfx-maven-plugin`.
4. Разделить desktop-специфичный UI и mobile-UI.
5. Убрать или адаптировать desktop-зависимые функции:
   - системный виджет поверх окон
   - часть звуковых сценариев
   - некоторые модальные desktop-диалоги
6. Добавить мобильные профили:
   - `android`
   - `ios`
   - `ios-sim`

Плюсы:

- максимум переиспользования Java-кода;
- проще сохранить общую бизнес-логику;
- SQLite/PostgreSQL слой уже можно использовать как основу.

Минусы:

- мобильный JavaFX стек потребует отдельной упаковки и настройки;
- часть UI всё равно придётся переработать;
- мобильные ограничения по фону, батарее и touch-событиям нужно учитывать отдельно.

### Вариант 2. Оставить сервер и ядро, а мобильный клиент сделать отдельно

Например:

- Android: Kotlin
- iPhone: SwiftUI
- либо единый мобильный клиент на Flutter/React Native

Плюсы:

- лучший нативный UX на телефонах;
- легче адаптировать push, background sync, permissions и store-публикацию.

Минусы:

- это уже фактически второй фронтенд;
- дублирование части клиентской логики;
- выше стоимость разработки.

Для этого проекта выбран именно **Вариант 2**:

- Android: отдельный Kotlin/Compose клиент
- iPhone: отдельный Swift/SwiftUI клиент
- серверная БД: единая
- авторизация и синхронизация: через общий API-контур

## Ограничения по платформам

Для отдельной native-ветки ограничения уже другие:

- Android emulator и SDK можно поднимать отдельно от desktop JavaFX.
- iOS Simulator доступен через Xcode на macOS.
- Для публикации в App Store всё равно нужен Apple Developer Program.

То есть в реальном процессе обычно получается так:

- desktop-сборки можно делать на текущих машинах;
- Android native build лучше делать на Linux runner;
- iOS build и подпись делать на macOS.

## Что уже сделано для Android

- создан каталог `mobile/android`
- добавлены Kotlin-модели задач и сессии
- добавлен Compose-каркас для login/calendar
- добавлены интерфейсы `AuthRepository` и `SyncRepository`
- добавлен `Gradle`-проект и собран debug `apk`
- добавлен README со сборкой и установкой

## Что уже сделано для iPhone

- создан каталог `mobile/ios`
- добавлены Swift-модели задач и сессии
- добавлен SwiftUI-каркас для login/calendar
- добавлены протоколы `AuthRepository` и `SyncRepository`
- добавлен `project.yml` для `XcodeGen`
- собраны simulator/device `.app`
- собран unsigned `.ipa`
- добавлен README с шагами для Xcode и iPhone Simulator

## Offline-first схема для этого проекта

Для ToDo-сервиса на нескольких устройствах я рекомендую такую модель:

1. Локальная SQLite всегда является первым источником данных для UI.
2. Все действия пользователя сначала сохраняются локально.
3. Для записи на сервер создаётся очередь синхронизации.
4. При появлении сети очередь отправляется пакетами.
5. У каждой задачи должны быть:
   - глобальный UUID;
   - `owner_user_id`;
   - `updated_at`;
   - признак удаления или отдельная запись удаления.
6. На конфликте:
   - либо `last write wins`;
   - либо отдельное правило с ручным разбором для важных полей.

Для ToDo приложения чаще всего лучше всего работает:

- **lazy write**: сначала локально, потом на сервер;
- сервер подтверждает версию записи;
- при ошибке запись остаётся в локальной очереди.

## Что нужно оптимизировать под телефоны

### Интерфейс

- Не использовать тяжёлые вложенные `ScrollPane + VBox` для больших списков.
- Предпочитать виртуализированные списки (`ListView`) там, где элементов много.
- Не пытаться показывать слишком много текста в ячейке календаря.
- Для дня в месячной сетке показывать максимум 1-2 превью задачи и счётчик остатка.

### Производительность

- Не пересчитывать статистику и большие выборки на каждом мелком событии.
- Вынести тяжёлые операции БД/синхронизации из UI-потока.
- Ограничить автозапуск музыки на мобильных устройствах.
- Минимизировать число постоянно работающих таймеров.

### Сеть и батарея

- Делать batch sync вместо одиночных запросов на каждую задачу.
- Добавить retry с backoff.
- Не синхронизировать всё подряд при каждом открытии экрана.
- Использовать "грязные" записи и очередь изменений.

### Хранение

- Локальная БД должна быть каноническим слоем чтения.
- Серверная БД должна обновляться через sync-service, а не напрямую из каждого UI-экрана.

## Какие проблемы разработчики реально встречали

На форумах и в обсуждениях по Gluon/мобильной разработке повторяются одни и те же темы:

- `ScrollPane` на мобильных устройствах может вести себя тяжело и лагать на длинных списках.
  Решение: использовать более лёгкие списки и упрощённые ячейки, а в крайних случаях кастомный skin.
- Touch/WebView может вести себя по-разному на Samsung и других Android-устройствах.
  Решение: device-specific runtime args, обход через JS bridge, избегать критически важного UX на полном WebView.
- На iOS нельзя рассчитывать на "постоянный фон".
  Решение: строить sync как короткую отложенную задачу, а не как бесконечный background service.

Для вашего проекта это означает:

- календарь и списки должны оставаться лёгкими;
- авторизация и синхронизация не должны зависеть от постоянного фона;
- mobile-версию нужно проектировать как offline-first, а не как "всегда онлайн".

## Что я рекомендую делать дальше

1. Оставить текущий desktop-проект как стабильную базу.
2. Поднять минимальный sync API над PostgreSQL.
3. Вынести desktop-логику работы с задачами в общий серверный контракт.
4. Подключить Android local storage + sync queue.
5. Подключить iOS local storage + sync queue.
6. Для Android уже можно ставить `app-debug.apk` на реальные устройства и проверять UX.
7. Для iPhone следующим шагом нужно добавить подпись в Xcode и собрать уже подписанный `.ipa`.

## Источники

- [Gluon Documentation](https://docs.gluonhq.com/)
- [Android Developers: Build an offline-first app](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Android Developers: Android vitals](https://developer.android.com/topic/performance/vitals/index.html)
- [Android Developers: App orientation, aspect ratio, and resizability](https://developer.android.com/develop/ui/compose/layouts/adaptive/app-orientation-aspect-ratio-resizability)
- [Apple Developer: Refreshing and Maintaining Your App Using Background Tasks](https://developer.apple.com/documentation/backgroundtasks/refreshing-and-maintaining-your-app-using-background-tasks)
- [Apple Developer: Choosing Background Strategies for Your App](https://developer.apple.com/documentation/backgroundtasks/choosing-background-strategies-for-your-app)
- [Apple Developer: Energy Efficiency Guide for iOS Apps](https://developer.apple.com/library/archive/documentation/Performance/Conceptual/EnergyGuide-iOS/index.html)
- [Stack Overflow: Gluon Mobile ScrollPane Optimization](https://stackoverflow.com/questions/48489652/gluon-mobile-scrollpane-optimization)
- [Stack Overflow: JavaFX Webview on Mobile does not receive any TouchEvent](https://stackoverflow.com/questions/73593166/javafx-webview-on-mobile-does-not-receive-any-touchevent)
- [Stack Overflow: iOS Background service limitations](https://stackoverflow.com/questions/61750991/ios-background-service-limitations)
