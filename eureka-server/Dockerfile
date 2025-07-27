# eureka-server/Dockerfile
FROM openjdk:24-jdk-slim 
# Changed from 17 to 24
WORKDIR /app
COPY target/eureka-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]