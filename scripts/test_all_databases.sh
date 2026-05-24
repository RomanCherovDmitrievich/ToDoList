#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

mkdir -p tests/reports/db-checks

DB_CONFIG="data/db.properties"
BACKUP=""
if [[ -f "$DB_CONFIG" ]]; then
  BACKUP="$(mktemp)"
  cp "$DB_CONFIG" "$BACKUP"
fi

cleanup() {
  if [[ -n "$BACKUP" && -f "$BACKUP" ]]; then
    cp "$BACKUP" "$DB_CONFIG"
    rm -f "$BACKUP"
  else
    rm -f "$DB_CONFIG"
  fi
}
trap cleanup EXIT

cp data/db.properties.example "$DB_CONFIG"

run_probe() {
  local db_type="$1"
  local output_file="tests/reports/db-checks/${db_type}_probe.txt"
  python3 - "$db_type" "$DB_CONFIG" <<'PY'
from pathlib import Path
import sys

db_type = sys.argv[1]
config_path = Path(sys.argv[2])
lines = config_path.read_text(encoding="utf-8").splitlines()
for i, line in enumerate(lines):
    if line.startswith("db.type="):
        lines[i] = f"db.type={db_type}"
        break
config_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
PY
  ./scripts/test_db_connection.sh | tee "$output_file"
}

run_probe sqlite
run_probe postgresql
run_probe mysql

echo "[db-check] Done. Reports:"
echo "  tests/reports/db-checks/sqlite_probe.txt"
echo "  tests/reports/db-checks/postgresql_probe.txt"
echo "  tests/reports/db-checks/mysql_probe.txt"
