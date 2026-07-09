# CI trigger: rebuild customer for v1.0.3 release candidate.
FROM eclipse-temurin:25-jre-alpine
COPY target/customer*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
