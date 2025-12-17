#!/bin/bash
# create_mac_app.sh - Создание macOS приложения .app (ИСПРАВЛЕННАЯ ВЕРСИЯ)

echo "🍎 Создание macOS приложения..."

# Используем имя БЕЗ ПРОБЕЛОВ
APP_NAME="ToDoListPlanner"

# Проверяем JAR
if [ ! -f "ToDoList.jar" ]; then
    echo "❌ ToDoList.jar не найден. Сначала запустите ./build_jar.sh"
    echo "   или ./build_jar_fixed.sh"
    exit 1
fi

# Проверяем JavaFX
if [ ! -d "../javafx-sdk-25.0.1" ]; then
    echo "❌ JavaFX SDK не найден в ../javafx-sdk-25.0.1"
    echo "   Скачайте с https://gluonhq.com/products/javafx/"
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
echo "📦 Копирование JAR файла..."
cp ToDoList.jar "$APP_NAME.app/Contents/Resources/Java/"

echo "🎨 Копирование JavaFX..."
cp -r ../javafx-sdk-25.0.1 "$APP_NAME.app/Contents/Frameworks/"

# Создаем Info.plist
echo "📄 Создание Info.plist..."
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
    <key>CFBundleIconFile</key>
    <string>ToDoList.icns</string>
    <key>CFBundleDevelopmentRegion</key>
    <string>English</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.13</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSHumanReadableCopyright</key>
    <string>Copyright © 2025. Все права защищены.</string>
</dict>
</plist>
EOF

# Создаем УЛУЧШЕННЫЙ запускающий скрипт с отладкой
echo "🚀 Создание запускаемого скрипта..."
cat > "$APP_NAME.app/Contents/MacOS/launcher" << 'EOF'
#!/bin/bash

# Включаем отладку
set -x

# Уникальный лог файл для каждого запуска
LOG_FILE="/tmp/todolist_$(date +%Y%m%d_%H%M%S).log"
echo "=== ЗАПУСК ToDo List Planner $(date) ===" > "$LOG_FILE"

# Получаем абсолютные пути ПРАВИЛЬНО
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_DIR="$(dirname "$SCRIPT_DIR")"  # На один уровень выше, чем MacOS

echo "SCRIPT_DIR: $SCRIPT_DIR" >> "$LOG_FILE"
echo "APP_DIR: $APP_DIR" >> "$LOG_FILE"

# Пути к файлам (ВАЖНО: без лишнего Contents!)
JAR_PATH="$APP_DIR/Resources/Java/ToDoList.jar"
JAVAFX_PATH="$APP_DIR/Frameworks/javafx-sdk-25.0.1/lib"

echo "JAR_PATH: $JAR_PATH" >> "$LOG_FILE"
echo "JAVAFX_PATH: $JAVAFX_PATH" >> "$LOG_FILE"

# Проверяем структуру
echo "=== СТРУКТУРА ПРИЛОЖЕНИЯ ===" >> "$LOG_FILE"
find "$APP_DIR" -type f -name "*.jar" 2>/dev/null >> "$LOG_FILE"
ls -la "$APP_DIR/Resources/Java/" 2>/dev/null >> "$LOG_FILE"

# Проверяем файлы
if [ ! -f "$JAR_PATH" ]; then
    echo "❌ ОШИБКА: JAR файл не найден!" >> "$LOG_FILE"
    echo "Искали по пути: $JAR_PATH" >> "$LOG_FILE"
    echo "Содержимое папки Resources/Java:" >> "$LOG_FILE"
    ls -la "$(dirname "$JAR_PATH")" 2>/dev/null >> "$LOG_FILE"
    echo "❌ JAR файл не найден. Проверьте лог: $LOG_FILE"
    exit 1
fi

