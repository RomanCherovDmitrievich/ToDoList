#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

rm -rf bin dist
find . -name "*.class" -type f -delete

echo "Clean complete: removed bin/, dist/, and stray .class files."
