#!/bin/bash
cd "$(dirname "$0")"

echo "🔍 Диагностика графической системы..."
echo "Java version:"
java -version
echo ""
echo "Architecture: $(uname -m)"
echo ""

echo "🧪 Тест 1: Software rendering..."
java --module-path javafx-sdk-25.0.1/lib \
     --add-modules javafx.controls,javafx.fxml \
     -Dprism.order=sw \
     -Dprism.verbose=true \
     -cp "bin" \
     com.todoapp.Main &
PID1=$!
sleep 5
kill $PID1 2>/dev/null

echo ""
echo "🧪 Тест 2: ES2 rendering..."
java --module-path javafx-sdk-25.0.1/lib \
     --add-modules javafx.controls,javafx.fxml \
     -Dprism.order=es2 \
     -Dprism.verbose=true \
     -XstartOnFirstThread \
     -cp "bin" \
     com.todoapp.Main &
PID2=$!
sleep 5
kill $PID2 2>/dev/null