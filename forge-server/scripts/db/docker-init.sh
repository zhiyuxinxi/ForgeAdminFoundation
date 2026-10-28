#!/usr/bin/env bash
# MySQL entrypoint sources non-executable .sh hooks; isolate shell options.
(
  set -euo pipefail
  : "${MYSQL_DATABASE:?MYSQL_DATABASE is required}"
  : "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is required}"
  # The image's temporary initialization server accepts only its local socket.
  # Keep the password out of argv and reuse the canonical SQL/seed/migration path.
  MYSQL_PWD="$MYSQL_ROOT_PASSWORD" bash /opt/forge/forge-server/scripts/db/init-db.sh \
    --host localhost --database "$MYSQL_DATABASE" --user root
)
