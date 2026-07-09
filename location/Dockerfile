# CI trigger: rebuild location for v1.0.3 release candidate.
FROM eclipse-temurin:25-jre-alpine
COPY target/location*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
