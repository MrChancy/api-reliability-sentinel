# ---- 阶段 0: 下载 OTel Agent ----
FROM alpine:latest AS agent-downloader
RUN apk add --no-cache curl
RUN curl -L https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.23.0/opentelemetry-javaagent.jar -o /opentelemetry-javaagent.jar

# ---- 阶段 1: Maven build (你原有的构建逻辑) ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# 1) COPY 根 pom + 各模块 pom
COPY pom.xml ./
COPY sentinel-app/pom.xml sentinel-app/pom.xml
COPY probe/pom.xml probe/pom.xml
COPY alert/pom.xml alert/pom.xml
COPY demo-api/pom.xml demo-api/pom.xml
COPY sentinel-common/pom.xml sentinel-common/pom.xml

# 2) 预拉依赖
RUN mvn -q -e -DskipTests -pl sentinel-app -am dependency:go-offline

# 3) COPY 源码
COPY probe/src probe/src
COPY alert/src alert/src
COPY sentinel-app/src sentinel-app/src
COPY sentinel-common/src sentinel-common/src

# 4) 打包 sentinel-app
RUN mvn -q -DskipTests -pl sentinel-app -am package

# ---- 阶段 2: 运行镜像 (补全 OTel 挂载) ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# 从阶段 0 拷贝 Agent JAR
COPY --from=agent-downloader /opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

# 从阶段 1 拷贝业务 JAR
COPY --from=build /workspace/sentinel-app/target/*.jar app.jar

ENV SERVER_PORT=8080

EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
