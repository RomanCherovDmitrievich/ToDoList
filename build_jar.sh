#!/bin/bash
# build_jar.sh - Создание JAR файла (ИСПРАВЛЕННАЯ ВЕРСИЯ с правильной структурой)

echo "📦 Создание JAR файла..."

# Удаляем старые файлы
rm -rf bin ToDoList.jar 2>/dev/null

# Создаем структуру папок
mkdir -p bin

# Компилируем все Java файлы
echo "🔨 Компиляция Java файлов..."
find src -name "*.java" > sources.txt
javac --module-path ../javafx-sdk-25.0.1/lib \
      --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.media \
      -d bin \
      @sources.txt

if [ $? -ne 0 ]; then
    echo "❌ Ошибка компиляции!"
    rm -f sources.txt
    exit 1
fi

# Проверяем MainApp
if [ ! -f "bin/app/MainApp.class" ]; then
    echo "❌ Ошибка: MainApp.class не найден в bin/app/"
    echo "Список файлов в bin/app/:"
    ls -la bin/app/ 2>/dev/null || echo "Папка не существует"
    rm -f sources.txt
    exit 1
fi

# Создаем манифест
echo "📝 Создание манифеста..."
cat > MANIFEST.MF << 'EOF'
Manifest-Version: 1.0
Main-Class: app.MainApp
Created-By: ToDo List Planner v1.0
EOF

echo "Built-Date: $(date '+%Y-%m-%d %H:%M:%S')" >> MANIFEST.MF

# Создаем временную папку с ПРАВИЛЬНОЙ структурой
echo "📁 Организация ресурсов..."
TEMP_DIR=$(mktemp -d)

# 1. СОЗДАЕМ ПАПКУ view/ внутри JAR (ВАЖНОЕ ИЗМЕНЕНИЕ!)
mkdir -p "$TEMP_DIR/view"
mkdir -p "$TEMP_DIR/resources/css"
mkdir -p "$TEMP_DIR/resources/images"
mkdir -p "$TEMP_DIR/resources/audio"

# 2. Копируем FXML файлы в папку view/ (а не в корень)
echo "📋 Копирование FXML файлов..."
if [ -f "src/view/MainView.fxml" ]; then
    cp "src/view/MainView.fxml" "$TEMP_DIR/view/"
    echo "✅ MainView.fxml скопирован в /view/"
else
    echo "❌ MainView.fxml не найден в src/view/"
    echo "Текущая директория: $(pwd)"
    ls -la src/view/ 2>/dev/null || echo "Папка src/view не существует"
fi

if [ -f "src/view/NewTaskDialog.fxml" ]; then
    cp "src/view/NewTaskDialog.fxml" "$TEMP_DIR/view/"
    echo "✅ NewTaskDialog.fxml скопирован в /view/"
else
    echo "❌ NewTaskDialog.fxml не найден"
fi

# 3. Копируем остальные ресурсы
echo "🎨 Копирование CSS файлов..."
if [ -f "src/resources/css/styles.css" ]; then
    cp "src/resources/css/styles.css" "$TEMP_DIR/resources/css/"
    echo "✅ styles.css скопирован в /resources/css/"
else
    echo "⚠️ styles.css не найден, создаем минимальный..."
    # Создаем минимальный CSS если нет
    cat > "$TEMP_DIR/resources/css/styles.css" << 'CSSEOF'
/* Минимальный CSS */
.root { -fx-font-family: "Arial"; }
.button { -fx-padding: 5; }
.table-view { -fx-background-color: white; }
CSSEOF
fi

