#!/usr/bin/env bash
set -euo pipefail

# Exercise the real entrypoint without connecting to or modifying a database.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

mysql() {
  local charset="" execute="false" arg
  for arg in "$@"; do
    case "$arg" in
      --default-character-set=*) charset="${arg#*=}" ;;
      --execute=*) execute="true" ;;
    esac
  done
  if [[ "$charset" != "utf8mb4" ]]; then
    echo "FAIL: mysql invocation did not select utf8mb4" >&2
    return 1
  fi
  # No real MySQL binary is called. Consume each actual SQL input as a byte stream.
  if [[ "$execute" != "true" ]]; then
    cat >/dev/null
  fi
}

for mode in new upgrade; do
  (
    args=(--database forge_charset_test --with-demo --with-optional --with-module)
    if [[ "$mode" == "upgrade" ]]; then
      args+=(--skip-admin-init)
    fi
    source "$ROOT/forge-server/scripts/db/init-db.sh" "${args[@]}"
  ) >/dev/null
  echo "PASS: $mode initialization selects utf8mb4 for every mysql invocation"
done
