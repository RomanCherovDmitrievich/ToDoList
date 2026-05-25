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
POSTGRES_JAR="$LIB_DIR/postgresql-42.7.4.jar"
MYSQL_JAR="$LIB_DIR/mysql-connector-j-9.3.0.jar"
MAIL_API_JAR="$LIB_DIR/jakarta.mail-api-2.1.5.jar"
ANGUS_MAIL_JAR="$LIB_DIR/angus-mail-2.0.5.jar"
ACTIVATION_API_JAR="$LIB_DIR/jakarta.activation-api-2.1.4.jar"
ANGUS_ACTIVATION_JAR="$LIB_DIR/angus-activation-2.0.3.jar"

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
    echo "$candidate/lib"
    return
  fi
  candidate="$(ls -d ../javafx-sdk-* 2>/dev/null | sort -V | tail -n 1 || true)"
  if [[ -n "$candidate" && -d "$candidate/lib" ]]; then
    echo "$candidate/lib"
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

REQUIRED_JARS=(
  "$LIB_DIR/gson-2.10.1.jar"
  "$LIB_DIR/sqlite-jdbc-3.45.1.0.jar"
  "$LIB_DIR/slf4j-api-2.0.12.jar"
  "$LIB_DIR/slf4j-nop-2.0.12.jar"
  "$MAIL_API_JAR"
  "$ANGUS_MAIL_JAR"
  "$ACTIVATION_API_JAR"
  "$ANGUS_ACTIVATION_JAR"
)

for jar in "${REQUIRED_JARS[@]}"; do
  if [[ ! -f "$jar" ]]; then
    echo "[tests] Required jar missing: $(basename "$jar")"
    echo "[tests] Run ./setup.sh first."
    exit 1
  fi
done

APP_CLASSPATH="$BIN_DIR"
for jar in "${REQUIRED_JARS[@]}"; do
  APP_CLASSPATH="${APP_CLASSPATH}${CP_SEP}${jar}"
done
if [[ -f "$POSTGRES_JAR" ]]; then
  APP_CLASSPATH="${APP_CLASSPATH}${CP_SEP}${POSTGRES_JAR}"
fi
if [[ -f "$MYSQL_JAR" ]]; then
  APP_CLASSPATH="${APP_CLASSPATH}${CP_SEP}${MYSQL_JAR}"
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

./run.sh --build-only

TEST_SOURCES_LIST="$REPORTS_DIR/test-sources.list"
find "$TESTS_DIR" -name "*Test.java" -type f -print0 | sort -z | while IFS= read -r -d '' file; do
  printf '"%s"\n' "$file"
done > "$TEST_SOURCES_LIST"

if [[ ! -s "$TEST_SOURCES_LIST" ]]; then
  echo "[tests] No test sources found in $TESTS_DIR"
  exit 1
fi

echo "[tests] Compiling tests..."
javac \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml,javafx.media \
  -cp "$APP_CLASSPATH${CP_SEP}$CLASSES_DIR${CP_SEP}$JUNIT_JAR" \
  -d "$CLASSES_DIR" \
  @"$TEST_SOURCES_LIST"

TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
RESULTS_FILE="$REPORTS_DIR/test_results_${TIMESTAMP}.txt"
XML_REPORT_DIR="$REPORTS_DIR/junit-xml-${TIMESTAMP}"
HTML_REPORT_FILE="$REPORTS_DIR/test_report_${TIMESTAMP}.html"
RUNTIME_DATA_DIR="$REPORTS_DIR/runtime-data-${TIMESTAMP}"
mkdir -p "$RUNTIME_DATA_DIR"

echo "[tests] Running JUnit..."
java \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml,javafx.media \
  --enable-native-access=javafx.graphics \
  -Djava.awt.headless=true \
  -Dtodolist.disableAudio=true \
  -Dtodolist.email.mode=disabled \
  -Dtodolist.dataDir="$RUNTIME_DATA_DIR" \
  -cp "$APP_CLASSPATH${CP_SEP}$CLASSES_DIR${CP_SEP}$JUNIT_JAR" \
  org.junit.platform.console.ConsoleLauncher \
  --scan-class-path \
  --class-path "$APP_CLASSPATH${CP_SEP}$CLASSES_DIR" \
  --details=tree \
  --disable-banner \
  --reports-dir "$XML_REPORT_DIR" \
  2>&1 | tee "$RESULTS_FILE"

EXIT_CODE=${PIPESTATUS[0]}

java \
  -cp "$BIN_DIR" \
  util.TestReportGenerator \
  "$XML_REPORT_DIR" \
  "$HTML_REPORT_FILE" \
  "$RESULTS_FILE"

echo "[tests] Results: $RESULTS_FILE"
echo "[tests] HTML report: $HTML_REPORT_FILE"
exit $EXIT_CODE
