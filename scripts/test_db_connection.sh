#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

./scripts/run.sh --build-only

OS="$(uname -s 2>/dev/null || echo UNKNOWN)"
if [[ "$OS" == MINGW* || "$OS" == MSYS* || "$OS" == CYGWIN* ]]; then
  CP_SEP=';'
else
  CP_SEP=':'
fi

CLASSPATH="bin${CP_SEP}lib/gson-2.10.1.jar${CP_SEP}lib/sqlite-jdbc-3.45.1.0.jar${CP_SEP}lib/slf4j-api-2.0.12.jar${CP_SEP}lib/slf4j-nop-2.0.12.jar"
if [[ -f "lib/postgresql-42.7.4.jar" ]]; then
  CLASSPATH="${CLASSPATH}${CP_SEP}lib/postgresql-42.7.4.jar"
fi
if [[ -f "lib/mysql-connector-j-9.3.0.jar" ]]; then
  CLASSPATH="${CLASSPATH}${CP_SEP}lib/mysql-connector-j-9.3.0.jar"
fi
if [[ -f "lib/jaybird.jar" ]]; then
  CLASSPATH="${CLASSPATH}${CP_SEP}lib/jaybird.jar"
fi
if [[ -f "lib/jaybird-5.0.5.jar" ]]; then
  CLASSPATH="${CLASSPATH}${CP_SEP}lib/jaybird-5.0.5.jar"
fi
if [[ -f "lib/jaybird-4.0.10.jar" ]]; then
  CLASSPATH="${CLASSPATH}${CP_SEP}lib/jaybird-4.0.10.jar"
fi

java -cp "$CLASSPATH" util.DatabaseCli probe
