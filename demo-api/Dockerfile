# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# 1) COPY 根 pom + 各模块 pom（确保父pom声明的 modules 都存在）
COPY pom.xml ./
COPY demo-api/pom.xml demo-api/pom.xml
COPY sentinel-app/pom.xml sentinel-app/pom.xml
COPY probe/pom.xml probe/pom.xml
COPY alert/pom.xml alert/pom.xml
COPY sentinel-common/pom.xml sentinel-common/pom.xml

# 2) 预拉依赖（只针对 demo-api，但需要解析父工程）
RUN mvn -q -e -DskipTests -pl demo-api -am dependency:go-offline

# 3) COPY 源码（只拷 demo-api 即可）
COPY demo-api/src demo-api/src

# 4) 打包 demo-api
RUN mvn -q -DskipTests -pl demo-api -am package

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/demo-api/target/*.jar app.jar
ENV SERVER_PORT=8081
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
