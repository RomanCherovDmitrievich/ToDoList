#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

BUILD_ONLY="false"
if [[ "${1:-}" == "--build-only" ]]; then
  BUILD_ONLY="true"
fi

SRC_DIR="src"
BIN_DIR="bin"
LIB_DIR="lib"

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
  echo "[run] JavaFX SDK not found."
  echo "[run] Place javafx-sdk-<version> in project root or set JAVAFX_HOME."
  exit 1
fi

if [[ "$OS" == "Darwin" ]]; then
  ARCH="$(uname -m)"
  if [[ -f "$JAVAFX_LIB/libglass.dylib" ]]; then
    SDK_ARCH="$(file "$JAVAFX_LIB/libglass.dylib" | awk '{print $NF}')"
    if [[ "$ARCH" == "arm64" && "$SDK_ARCH" == "arm64e" ]]; then
      SDK_ARCH="arm64"
    fi
    if [[ "$SDK_ARCH" != "$ARCH" ]]; then
      echo "[run] JavaFX SDK architecture mismatch."
      echo "[run] System: $ARCH, JavaFX: $SDK_ARCH"
      echo "[run] Download JavaFX SDK for macOS-$ARCH and replace javafx-sdk-25.0.1."
      if [[ "$BUILD_ONLY" == "false" ]]; then
        exit 1
      fi
    fi
  fi
fi

if [[ ! -f "$LIB_DIR/gson-2.10.1.jar" || ! -f "$LIB_DIR/sqlite-jdbc-3.45.1.0.jar" || ! -f "$LIB_DIR/slf4j-api-2.0.12.jar" || ! -f "$LIB_DIR/slf4j-nop-2.0.12.jar" ]]; then
  echo "[run] Required jars missing in lib/. Run: ./scripts/setup.sh"
  exit 1
fi

mkdir -p "$BIN_DIR" data audio

echo "[run] Compiling sources..."
SOURCES_LIST=".sources.list"
find "$SRC_DIR" -name "*.java" -type f | sort > "$SOURCES_LIST"

javac \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml,javafx.media \
  -cp "$LIB_DIR/gson-2.10.1.jar${CP_SEP}$LIB_DIR/sqlite-jdbc-3.45.1.0.jar${CP_SEP}$LIB_DIR/slf4j-api-2.0.12.jar${CP_SEP}$LIB_DIR/slf4j-nop-2.0.12.jar" \
  -d "$BIN_DIR" \
  @"$SOURCES_LIST"

mkdir -p "$BIN_DIR/view" "$BIN_DIR/resources"
cp "$SRC_DIR"/view/*.fxml "$BIN_DIR/view/"
cp -R "$SRC_DIR/resources"/* "$BIN_DIR/resources/"

if [[ "$BUILD_ONLY" == "true" ]]; then
  echo "[run] Build complete (build-only mode)."
  exit 0
fi

echo "[run] Starting app..."
java \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml,javafx.media \
  --enable-native-access=javafx.graphics \
  -cp "$BIN_DIR${CP_SEP}$LIB_DIR/gson-2.10.1.jar${CP_SEP}$LIB_DIR/sqlite-jdbc-3.45.1.0.jar${CP_SEP}$LIB_DIR/slf4j-api-2.0.12.jar${CP_SEP}$LIB_DIR/slf4j-nop-2.0.12.jar" \
  app.MainApp

rm -f "$SOURCES_LIST"
