#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

LIB_DIR="lib"

printf "[setup] Project: %s\n" "$PROJECT_ROOT"

if ! command -v java >/dev/null 2>&1; then
  echo "[setup] Java not found in PATH. Install JDK 21+ first."
  exit 1
fi

if ! command -v javac >/dev/null 2>&1; then
  echo "[setup] javac not found in PATH. Install full JDK (not JRE)."
  exit 1
fi

echo "[setup] Java: $(java -version 2>&1 | head -n 1)"

mkdir -p "$LIB_DIR"

download_if_missing() {
  local file="$1"
  local url="$2"
  if [[ -f "$file" ]]; then
    echo "[setup] OK: $(basename "$file")"
    return
  fi

  if ! command -v curl >/dev/null 2>&1; then
    echo "[setup] Missing $(basename "$file") and curl unavailable."
    echo "[setup] Download manually: $url"
    return
  fi

  echo "[setup] Downloading $(basename "$file")"
  curl -L --fail "$url" -o "$file" || {
    echo "[setup] Failed to download $(basename "$file")."
    echo "[setup] Download manually: $url"
  }
}

download_if_missing "$LIB_DIR/gson-2.10.1.jar" \
  "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"

download_if_missing "$LIB_DIR/sqlite-jdbc-3.45.1.0.jar" \
  "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar"

download_if_missing "$LIB_DIR/slf4j-api-2.0.12.jar" \
  "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.12/slf4j-api-2.0.12.jar"

download_if_missing "$LIB_DIR/slf4j-nop-2.0.12.jar" \
  "https://repo1.maven.org/maven2/org/slf4j/slf4j-nop/2.0.12/slf4j-nop-2.0.12.jar"

find_javafx_dir() {
  if [[ -n "${JAVAFX_HOME:-}" && -d "${JAVAFX_HOME}/lib" ]]; then
    echo "${JAVAFX_HOME}"
    return
  fi

  if [[ -d ".javafx/lib" ]]; then
    echo ".javafx"
    return
  fi

  local candidate
  candidate="$(ls -d javafx-sdk-* 2>/dev/null | sort -V | tail -n 1 || true)"
  if [[ -n "$candidate" && -d "$candidate/lib" ]]; then
    echo "$candidate"
    return
  fi

  echo ""
}

JAVAFX_DIR="$(find_javafx_dir)"
if [[ -n "$JAVAFX_DIR" ]]; then
  echo "[setup] OK: $JAVAFX_DIR/lib"

  if [[ "$JAVAFX_DIR" != ".javafx" ]]; then
    ln -sfn "$JAVAFX_DIR" ".javafx"
    echo "[setup] Linked .javafx -> $JAVAFX_DIR"
  fi

  if [[ "$(uname -s)" == "Darwin" && -f "$JAVAFX_DIR/lib/libglass.dylib" ]]; then
    SYS_ARCH="$(uname -m)"
    SDK_ARCH="$(file "$JAVAFX_DIR/lib/libglass.dylib" | awk '{print $NF}')"
    if [[ "$SYS_ARCH" == "arm64" && "$SDK_ARCH" == "arm64e" ]]; then
      SDK_ARCH="arm64"
    fi
    if [[ "$SDK_ARCH" != "$SYS_ARCH" ]]; then
      echo "[setup] WARNING: JavaFX SDK architecture mismatch."
      echo "[setup] System: $SYS_ARCH, JavaFX: $SDK_ARCH"
      echo "[setup] Download JavaFX SDK for macOS-$SYS_ARCH."
    fi
  fi
else
  echo "[setup] JavaFX SDK not found."
  echo "[setup] Use one of options:"
  echo "  1) put javafx-sdk-<version> in project root"
  echo "  2) export JAVAFX_HOME=/absolute/path/to/javafx-sdk"
fi

mkdir -p data audio

cat <<MSG
[setup] Done.
[setup] Next:
  ./scripts/run.sh

Optional:
  export TODOLIST_AUDIO_DIR="/audio"
MSG
