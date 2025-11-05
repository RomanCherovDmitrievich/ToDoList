#!/bin/bash

# Путь к JavaFX SDK
JAVAFX_PATH="../javafx-sdk-25.0.1/lib"

# Создаем папку для скомпилированных классов
mkdir -p target

echo "Компиляция Java файлов..."

# Компилируем все Java файлы
find src -name "*.java" > sources.txt
javac --module-path $JAVAFX_PATH --add-modules javafx.controls,javafx.fxml -d target @sources.txt

if [ $? -eq 0 ]; then
    echo "Компиляция успешна!"
    echo "Запуск приложения..."
    java --module-path $JAVAFX_PATH --add-modules javafx.controls,javafx.fxml -cp target com.todomanager.Main
else
    echo "Ошибка компиляции!"
    exit 1
fi