echo "🖼️ Копирование изображений..."
if [ -d "src/resources/images" ] && [ "$(ls -A src/resources/images/ 2>/dev/null)" ]; then
    cp -r src/resources/images/* "$TEMP_DIR/resources/images/" 2>/dev/null
    IMAGE_COUNT=$(find "$TEMP_DIR/resources/images" -type f 2>/dev/null | wc -l)
    echo "✅ $IMAGE_COUNT изображений скопированы в /resources/images/"
else
    echo "⚠️ Папка images пуста или не найдена"
fi

echo "🔊 Копирование аудио..."
if [ -d "src/resources/audio" ] && [ "$(ls -A src/resources/audio/ 2>/dev/null)" ]; then
    cp -r src/resources/audio/* "$TEMP_DIR/resources/audio/" 2>/dev/null
    AUDIO_COUNT=$(find "$TEMP_DIR/resources/audio" -type f 2>/dev/null | wc -l)
    echo "✅ $AUDIO_COUNT аудио файлов скопированы в /resources/audio/"
else
    echo "⚠️ Папка audio пуста или не найдена"
    # Создаем пустую папку
    mkdir -p "$TEMP_DIR/resources/audio"
fi

# Создаем JAR
echo "📦 Создание JAR файла..."
cd bin
jar cfm ../ToDoList.jar ../MANIFEST.MF .
cd ..

# Добавляем ресурсы в JAR с сохранением структуры
jar uf ToDoList.jar -C "$TEMP_DIR" .

# Проверяем содержимое
echo ""
echo "🔍 Проверка содержимого JAR..."
echo "=== СТРУКТУРА JAR (важные файлы): ==="

echo ""
echo "1. FXML файлы (должны быть в /view/):"
if jar tf ToDoList.jar | grep -q "view/MainView.fxml"; then
    echo "   ✅ /view/MainView.fxml"
else
    echo "   ❌ /view/MainView.fxml ОТСУТСТВУЕТ!"
fi

if jar tf ToDoList.jar | grep -q "view/NewTaskDialog.fxml"; then
    echo "   ✅ /view/NewTaskDialog.fxml"
else
    echo "   ❌ /view/NewTaskDialog.fxml ОТСУТСТВУЕТ!"
fi

echo ""
echo "2. Ресурсы:"
if jar tf ToDoList.jar | grep -q "resources/css/styles.css"; then
    echo "   ✅ /resources/css/styles.css"
else
    echo "   ❌ /resources/css/styles.css ОТСУТСТВУЕТ!"
fi

# Создаем иконку (если есть PNG)
if [ -f "src/resources/images/app_icon.png" ]; then
    echo "🖼️  Создание иконки .icns..."
    mkdir -p "ToDoList.app/Contents/Resources"
    
    # Создаем .iconset для разных размеров
    mkdir -p "app_icon.iconset"
    
    sips -z 16 16 src/resources/images/app_icon.png --out app_icon.iconset/icon_16x16.png
    sips -z 32 32 src/resources/images/app_icon.png --out app_icon.iconset/icon_16x16@2x.png
    sips -z 32 32 src/resources/images/app_icon.png --out app_icon.iconset/icon_32x32.png
    sips -z 64 64 src/resources/images/app_icon.png --out app_icon.iconset/icon_32x32@2x.png
    sips -z 128 128 src/resources/images/app_icon.png --out app_icon.iconset/icon_128x128.png
    sips -z 256 256 src/resources/images/app_icon.png --out app_icon.iconset/icon_128x128@2x.png
    sips -z 256 256 src/resources/images/app_icon.png --out app_icon.iconset/icon_256x256.png
    sips -z 512 512 src/resources/images/app_icon.png --out app_icon.iconset/icon_256x256@2x.png
    sips -z 512 512 src/resources/images/app_icon.png --out app_icon.iconset/icon_512x512.png
    
    # Создаем .icns файл
    iconutil -c icns app_icon.iconset -o ToDoList.app/Contents/Resources/app_icon.icns
    
    rm -rf app_icon.iconset
fi

echo ""
echo "3. Основные классы:"
if jar tf ToDoList.jar | grep -q "app/MainApp.class"; then
    echo "   ✅ app/MainApp.class"
else
    echo "   ❌ app/MainApp.class ОТСУТСТВУЕТ!"
fi

# Показываем полную структуру для отладки
echo ""
echo "=== ПОЛНАЯ СТРУКТУРА (первые 30 файлов): ==="
jar tf ToDoList.jar | head -30

# Очищаем
rm -f sources.txt
rm -f MANIFEST.MF
rm -rf "$TEMP_DIR"

echo ""
echo "🎉 JAR создан успешно!"
echo "📏 Размер: $(du -h ToDoList.jar | cut -f1)"
echo ""
echo "🚀 Тестирование структуры:"
echo "   jar tf ToDoList.jar | grep '\.fxml$'"
echo "   (должно показать view/MainView.fxml и view/NewTaskDialog.fxml)"