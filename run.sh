#!/bin/bash

# Определяем текущую директорию скрипта
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "📁 Текущая директория: $(pwd)"
echo "🚀 Запуск ToDo List Planner..."

# Пути
JAVA_HOME=$(/usr/libexec/java_home -v 25 2>/dev/null || echo "/usr/libexec/java_home")
JAVAFX_PATH="../javafx-sdk-25.0.1/lib"
SRC_DIR="src"
BIN_DIR="bin"
MAIN_CLASS="app.MainApp"

# Создаем необходимые папки
mkdir -p $BIN_DIR
mkdir -p data

echo "🔨 Компиляция JavaFX приложения..."

# Находим все Java файлы
echo "📦 Поиск Java файлов..."
JAVA_FILES=$(find $SRC_DIR -name "*.java")

if [ -z "$JAVA_FILES" ]; then
    echo "❌ Java файлы не найдены в директории $SRC_DIR"
    exit 1
fi

echo "📦 Найдено файлов: $(echo "$JAVA_FILES" | wc -l)"

# Компилируем ВСЕ файлы ОДНОЙ командой
echo "📦 Компиляция всех файлов..."
$JAVA_HOME/bin/javac --module-path $JAVAFX_PATH \
    --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.media \
    -d $BIN_DIR \
    $JAVA_FILES

# Проверяем успешность компиляции
if [ $? -ne 0 ]; then
    echo "❌ Ошибка компиляции!"
    echo "Попробуйте скомпилировать вручную:"
    echo "javac --module-path $JAVAFX_PATH --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.media -d $BIN_DIR \$(find $SRC_DIR -name \"*.java\")"
    exit 1
fi

echo "✅ Компиляция успешна!"

echo "📦 Копирование ресурсов..."

# Создаем директории для ресурсов в bin
mkdir -p $BIN_DIR/view
mkdir -p $BIN_DIR/resources/css
mkdir -p $BIN_DIR/resources/images
mkdir -p $BIN_DIR/resources/audio

