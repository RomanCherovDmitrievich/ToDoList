#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

APP_NAME="ToDoList"
APP_VERSION="1.0.0"
DIST_DIR="$PROJECT_ROOT/dist"
INPUT_DIR="$DIST_DIR/package-input"
OUTPUT_DIR="$DIST_DIR/package-output"
JAR_NAME="ToDoList.jar"

INSTALLER_MODE="false"
if [[ "${1:-}" == "--installer" ]]; then
  INSTALLER_MODE="true"
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
  echo "[package] JavaFX SDK not found. Set JAVAFX_HOME or place javafx-sdk-<version> in project root."
  exit 1
fi

if [[ "$OS" == "Darwin" && -f "$JAVAFX_LIB/libglass.dylib" ]]; then
  SYS_ARCH="$(uname -m)"
  SDK_ARCH="$(file "$JAVAFX_LIB/libglass.dylib" | awk '{print $NF}')"
  if [[ "$SYS_ARCH" == "arm64" && "$SDK_ARCH" == "arm64e" ]]; then
    SDK_ARCH="arm64"
  fi
  if [[ "$SDK_ARCH" != "$SYS_ARCH" ]]; then
    echo "[package] JavaFX SDK architecture mismatch."
    echo "[package] System: $SYS_ARCH, JavaFX: $SDK_ARCH"
    echo "[package] Download JavaFX SDK for macOS-$SYS_ARCH and replace javafx-sdk-25.0.1."
    exit 1
  fi
fi

if ! command -v jpackage >/dev/null 2>&1; then
  echo "[package] jpackage is not available in current JDK."
  echo "[package] Use JDK 14+ (recommended JDK 21+)."
  exit 1
fi

./scripts/run.sh --build-only

rm -rf "$DIST_DIR"
mkdir -p "$INPUT_DIR" "$OUTPUT_DIR"

cat > "$DIST_DIR/MANIFEST.MF" <<MANIFEST
Manifest-Version: 1.0
Main-Class: app.MainApp
Class-Path: gson-2.10.1.jar sqlite-jdbc-3.45.1.0.jar slf4j-api-2.0.12.jar slf4j-nop-2.0.12.jar
MANIFEST

jar cfm "$INPUT_DIR/$JAR_NAME" "$DIST_DIR/MANIFEST.MF" -C bin .
cp lib/gson-2.10.1.jar "$INPUT_DIR/"
cp lib/sqlite-jdbc-3.45.1.0.jar "$INPUT_DIR/"
cp lib/slf4j-api-2.0.12.jar "$INPUT_DIR/" 2>/dev/null || true
cp lib/slf4j-nop-2.0.12.jar "$INPUT_DIR/" 2>/dev/null || true

mkdir -p "$INPUT_DIR/javafx"
cp "$JAVAFX_LIB"/* "$INPUT_DIR/javafx/" 2>/dev/null || true

OS="$(uname -s 2>/dev/null || echo UNKNOWN)"

ICON_OPTION=()
if [[ -f "src/resources/images/app_icon.png" ]]; then
  if [[ "$OS" == "Darwin" ]]; then
    if command -v sips >/dev/null 2>&1 && command -v iconutil >/dev/null 2>&1; then
      ICONSET_DIR="$DIST_DIR/app_icon.iconset"
      ICNS_FILE="$DIST_DIR/app_icon.icns"
      rm -rf "$ICONSET_DIR"
      mkdir -p "$ICONSET_DIR"

      sips -z 16 16 src/resources/images/app_icon.png --out "$ICONSET_DIR/icon_16x16.png" >/dev/null
      sips -z 32 32 src/resources/images/app_icon.png --out "$ICONSET_DIR/icon_16x16@2x.png" >/dev/null
      sips -z 32 32 src/resources/images/app_icon.png --out "$ICONSET_DIR/icon_32x32.png" >/dev/null
      sips -z 64 64 src/resources/images/app_icon.png --out "$ICONSET_DIR/icon_32x32@2x.png" >/dev/null
      sips -z 128 128 src/resources/images/app_icon.png --out "$ICONSET_DIR/icon_128x128.png" >/dev/null
      sips -z 256 256 src/resources/images/app_icon.png --out "$ICONSET_DIR/icon_128x128@2x.png" >/dev/null
      sips -z 256 256 src/resources/images/app_icon.png --out "$ICONSET_DIR/icon_256x256.png" >/dev/null
      sips -z 512 512 src/resources/images/app_icon.png --out "$ICONSET_DIR/icon_256x256@2x.png" >/dev/null
      sips -z 512 512 src/resources/images/app_icon.png --out "$ICONSET_DIR/icon_512x512.png" >/dev/null
      sips -z 1024 1024 src/resources/images/app_icon.png --out "$ICONSET_DIR/icon_512x512@2x.png" >/dev/null

      iconutil -c icns "$ICONSET_DIR" -o "$ICNS_FILE"
      ICON_OPTION=(--icon "$ICNS_FILE")
    else
      echo "[package] sips/iconutil not found; icon will use default on macOS."
    fi
  else
    ICON_OPTION=(--icon "$PROJECT_ROOT/src/resources/images/app_icon.png")
  fi
fi

PACKAGE_TYPE="app-image"
if [[ "$INSTALLER_MODE" == "true" ]]; then
  case "$OS" in
    Darwin) PACKAGE_TYPE="dmg" ;;
    Linux) PACKAGE_TYPE="rpm" ;;
    MINGW*|MSYS*|CYGWIN*) PACKAGE_TYPE="exe" ;;
  esac
fi

JAVA_OPTS='--module-path $APPDIR/javafx --add-modules javafx.controls,javafx.fxml,javafx.media --enable-native-access=javafx.graphics'

jpackage \
  --type "$PACKAGE_TYPE" \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --input "$INPUT_DIR" \
  --main-jar "$JAR_NAME" \
  --dest "$OUTPUT_DIR" \
  --vendor "ToDoList" \
  --description "Cross-platform TodoList app" \
  --java-options "$JAVA_OPTS" \
  "${ICON_OPTION[@]}"

cat <<MSG
[package] Done.
[package] Output: $OUTPUT_DIR

macOS:
  Open the generated .app and drag it to Applications or Desktop.

Linux/RedOS:
  Use the generated app-image (or installer with --installer).

Windows:
  Use generated app-image/exe depending on mode.
MSG
