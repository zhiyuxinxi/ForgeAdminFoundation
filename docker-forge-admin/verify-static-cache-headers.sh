#!/usr/bin/env bash
# 静态资源缓存响应头集成测试（需 Docker，由 Runner 或临时 nginx 镜像执行）
#
# 限制说明：Forge TaskSpace 无 Docker Socket 与 nginx 二进制，本脚本无法在本工作舱执行；
# 只能作为候选产物由具备 Docker 的 Runner / 人工在临时 nginx 镜像上运行，用于在发布前
# 验证真实响应头。不要把仅 HTTP 200 冒烟当作 nginx 配置语法或响应头的验证。
#
# 用法（在仓库根目录，且已构建 H5 与 PC 产物）：
#   bash docker-forge-admin/verify-static-cache-headers.sh
#
# 覆盖：根入口、显式 index、任意不存在 SPA 路径 fallback、哈希资源 immutable、
# 非哈希资源不 immutable、API 不带静态缓存头。
set -Eeuo pipefail

ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd -P)"
H5_DIST="${ROOT}/forge-h5-ui/dist/build/h5"
PC_DIST="${ROOT}/forge-admin-ui/dist"
CONF="${ROOT}/docker-forge-admin/nginx.conf"
PORT=18080
NAME="forge-cache-verify-$$"

failures=0
pass() { printf 'PASS %s\n' "$1"; }
fail() { printf 'FAIL %s — %s\n' "$1" "$2"; failures=$((failures + 1)); }

check_header() {
  local name="$1" url="$2" header="$3" expected="$4"
  local value
  value="$(curl -s -o /dev/null -D - "http://127.0.0.1:${PORT}${url}" | tr -d '\r' | grep -i "^${header}:" | head -1 | cut -d: -f2- | sed 's/^ //')"
  if [[ "${value}" == "${expected}" ]]; then
    pass "${name} (${url} → ${header}: ${value})"
  else
    fail "${name}" "expected ${header}: ${expected}, got: ${value}"
  fi
}

check_header_absent() {
  local name="$1" url="$2" header="$3"
  local raw
  raw="$(curl -s -o /dev/null -D - "http://127.0.0.1:${PORT}${url}" | tr -d '\r' | grep -i "^${header}:" || true)"
  if [[ -z "${raw}" ]]; then
    pass "${name} (${url} → no ${header})"
  else
    fail "${name}" "unexpected ${header}: ${raw}"
  fi
}

[[ -d "${H5_DIST}" && -f "${H5_DIST}/index.html" ]] || { echo "缺少 H5 产物：${H5_DIST}"; exit 2; }
[[ -d "${PC_DIST}" && -f "${PC_DIST}/index.html" ]] || { echo "缺少 PC 产物：${PC_DIST}"; exit 2; }
command -v docker >/dev/null || { echo "缺少 docker（本工作舱限制，需 Runner/临时 nginx 镜像执行）"; exit 2; }

WORK="$(mktemp -d)"
trap 'docker rm -f "${NAME}" >/dev/null 2>&1 || true; rm -rf "${WORK}"' EXIT
mkdir -p "${WORK}/html/forge" "${WORK}/html/forge-h5"
cp -r "${PC_DIST}/." "${WORK}/html/forge/"
cp -r "${H5_DIST}/." "${WORK}/html/forge-h5/"

docker run --rm --pull never -d --name "${NAME}" -p "127.0.0.1:${PORT}:80" \
  --add-host forge-admin:127.0.0.1 --add-host forge-flow:127.0.0.1 \
  -v "${CONF}:/etc/nginx/conf.d/default.conf:ro" \
  -v "${WORK}/html:/usr/share/nginx/html:ro" \
  nginx:1.25-alpine >/dev/null

for i in $(seq 1 20); do
  curl -s -o /dev/null "http://127.0.0.1:${PORT}/forge-h5/" && break
  sleep 1
done
curl --fail --silent --output /dev/null "http://127.0.0.1:${PORT}/forge-h5/" \
  || { echo "Nginx 未就绪，不能将无响应当作缓存头不存在。" >&2; exit 1; }

NO_CACHE='no-store, no-cache, must-revalidate'
HASHED_JS="$(ls "${WORK}/html/forge-h5/assets/" | grep -E '\.js$' | head -1)"
HASHED_CSS="$(ls "${WORK}/html/forge-h5/assets/" | grep -E '\.css$' | head -1)"
PC_HASHED="$(ls "${WORK}/html/forge/assets/" | grep -E '\.js$' | head -1)"

# 入口与 SPA fallback HTML：no-store 三件套 + Pragma + Expires
check_header 'H5 根入口' '/forge-h5/' 'Cache-Control' "${NO_CACHE}"
check_header 'H5 根入口 Pragma' '/forge-h5/' 'Pragma' 'no-cache'
check_header 'H5 根入口 Expires' '/forge-h5/' 'Expires' '0'
check_header 'H5 显式 index' '/forge-h5/index.html' 'Cache-Control' "${NO_CACHE}"
check_header 'PC 根入口' '/forge/' 'Cache-Control' "${NO_CACHE}"
check_header 'PC 显式 index' '/forge/index.html' 'Cache-Control' "${NO_CACHE}"
check_header 'H5 SPA fallback(不存在路径)' '/forge-h5/any/missing/route' 'Cache-Control' "${NO_CACHE}"
check_header 'PC SPA fallback(不存在路径)' '/forge/some/deep/route' 'Cache-Control' "${NO_CACHE}"

# 哈希资源：immutable
check_header 'H5 哈希 js' "/forge-h5/assets/${HASHED_JS}" 'Cache-Control' 'public, max-age=31536000, immutable'
check_header 'H5 哈希 css' "/forge-h5/assets/${HASHED_CSS}" 'Cache-Control' 'public, max-age=31536000, immutable'
check_header 'PC 哈希 js' "/forge/assets/${PC_HASHED}" 'Cache-Control' 'public, max-age=31536000, immutable'

# 非哈希资源：no-cache，绝不 immutable / 固定 3600
check_header 'H5 static logo' '/forge-h5/static/logo.png' 'Cache-Control' 'no-cache'
check_header 'PC favicon' '/forge/favicon.ico' 'Cache-Control' 'no-cache'

# API：不带静态缓存头（无后端返回 502，但头必须不存在）
check_header_absent 'H5 API 无缓存头' '/forge-h5-api/auth/captcha' 'Cache-Control'
check_header_absent 'PC API 无缓存头' '/forge-api/auth/captcha' 'Cache-Control'

docker rm -f "${NAME}" >/dev/null 2>&1 || true

if [[ "${failures}" -eq 0 ]]; then
  echo "RESULT: PASS (static cache response headers)"
else
  echo "RESULT: FAIL (${failures} check(s) failed)"
  exit 1
fi