# Копируем FXML файлы
if [ -d "$SRC_DIR/view" ] && [ "$(ls -A $SRC_DIR/view/*.fxml 2>/dev/null)" ]; then
    cp $SRC_DIR/view/*.fxml $BIN_DIR/view/ 2>/dev/null
    echo "✅ FXML файлы скопированы"
else
    echo "⚠️  FXML файлы не найдены в $SRC_DIR/view/"
fi

# Копируем CSS файлы
if [ -f "$SRC_DIR/resources/css/styles.css" ]; then
    mkdir -p $BIN_DIR/resources/css
    cp $SRC_DIR/resources/css/styles.css $BIN_DIR/resources/css/ 2>/dev/null
    echo "✅ CSS файлы скопированы"
else
    echo "⚠️  CSS файлы не найдены"
fi

# Создаем простые изображения, если их нет
echo "🖼️  Проверка изображений..."
if [ ! -d "$SRC_DIR/resources/images" ]; then
    mkdir -p $SRC_DIR/resources/images
fi

# Создаем простую иконку приложения (синий квадрат)
if [ ! -f "$SRC_DIR/resources/images/app_icon.png" ]; then
    echo "📸 Создание тестовой иконки приложения..."
    # Используем convert из ImageMagick, если доступен
    if command -v convert &> /dev/null; then
        convert -size 64x64 xc:#3498db -fill white -pointsize 24 -gravity center -draw "text 0,0 'TD'" $SRC_DIR/resources/images/app_icon.png
    else
        # Создаем простой текстовый файл как заглушку
        echo "PNG placeholder" > $SRC_DIR/resources/images/app_icon.png
    fi
fi

# Создаем иконки для кнопок
if [ ! -f "$SRC_DIR/resources/images/add_icon.png" ]; then
    echo "➕ Создание иконки добавления..."
    if command -v convert &> /dev/null; then
        convert -size 32x32 xc:#4CAF50 -fill white -pointsize 20 -gravity center -draw "text 0,0 '+'" $SRC_DIR/resources/images/add_icon.png
    else
        echo "Add icon" > $SRC_DIR/resources/images/add_icon.png
    fi
fi

if [ ! -f "$SRC_DIR/resources/images/delete_icon.png" ]; then
    echo "🗑️  Создание иконки удаления..."
    if command -v convert &> /dev/null; then
        convert -size 32x32 xc:#f44336 -fill white -pointsize 20 -gravity center -draw "text 0,0 '×'" $SRC_DIR/resources/images/delete_icon.png
    else
        echo "Delete icon" > $SRC_DIR/resources/images/delete_icon.png
    fi
fi

# Копируем изображения
cp $SRC_DIR/resources/images/*.png $BIN_DIR/resources/images/ 2>/dev/null || echo "⚠️  Не удалось скопировать изображения"

# Создаем заглушку для аудио
echo "🔊 Проверка аудио файлов..."
if [ ! -d "$SRC_DIR/resources/audio" ]; then
    mkdir -p $SRC_DIR/resources/audio
fi

if [ ! -f "$SRC_DIR/resources/audio/startup.mp3" ]; then
    echo "🎵 Создание заглушки для аудио..."
    # Создаем минимальный валидный MP3 заголовок (без звука)
    echo -n "ID3" > $SRC_DIR/resources/audio/startup.mp3
    printf "\x00\x00\x00\x00\x00\x00" >> $SRC_DIR/resources/audio/startup.mp3
fi

cp $SRC_DIR/resources/audio/*.mp3 $BIN_DIR/resources/audio/ 2>/dev/null || echo "⚠️  Не удалось скопировать аудио файлы"

echo "✅ Ресурсы обработаны"

# Создаем файл tasks.json если его нет
if [ ! -f "data/tasks.json" ]; then
    echo "📝 Создание файла tasks.json..."
    cat > data/tasks.json << 'EOF'
[
  {
    "id": "77ec9df9-bb77-4caf-abd6-4fbc6810d33e",
    "title": "Добро пожаловать в ToDo List!",
    "description": "Это ваша первая задача. Вы можете её редактировать, отмечать как выполненную или удалить.",
    "startTime": "2025-12-01T09:00:00",
    "endTime": "2025-12-31T18:00:00",
    "priority": "IMPORTANT",
    "category": "OTHER",
    "completed": false,
    "overdue": false,
    "createdAt": "2025-11-30T10:00:00"
  },
  {
    "id": "88fc9df9-cc77-4caf-bcd6-4fbc6810d44f",
    "title": "Пример выполненной задачи",
    "description": "Эта задача уже выполнена. Выполненные задачи отображаются серым цветом.",
    "startTime": "2025-11-01T10:00:00",
    "endTime": "2025-11-15T12:00:00",
    "priority": "NORMAL",
    "category": "HOME",
    "completed": true,
    "overdue": false,
    "createdAt": "2025-10-28T08:20:15"
  },
  {
    "id": "99ac9df9-dd77-4caf-cdd6-4fbc6810d55g",
    "title": "Пример просроченной задачи",
    "description": "Эта задача просрочена. Просроченные задачи выделяются красным цветом.",
    "startTime": "2025-11-20T14:00:00",
    "endTime": "2025-11-25T17:00:00",
    "priority": "URGENT",
    "category": "WORK",
    "completed": false,
    "overdue": true,
    "createdAt": "2025-11-19T09:15:22"
  }
]
EOF
    echo "✅ Файл tasks.json создан"
fi

# Запускаем приложение
echo "🚀 Запуск ToDo List Planner..."
echo "=========================================="
echo "Информация о системе:"
echo "Java: $($JAVA_HOME/bin/java -version 2>&1 | head -n 1)"
echo "JavaFX SDK: $(basename $JAVAFX_PATH/../..)"
echo "Директория проекта: $(pwd)"
echo "=========================================="

# Проверяем, установлен ли ImageMagick (для создания иконок)
if ! command -v convert &> /dev/null; then
    echo "⚠️  ImageMagick не установлен. Установите для создания иконок:"
    echo "    brew install imagemagick   # на macOS"
    echo "    sudo apt install imagemagick   # на Ubuntu/Debian"
fi

$JAVA_HOME/bin/java --module-path $JAVAFX_PATH \
    --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.media \
    -cp "$BIN_DIR" \
    $MAIN_CLASS

EXIT_CODE=$?

# Генерация документации если нужно
if [ "$1" = "--docs" ]; then
    echo "📖 Генерация документации..."
    if [ -f "generate_docs.sh" ]; then
        ./generate_docs.sh
    else
        doxygen Doxyfile 2>/dev/null || echo "⚠️  Doxygen не настроен"
    fi
fi

if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ Приложение успешно завершило работу"
else
    echo "❌ Приложение завершилось с ошибкой (код: $EXIT_CODE)"
fi