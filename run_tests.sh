#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

SRC_DIR="$PROJECT_ROOT/src"
BIN_DIR="$PROJECT_ROOT/bin"
TESTS_DIR="$PROJECT_ROOT/tests"
CLASSES_DIR="$TESTS_DIR/classes"
REPORTS_DIR="$TESTS_DIR/reports"
LIB_DIR="$PROJECT_ROOT/lib"

JUNIT_VERSION="1.9.2"
JUNIT_DIR="$TESTS_DIR/lib"
JUNIT_JAR="$JUNIT_DIR/junit-platform-console-standalone-${JUNIT_VERSION}.jar"

mkdir -p "$CLASSES_DIR" "$REPORTS_DIR" "$JUNIT_DIR"

resolve_javafx_lib() {
  if [[ -n "${JAVAFX_HOME:-}" && -d "${JAVAFX_HOME}/lib" ]]; then
    echo "${JAVAFX_HOME}/lib"
    return
  fi
  if [[ -d ".javafx/lib" ]]; then
    echo ".javafx/lib"
    return
  fi
  local candidate
  candidate="$(ls -d javafx-sdk-* 2>/dev/null | sort -V | tail -n 1 || true)"
  if [[ -n "$candidate" && -d "$candidate/lib" ]]; then
    if [[ ! -e ".javafx" ]]; then
      ln -sfn "$candidate" ".javafx"
    fi
    echo ".javafx/lib"
    return
  fi
  echo ""
}

JAVAFX_LIB="$(resolve_javafx_lib)"
if [[ -z "$JAVAFX_LIB" ]]; then
  echo "[tests] JavaFX SDK not found. Set JAVAFX_HOME or place javafx-sdk-<version> in project root."
  exit 1
fi

OS="$(uname -s 2>/dev/null || echo UNKNOWN)"
if [[ "$OS" == MINGW* || "$OS" == MSYS* || "$OS" == CYGWIN* ]]; then
  CP_SEP=';'
else
  CP_SEP=':'
fi

if [[ ! -f "$LIB_DIR/gson-2.10.1.jar" || ! -f "$LIB_DIR/sqlite-jdbc-3.45.1.0.jar" || ! -f "$LIB_DIR/slf4j-api-2.0.12.jar" || ! -f "$LIB_DIR/slf4j-nop-2.0.12.jar" ]]; then
  echo "[tests] Required jars missing in lib/. Run: ./scripts/setup.sh"
  exit 1
fi

if [[ ! -f "$JUNIT_JAR" ]]; then
  echo "[tests] Downloading JUnit console..."
  if command -v curl >/dev/null 2>&1; then
    curl -L --fail "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/${JUNIT_VERSION}/junit-platform-console-standalone-${JUNIT_VERSION}.jar" \
      -o "$JUNIT_JAR"
  else
    echo "[tests] curl not found. Download JUnit manually: $JUNIT_JAR"
    exit 1
  fi
fi

./scripts/run.sh --build-only

TEST_SOURCES_LIST="$REPORTS_DIR/test-sources.list"
find "$TESTS_DIR" -name "*Test.java" -type f | sort > "$TEST_SOURCES_LIST"

if [[ ! -s "$TEST_SOURCES_LIST" ]]; then
  echo "[tests] No test sources found in $TESTS_DIR"
  exit 1
fi

echo "[tests] Compiling tests..."

javac \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml,javafx.media \
  -cp "$BIN_DIR${CP_SEP}$JUNIT_JAR${CP_SEP}$LIB_DIR/gson-2.10.1.jar${CP_SEP}$LIB_DIR/sqlite-jdbc-3.45.1.0.jar${CP_SEP}$LIB_DIR/slf4j-api-2.0.12.jar${CP_SEP}$LIB_DIR/slf4j-nop-2.0.12.jar" \
  -d "$CLASSES_DIR" \
  @"$TEST_SOURCES_LIST"

RESULTS_FILE="$REPORTS_DIR/test_results_$(date +%Y%m%d_%H%M%S).txt"

echo "[tests] Running JUnit..."

java \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml,javafx.media \
  --enable-native-access=javafx.graphics \
  -cp "$BIN_DIR${CP_SEP}$CLASSES_DIR${CP_SEP}$JUNIT_JAR${CP_SEP}$LIB_DIR/gson-2.10.1.jar${CP_SEP}$LIB_DIR/sqlite-jdbc-3.45.1.0.jar${CP_SEP}$LIB_DIR/slf4j-api-2.0.12.jar${CP_SEP}$LIB_DIR/slf4j-nop-2.0.12.jar" \
  org.junit.platform.console.ConsoleLauncher \
  --scan-class-path \
  --class-path "$BIN_DIR${CP_SEP}$CLASSES_DIR" \
  --details=tree \
  --disable-banner \
  2>&1 | tee "$RESULTS_FILE"

EXIT_CODE=${PIPESTATUS[0]}

echo "[tests] Results: $RESULTS_FILE"
exit $EXIT_CODE
