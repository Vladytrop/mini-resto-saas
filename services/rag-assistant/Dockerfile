FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY build/libs/* app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
