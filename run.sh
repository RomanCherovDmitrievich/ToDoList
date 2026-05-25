#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

BUILD_ONLY="false"
if [[ "${1:-}" == "--build-only" ]]; then
  BUILD_ONLY="true"
fi

SRC_DIR="$PROJECT_ROOT/src"
BIN_DIR="$PROJECT_ROOT/bin"
LIB_DIR="$PROJECT_ROOT/lib"
POSTGRES_JAR="$LIB_DIR/postgresql-42.7.4.jar"
MYSQL_JAR="$LIB_DIR/mysql-connector-j-9.3.0.jar"
MAIL_API_JAR="$LIB_DIR/jakarta.mail-api-2.1.5.jar"
ANGUS_MAIL_JAR="$LIB_DIR/angus-mail-2.0.5.jar"
ACTIVATION_API_JAR="$LIB_DIR/jakarta.activation-api-2.1.4.jar"
ANGUS_ACTIVATION_JAR="$LIB_DIR/angus-activation-2.0.3.jar"
SOURCES_LIST="$PROJECT_ROOT/.sources.list"

OS="$(uname -s 2>/dev/null || echo UNKNOWN)"
if [[ "$OS" == MINGW* || "$OS" == MSYS* || "$OS" == CYGWIN* ]]; then
  CP_SEP=';'
else
  CP_SEP=':'
fi

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
  echo "[run] JavaFX SDK not found."
  echo "[run] Set JAVAFX_HOME or place javafx-sdk-<version> in project root."
  exit 1
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
    echo "[run] Required jar missing: $(basename "$jar")"
    echo "[run] Run ./setup.sh first."
    exit 1
  fi
done

CLASSPATH="${REQUIRED_JARS[0]}"
for jar in "${REQUIRED_JARS[@]:1}"; do
  CLASSPATH="${CLASSPATH}${CP_SEP}${jar}"
done
if [[ -f "$POSTGRES_JAR" ]]; then
  CLASSPATH="${CLASSPATH}${CP_SEP}${POSTGRES_JAR}"
fi
if [[ -f "$MYSQL_JAR" ]]; then
  CLASSPATH="${CLASSPATH}${CP_SEP}${MYSQL_JAR}"
fi

mkdir -p "$BIN_DIR" "$PROJECT_ROOT/data" "$PROJECT_ROOT/audio"

echo "[run] Compiling sources..."
find "$SRC_DIR" -name "*.java" -type f -print0 | sort -z | while IFS= read -r -d '' file; do
  printf '"%s"\n' "$file"
done > "$SOURCES_LIST"

if [[ ! -s "$SOURCES_LIST" ]]; then
  echo "[run] No Java sources found in src/."
  rm -f "$SOURCES_LIST"
  exit 1
fi

javac \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml,javafx.media \
  -cp "$CLASSPATH" \
  -d "$BIN_DIR" \
  @"$SOURCES_LIST"

mkdir -p "$BIN_DIR/view" "$BIN_DIR/resources"
cp "$SRC_DIR"/view/*.fxml "$BIN_DIR/view/"
cp -R "$SRC_DIR/resources"/* "$BIN_DIR/resources/"

if [[ "$BUILD_ONLY" == "true" ]]; then
  rm -f "$SOURCES_LIST"
  echo "[run] Build complete (build-only mode)."
  exit 0
fi

echo "[run] Starting app..."
java \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml,javafx.media \
  --enable-native-access=javafx.graphics \
  -cp "$BIN_DIR${CP_SEP}$CLASSPATH" \
  app.MainApp

rm -f "$SOURCES_LIST"
