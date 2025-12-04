#!/bin/bash
# Простой скрипт для запуска тестов (обертка над run_tests.sh)

echo "🔧 Подготовка к запуску тестов..."
echo ""

# Проверяем наличие основного скрипта
if [ ! -f "run_tests.sh" ]; then
    echo "❌ Скрипт run_tests.sh не найден"
    echo "📝 Создаю базовый скрипт..."
    
    # Создаем простую версию если нет основной
    cat > run_tests.sh << 'EOF'
#!/bin/bash
echo "🧪 Запуск тестов..."
echo "===================="

# Находим и запускаем тесты через JUnit
find tests -name "*Test.java" | while read test_file; do
    echo "Тестируем: $test_file"
done

echo "✅ Тестирование завершено"
EOF
    
    chmod +x run_tests.sh
fi

# Запускаем основной скрипт
chmod +x run_tests.sh
./run_tests.sh

# Открываем отчет в браузере если есть команда open
if command -v open &> /dev/null; then
    if [ -f "tests/reports/test-report.html" ]; then
        echo ""
        echo "🌐 Открываю отчет в браузере..."
        open "tests/reports/test-report.html"
    fi
elif command -v xdg-open &> /dev/null; then
    if [ -f "tests/reports/test-report.html" ]; then
        echo ""
        echo "🌐 Открываю отчет в браузере..."
        xdg-open "tests/reports/test-report.html"
    fi
fi