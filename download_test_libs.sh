#!/bin/bash
# download_test_libs.sh - Скрипт для скачивания библиотек тестирования

echo "📦 Загрузка библиотек JUnit 5 для тестирования..."

# Создаем директорию для библиотек
mkdir -p tests/lib
cd tests/lib

echo "📥 Скачивание JUnit 5 библиотек..."

# Скачиваем JUnit Jupiter API
echo "1. Скачиваем junit-jupiter-api..."
curl -L -o junit-jupiter-api-5.9.2.jar \
     https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.9.2/junit-jupiter-api-5.9.2.jar

# Скачиваем JUnit Jupiter Engine
echo "2. Скачиваем junit-jupiter-engine..."
curl -L -o junit-jupiter-engine-5.9.2.jar \
     https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-engine/5.9.2/junit-jupiter-engine-5.9.2.jar

# Скачиваем JUnit Platform Console
echo "3. Скачиваем junit-platform-console-standalone..."
curl -L -o junit-platform-console-standalone-1.9.2.jar \
     https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.9.2/junit-platform-console-standalone-1.9.2.jar

# Скачиваем дополнительные зависимости
echo "4. Скачиваем дополнительные зависимости..."

# APIGUARDIAN (обязательная зависимость)
curl -L -o apiguardian-api-1.1.2.jar \
     https://repo1.maven.org/maven2/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar

# OPENTEST4J (для расширенных assert'ов)
curl -L -o opentest4j-1.2.0.jar \
     https://repo1.maven.org/maven2/org/opentest4j/opentest4j/1.2.0/opentest4j-1.2.0.jar

# JUnit Platform Commons
curl -L -o junit-platform-commons-1.9.2.jar \
     https://repo1.maven.org/maven2/org/junit/platform/junit-platform-commons/1.9.2/junit-platform-commons-1.9.2.jar

echo ""
echo "✅ Все библиотеки успешно скачаны!"
echo ""
echo "📁 Содержимое папки tests/lib:"
ls -la *.jar
echo ""
echo "📊 Итого скачано JAR файлов: $(ls *.jar | wc -l)"