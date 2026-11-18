# Build multi-stage para Spring Boot / Java 21
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN command -v curl && groupadd --system matriz && useradd --system --gid matriz matriz
COPY --from=build /app/target/*.jar /app/matriz.jar
USER matriz
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/matriz.jar"]
