#!/usr/bin/env bash
# ============================================================
# 构建回归检查：验证 Admin/App/Flow 复用同一 Maven reactor 产物
# ============================================================
# 优化说明（cold/warm 构建机制）：
#   三个 Dockerfile 的 builder 阶段逐字节一致，统一执行包含 Admin、App、Flow 的 reactor。
#   一次构建同时产出三个可运行 JAR，buildx 按内容寻址去重共享公共模块。
#   /root/.m2 通过 BuildKit cache mount 持久化（warm 构建不再重复下载依赖）。
#   builder 是全新文件系统，故命令不再带 clean（避免无意义的清空与 flatten.clean）。
#
# 本脚本在不具备 Docker 权限时离线验证两点：
#   1) 三个 Dockerfile 的 builder 阶段逐字节一致（buildx 可据此去重）；
#   2) 单次 reactor 构建能同时产出三个可运行 JAR（非空）。
#
# 用法：
#   bash docker-forge-admin/verify-shared-reactor.sh --static  # 仅源码合同，不构建
#   bash docker-forge-admin/verify-shared-reactor.sh           # 显式运行离线 Maven 构建
# 可选环境变量：
#   MVN_REPO   本地 Maven 仓库路径（默认 <仓库根>/.m2-repo）
#   JANSI_TMP  jansi 临时目录（默认 <仓库根>/.jansi-tmp）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

MVN_REPO="${MVN_REPO:-$ROOT/.m2-repo}"
JANSI_TMP="${JANSI_TMP:-$ROOT/.jansi-tmp}"
MODE="${1:-build}"
[[ "$MODE" == build || "$MODE" == --static ]] || { echo "Usage: $0 [--static]" >&2; exit 2; }

# ---- 1) builder 阶段一致性 ----
echo "== [1/2] 校验三个 Dockerfile 的 builder 阶段逐字节一致 =="
extract_builder() {
  awk '/^FROM eclipse-temurin:17-jre/{exit} /^FROM maven:3.9.16-eclipse-temurin-17 AS builder/{f=1} f{print}' "$1"
}
if diff <(extract_builder docker-forge-admin/Dockerfile.admin) \
         <(extract_builder docker-forge-admin/Dockerfile.app) >/dev/null \
  && diff <(extract_builder docker-forge-admin/Dockerfile.admin) \
          <(extract_builder docker-forge-admin/Dockerfile.flow) >/dev/null; then
  echo "PASS: builder 阶段一致，buildx 将去重共享 reactor 构建"
else
  echo "FAIL: builder 阶段不一致，差异如下：" >&2
  diff <(extract_builder docker-forge-admin/Dockerfile.admin) \
       <(extract_builder docker-forge-admin/Dockerfile.app) >&2 || true
  diff <(extract_builder docker-forge-admin/Dockerfile.admin) \
       <(extract_builder docker-forge-admin/Dockerfile.flow) >&2 || true
  exit 1
fi

if [[ "$MODE" == --static ]]; then
  echo "PASS: shared reactor source contract (build not executed)"
  exit 0
fi
mkdir -p "$JANSI_TMP"

# ---- 2) 单次 reactor 构建产出三个 JAR ----
echo
echo "== [2/2] 离线执行单次 reactor 构建并断言三个 JAR 均产出 =="
cd "$ROOT/forge-server"
MAVEN_OPTS="-Djansi.tmpdir=$JANSI_TMP" \
  mvn -o -Dmaven.repo.local="$MVN_REPO" \
      -pl forge-admin-server,forge-app-server,forge-flow/forge-flow-server -am \
      package -DskipTests -Pprod

ADMIN_JAR="forge-admin-server/target/forge-admin-server.jar"
APP_JAR="forge-app-server/target/forge-app-server.jar"
FLOW_JAR="forge-flow/forge-flow-server/target/forge-flow-server.jar"
test -s "$ADMIN_JAR" || { echo "FAIL: 缺少或为空 $ADMIN_JAR" >&2; exit 1; }
test -s "$APP_JAR"   || { echo "FAIL: 缺少或为空 $APP_JAR" >&2; exit 1; }
test -s "$FLOW_JAR"  || { echo "FAIL: 缺少或为空 $FLOW_JAR" >&2; exit 1; }

echo "PASS: 单次 reactor 构建同时产出："
echo "  - $ADMIN_JAR"
echo "  - $APP_JAR"
echo "  - $FLOW_JAR"
