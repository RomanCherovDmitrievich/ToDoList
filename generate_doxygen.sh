#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

echo "[doxygen] Generating documentation..."

if ! command -v doxygen >/dev/null 2>&1; then
  echo "[doxygen] Doxygen is not installed."
  echo "[doxygen] Install it first, for example: brew install doxygen graphviz"
  exit 1
fi

if ! command -v dot >/dev/null 2>&1; then
  echo "[doxygen] Graphviz is not installed. Diagrams will be skipped."
fi

mkdir -p docs
rm -rf docs/*

doxygen Doxyfile

echo "[doxygen] Done."
echo "[doxygen] Open docs/html/index.html"