if [ ! -d "$JAVAFX_PATH" ]; then
    echo "❌ ОШИБКА: JavaFX не найден!" >> "$LOG_FILE"
    echo "Искали по пути: $JAVAFX_PATH" >> "$LOG_FILE"
    echo "❌ JavaFX не найден. Проверьте лог: $LOG_FILE"
    exit 1
fi

# Проверяем необходимые модули JavaFX
echo "=== ПРОВЕРКА МОДУЛЕЙ JAVAFX ===" >> "$LOG_FILE"
REQUIRED_MODULES=("javafx.base.jar" "javafx.controls.jar" "javafx.fxml.jar" "javafx.graphics.jar" "javafx.media.jar")

for module in "${REQUIRED_MODULES[@]}"; do
    if [ -f "$JAVAFX_PATH/$module" ]; then
        echo "✅ $module найден" >> "$LOG_FILE"
    else
        echo "❌ $module НЕ НАЙДЕН!" >> "$LOG_FILE"
    fi
done

# Запускаем приложение со ВСЕМИ необходимыми модулями
echo "=== ЗАПУСК ПРИЛОЖЕНИЯ ===" >> "$LOG_FILE"
echo "Команда:" >> "$LOG_FILE"
echo "java --module-path \"$JAVAFX_PATH\" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.media -jar \"$JAR_PATH\"" >> "$LOG_FILE"

/usr/bin/java --module-path "$JAVAFX_PATH" \
     --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.media \
     -jar "$JAR_PATH" 2>&1 | tee -a "$LOG_FILE"

EXIT_CODE=${PIPESTATUS[0]}
echo "=== ЗАВЕРШЕНО С КОДОМ: $EXIT_CODE ===" >> "$LOG_FILE"

if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ Приложение завершилось успешно"
    echo "Логи в: $LOG_FILE"
else
    echo "❌ Приложение завершилось с ошибкой (код: $EXIT_CODE)"
    echo "📋 Последние строки лога:"
    tail -20 "$LOG_FILE"
    echo "Полный лог: $LOG_FILE"
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

exit $EXIT_CODE
EOF

# Делаем скрипт исполняемым
chmod +x "$APP_NAME.app/Contents/MacOS/launcher"



# Даем права на весь .app
chmod -R 755 "$APP_NAME.app"

# Удаляем атрибуты карантина (чтобы не было предупреждений)
echo "🔓 Удаление атрибутов карантина..."
xattr -cr "$APP_NAME.app"

echo ""
echo "🎉 ПРИЛОЖЕНИЕ СОЗДАНО УСПЕШНО!"
echo "═══════════════════════════════════════════════════"
echo "📁 Приложение: '$APP_NAME.app'"
echo ""
echo "📋 ИНСТРУКЦИЯ ПО ЗАПУСКУ:"
echo ""
echo "1. 🔧 ТЕСТИРОВАНИЕ (обязательно):"
echo "   Открой Terminal и выполни:"
echo "   cd '$(pwd)'"
echo "   ./$APP_NAME.app/Contents/MacOS/launcher"
echo ""
echo "2. 📍 Если тест успешен:"
echo "   Перетащи '$APP_NAME.app' в папку Applications"
echo ""
echo "3. 🚀 ПЕРВЫЙ ЗАПУСК ИЗ Finder:"
echo "   - macOS покажет предупреждение безопасности"
echo "   - Зайдите в Системные настройки → Конфиденциальность и безопасность"
echo "   - Нажмите 'Разрешить всё равно'"
echo ""
echo "4. 🔧 ЕСЛИ НЕ ЗАПУСКАЕТСЯ ИЗ Finder:"
echo "   Правая кнопка на приложении → 'Открыть'"
echo ""
echo "5. 📝 ЛОГИ:"
echo "   Все логи сохраняются в /tmp/todolist_*.log"
echo "═══════════════════════════════════════════════════"
echo ""
echo "⚠️  ВАЖНО: При первом запуске через Finder будет"
echo "    предупреждение безопасности. Это нормально!"
echo "    Нажмите 'Открыть' или разрешите в настройках."