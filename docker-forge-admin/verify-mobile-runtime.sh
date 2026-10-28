#!/usr/bin/env bash

set -Eeuo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd -P)"
cd "${ROOT}"

for required in \
  docker-forge-admin/Dockerfile.app \
  docker-forge-admin/Dockerfile.ui \
  docker-forge-admin/nginx.conf \
  forge-h5-ui/package.json \
  forge-h5-ui/pnpm-lock.yaml \
  forge-h5-ui/pnpm-workspace.yaml; do
  [[ -f "${required}" && ! -L "${required}" ]] \
    || { printf 'Missing or unsafe mobile runtime input: %s\n' "${required}" >&2; exit 1; }
done

grep -F 'AS h5-builder' docker-forge-admin/Dockerfile.ui >/dev/null
grep -F 'pnpm@10.34.5' docker-forge-admin/Dockerfile.ui >/dev/null
grep -F '/usr/share/nginx/html/forge-h5' docker-forge-admin/Dockerfile.ui >/dev/null
grep -F 'location /forge-h5/' docker-forge-admin/nginx.conf >/dev/null
grep -F 'location /forge-h5-api/' docker-forge-admin/nginx.conf >/dev/null
grep -F 'set $forge_app_backend forge-app:8583;' docker-forge-admin/nginx.conf >/dev/null
grep -F 'rewrite ^/forge-h5-api/(.*)$ /$1 break;' docker-forge-admin/nginx.conf >/dev/null
grep -F 'proxy_pass http://$forge_app_backend;' docker-forge-admin/nginx.conf >/dev/null
grep -F 'proxy_pass http://forge-flow:8081/api/flow/' docker-forge-admin/nginx.conf >/dev/null
grep -F '"packageManager": "pnpm@10.34.5"' forge-h5-ui/package.json >/dev/null
grep -F 'esbuild: true' forge-h5-ui/pnpm-workspace.yaml >/dev/null

if grep -Eq 'VITE_DEBUG_CONSOLE=1|READ_LOGS|READ_PHONE_STATE|GET_ACCOUNTS|WRITE_SETTINGS|MOUNT_UNMOUNT_FILESYSTEMS' \
    forge-h5-ui/.env.production forge-h5-ui/src/manifest.json; then
  printf 'Production H5/App configuration still contains a forbidden debug or Android permission capability.\n' >&2
  exit 1
fi

printf 'PASS mobile runtime static contract\n'
