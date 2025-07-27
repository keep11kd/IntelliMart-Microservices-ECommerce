    # Use a slim OpenJDK image as the base. OpenJDK 17 is specified in your parent POM.
    FROM openjdk:17-jdk-slim

    # Set the working directory inside the container
    WORKDIR /app

    # Copy the built JAR file from your Maven target directory into the container's /app directory.
    COPY target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

    # Expose the port that the Spring Boot application will run on (api-gateway runs on 8081).
    EXPOSE 8081

    # Define the command to run your Spring Boot application when the container starts.
    ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8081"]

    # Optional: Add metadata for the Docker image
    LABEL maintainer="Your Name <your.email@example.com>"
    LABEL version="0.0.1"
    LABEL description="Docker image for IntelliMart API Gateway"
    