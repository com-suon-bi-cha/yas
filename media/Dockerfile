# CI trigger: rebuild media for v1.0.3 release candidate.
FROM eclipse-temurin:25-jre-alpine
COPY target/media*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
