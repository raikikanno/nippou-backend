# JARビルドフェーズ
FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/app
WORKDIR /home/app
RUN gradle clean bootJar --no-daemon

# 実行フェーズ（軽量なJRE）
FROM eclipse-temurin:17-jdk-alpine
COPY --from=build /home/app/build/libs/nippou-backend-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
