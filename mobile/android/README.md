# Android клиент

Эта папка теперь содержит buildable Android-проект на Kotlin + Jetpack Compose.
Он собирает мобильный прототип ToDoList с экраном входа и большим календарём.

## Что уже есть

1. `Gradle`-проект с модулем `app`.
2. `Compose`-экраны для входа и календаря.
3. DTO/контракты для будущей синхронизации с API.

## Быстрая сборка debug APK

```bash
cd mobile/android
./gradlew assembleDebug
```

После успешной сборки APK лежит в:

```bash
mobile/android/app/build/outputs/apk/debug/app-debug.apk
```

## Установка на Android

1. Включите `Developer options` и `USB debugging` на телефоне.
2. Подключите устройство.
3. Установите APK:

```bash
cd mobile/android
./gradlew installDebug
```

Или просто перенесите `app-debug.apk` на телефон и откройте его.

## Архитектурное замечание

Не подключайте Android-приложение напрямую к PostgreSQL.
Используйте серверный API перед PostgreSQL.

Общий контракт:
`mobile/shared/ARCHITECTURE.md`

## Что доделать дальше

1. Добавить Room-сущности для `TaskDto`.
2. Реализовать `AuthRepository`.
3. Реализовать `SyncRepository`.
4. Добавить хранение токена в `DataStore`.
5. Подключить экран календаря к локальным данным и очереди синхронизации.
