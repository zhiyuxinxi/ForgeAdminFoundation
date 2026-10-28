# ============================================
# Forge App 移动端 API 服务 Dockerfile
# ============================================
# 构建上下文为项目根目录，docker compose 中 context 设为 ..
FROM maven:3.9.16-eclipse-temurin-17 AS builder
WORKDIR /build
COPY ./forge-server .
# 单一 reactor 构建：一次编译同时产出 Admin、App 与 Flow 三个可运行 JAR；
# Dockerfile.admin、Dockerfile.app 与 Dockerfile.flow 的 builder 阶段逐字节一致，
# buildx 按内容寻址去重共享 stage，公共模块只编译一次。
# BuildKit cache mount：持久缓存 Maven 依赖（/root/.m2），sharing=locked 防并发写坏；仅缓存依赖仓库、
# 不缓存业务 target/JAR。builder 为全新文件系统，不再执行无意义的 clean。
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -ntp package -pl forge-admin-server,forge-app-server,forge-flow/forge-flow-server -am -Pprod,fast-package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/forge-app-server/target/forge-app-server.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=prod
ENV MYSQL_HOST=mysql
ENV MYSQL_PORT=3306
ENV MYSQL_DB=forge_admin
ENV MYSQL_USER=root
ENV MYSQL_PASSWORD=""
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379
ENV REDIS_PASSWORD=""
ENV FLOW_CLIENT_URL=http://forge-flow:8081

EXPOSE 8583

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} \
  -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
  -Dforge.flow.client.url=${FLOW_CLIENT_URL} \
  -jar app.jar"]
