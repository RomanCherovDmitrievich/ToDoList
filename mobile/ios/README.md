# iOS клиент

Эта папка теперь содержит buildable SwiftUI-проект для генерации через `XcodeGen`.
Он собирает мобильный прототип ToDoList с экраном входа и большим календарём.

## Что уже есть

1. SwiftUI-приложение `ToDoListMobile`.
2. `project.yml` для генерации `xcodeproj`.
3. Swift-модели и контракты будущей синхронизации.

## Быстрая генерация проекта

```bash
cd mobile/ios
xcodegen generate
```

После этого появится:

```bash
mobile/ios/ToDoListMobile.xcodeproj
```

## Сборка для iPhone Simulator

```bash
cd mobile/ios
xcodegen generate
xcodebuild \
  -project ToDoListMobile.xcodeproj \
  -scheme ToDoListMobile \
  -sdk iphonesimulator \
  -configuration Debug \
  build
```

## Сборка для физического iPhone

Для установки на настоящий iPhone нужен подписанный билд:

1. Добавьте Apple ID / Team в Xcode.
2. Укажите `DEVELOPMENT_TEAM`.
3. Соберите под `iphoneos` или через Xcode.

Без кода подписи и provisioning profile файл для iPhone нельзя просто перенести как обычный `apk`.

## Архитектурное замечание

Не подключайте iPhone-приложение напрямую к PostgreSQL.
Используйте серверный API перед PostgreSQL.

Общий контракт:
`mobile/shared/ARCHITECTURE.md`

## Что доделать дальше

1. Добавить локальное хранение данных с помощью SwiftData или SQLite.
2. Реализовать `AuthRepository`.
3. Реализовать `SyncRepository`.
4. Сохранять access/refresh токены в Keychain.
5. Привязать экран календаря к локальному кэшу задач и очереди синхронизации.
