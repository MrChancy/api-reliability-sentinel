# ---- build ----
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

# 3) COPY 源码（sentinel-app + 依赖库模块）
COPY probe/src probe/src
COPY alert/src alert/src
COPY sentinel-app/src sentinel-app/src
COPY sentinel-common/src sentinel-common/src

# 4) 打包 sentinel-app
RUN mvn -q -DskipTests -pl sentinel-app -am package

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/sentinel-app/target/*.jar app.jar
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
