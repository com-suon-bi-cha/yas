# CI trigger: rebuild product for v1.0.3 release candidate.
FROM eclipse-temurin:25-jre-alpine
COPY target/product*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
