FROM docker.io/library/node:22-alpine AS web
WORKDIR /web
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

FROM docker.io/library/maven:3-eclipse-temurin-25 AS api
WORKDIR /api
COPY backend/pom.xml .
RUN mvn -q dependency:go-offline
COPY backend/src ./src
COPY --from=web /web/dist ./src/main/resources/static
RUN mvn -q package -DskipTests

FROM docker.io/library/eclipse-temurin:25-jre
COPY --from=api /api/target/*.jar /app.jar
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app.jar"]
