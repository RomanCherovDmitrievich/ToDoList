#!/bin/bash
# create_mac_app.sh - Создание macOS приложения .app

echo "🍎 Создание macOS приложения..."

# Используем имя БЕЗ ПРОБЕЛОВ
APP_NAME="ToDoListPlanner"

# Проверяем JAR
if [ ! -f "ToDoList.jar" ]; then
    echo "❌ ToDoList.jar не найден. Сначала запустите ./build_jar.sh"
    exit 1
fi

# Проверяем JavaFX
if [ ! -d "../javafx-sdk-25.0.1" ]; then
    echo "❌ JavaFX SDK не найден в ../javafx-sdk-25.0.1"
    exit 1
fi

# Удаляем старое приложение
rm -rf "$APP_NAME.app" "ToDo List Planner.app" 2>/dev/null

# Создаем структуру
echo "📁 Создание структуры приложения..."
mkdir -p "$APP_NAME.app/Contents/MacOS"
mkdir -p "$APP_NAME.app/Contents/Resources/Java"
mkdir -p "$APP_NAME.app/Contents/Frameworks"

# Копируем файлы
cp ToDoList.jar "$APP_NAME.app/Contents/Resources/Java/"
cp -r ../javafx-sdk-25.0.1 "$APP_NAME.app/Contents/Frameworks/"

# Создаем Info.plist
cat > "$APP_NAME.app/Contents/Info.plist" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>
    <string>ToDoListPlanner</string>
    <key>CFBundleDisplayName</key>
    <string>ToDo List Planner</string>
    <key>CFBundleIdentifier</key>
    <string>com.yourname.todolist</string>
    <key>CFBundleVersion</key>
    <string>1.0</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleExecutable</key>
    <string>launcher</string>
    <key>NSHighResolutionCapable</key>
    <true/>
</dict>
</plist>
EOF

# Создаем ПРОСТОЙ запускающий скрипт
cat > "$APP_NAME.app/Contents/MacOS/launcher" << 'EOF'
#!/bin/bash

# Простой скрипт для отладки
echo "Запуск ToDo List Planner..." > /tmp/todolist.log

APP_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
JAR_PATH="$APP_DIR/Contents/Resources/Java/ToDoList.jar"
JAVAFX_PATH="$APP_DIR/Contents/Frameworks/javafx-sdk-25.0.1/lib"

if [ ! -f "$JAR_PATH" ]; then
    echo "JAR не найден: $JAR_PATH" >> /tmp/todolist.log
    exit 1
fi

# Запускаем приложение с выводом в консоль
/usr/bin/java --module-path "$JAVAFX_PATH" \
     --add-modules javafx.controls,javafx.fxml,javafx.graphics \
     -jar "$JAR_PATH" 2>&1 | tee -a /tmp/todolist.log
EOF

# Делаем скрипт исполняемым
chmod +x "$APP_NAME.app/Contents/MacOS/launcher"

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

# Даем права на весь .app
chmod -R 755 "$APP_NAME.app"

# Удаляем атрибуты карантина
xattr -cr "$APP_NAME.app"

echo ""
echo "✅ Приложение создано: $APP_NAME.app"
echo ""
echo "🚀 ТЕСТИРОВАНИЕ:"
echo "1. Запустите в терминале для проверки:"
echo "   $APP_NAME.app/Contents/MacOS/launcher"
echo ""
echo "2. Если работает, попробуйте открыть через Finder:"
echo "   open $APP_NAME.app"
echo ""
echo "📝 Логи будут в: /tmp/todolist.log"