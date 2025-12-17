#!/bin/bash
# test_app.sh - Тестирование приложения перед использованием

echo "🧪 Тестирование приложения..."

# Проверяем JAR
echo "1. Проверка JAR файла..."
if [ ! -f "ToDoList.jar" ]; then
    echo "❌ ToDoList.jar не найден!"
    echo "   Запусти: ./build_jar.sh"
    exit 1
fi

echo "✅ ToDoList.jar найден ($(du -h ToDoList.jar | cut -f1))"

# Проверяем содержимое JAR
echo ""
echo "2. Проверка содержимого JAR:"
jar tf ToDoList.jar | grep -E "(MainApp|MainView|NewTaskDialog|styles\.css)" | while read line; do
    echo "   ✅ $line"
done

# Проверяем .app приложение
echo ""
echo "3. Проверка .app приложения..."
if [ ! -d "ToDo List Planner.app" ]; then
    echo "⚠️  .app приложение не создано"
    echo "   Запусти: ./create_mac_app.sh"
else
    echo "✅ Приложение создано"
    
    # Проверяем структуру
    echo "   📁 Структура:"
    find "ToDo List Planner.app" -type f -name "*.jar" -o -name "*.fxml" -o -name "*.class" 2>/dev/null | head -10
    
    # Проверяем права
    if [ -x "ToDo List Planner.app/Contents/MacOS/ToDoList" ]; then
        echo "   ✅ Скрипт запуска исполняемый"
    else
        echo "   ❌ Скрипт не исполняемый"
        echo "   Исправь: chmod +x 'ToDo List Planner.app/Contents/MacOS/ToDoList'"
    fi
fi

# Тестируем запуск JAR
echo ""
echo "4. Тестируем запуск JAR напрямую..."
if command -v java &> /dev/null; then
    echo "   Java найдена: $(java --version 2>&1 | head -1)"
    
    # Быстрый тест
    timeout 5 java --module-path ../javafx-sdk-25.0.1/lib \
        --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base \
        -jar ToDoList.jar 2>&1 | head -5
    
    if [ ${PIPESTATUS[0]} -eq 0 ]; then
        echo "   ✅ JAR запускается"
    else
        echo "   ❌ Ошибка запуска JAR"
        echo "   Подробнее: java -jar ToDoList.jar"
    fi
else
    echo "   ❌ Java не установлена!"
fi

echo ""
echo "═══════════════════════════════════════════════════"
echo "📋 ИТОГ:"
echo ""
if [ -d "ToDo List Planner.app" ] && [ -f "ToDoList.jar" ]; then
    echo "🎉 ВСЁ ГОТОВО! Приложение можно использовать."
    echo ""
    echo "🚀 Для запуска:"
    echo "   1. Дважды кликни на 'ToDo List Planner.app'"
    echo "   2. Или в Terminal: open 'ToDo List Planner.app'"
    echo ""
    echo "🔧 Если есть проблемы, выполни:"
    echo "   xattr -cr 'ToDo List Planner.app'"
else
    echo "⚠️  Нужно выполнить:"
    echo "   ./build_jar.sh   # Создать JAR"
    echo "   ./create_mac_app.sh # Создать .app"
fi
echo "═══════════════════════════════════════════════════"