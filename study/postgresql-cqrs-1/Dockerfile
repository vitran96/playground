FROM docker.io/library/gradle:9.7.0-jdk25 AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src src
RUN gradle bootJar --no-daemon -x test

FROM docker.io/library/eclipse-temurin:25-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
