#!/bin/bash
# build_jar.sh - Создание JAR файла

echo "📦 Создание JAR файла..."

# 1. Создаем временную директорию для ресурсов
mkdir -p temp_resources
mkdir -p temp_resources/view
mkdir -p temp_resources/resources/css
mkdir -p temp_resources/resources/images
mkdir -p temp_resources/resources/audio

# 2. Копируем ресурсы
cp src/view/*.fxml temp_resources/view/
cp src/resources/css/*.css temp_resources/resources/css/ 2>/dev/null || true
cp src/resources/images/*.png temp_resources/resources/images/ 2>/dev/null || true
cp src/resources/audio/*.mp3 temp_resources/resources/audio/ 2>/dev/null || true

# 3. Создаем манифест
echo "Creating manifest..."
cat > MANIFEST.MF << 'EOF'
Manifest-Version: 1.0
Main-Class: app.MainApp
Class-Path: .
Created-By: JavaFX ToDo List App
EOF

# 4. Компилируем
echo "🔨 Компиляция..."
find src -name "*.java" > sources.txt
javac --module-path ../javafx-sdk-25.0.1/lib \
      --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.media \
      -d bin \
      @sources.txt

# 5. Создаем JAR
echo "📦 Упаковка в JAR..."
jar cfm ToDoList.jar MANIFEST.MF -C bin . -C temp_resources .

# 6. Очищаем
rm -f sources.txt
rm -rf temp_resources
rm -f MANIFEST.MF

echo "✅ JAR файл создан: ToDoList.jar"
echo ""
echo "📋 Инструкция по запуску:"
echo "1. Убедитесь, что Java установлена: java --version"
echo "2. Запустите: java --module-path ../javafx-sdk-25.0.1/lib --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.media -jar ToDoList.jar"
echo "3. Или используйте run_app.sh"