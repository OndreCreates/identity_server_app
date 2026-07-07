# Build stage: only invalidated by pom.xml/src changes, not by files outside the build context
# that the .dockerignore already excludes (target/, .git, demo-client/, etc).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# Runtime stage: no Maven, no source, no build cache -- just a JRE and the packaged jar.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/identity-server-*.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]
