    # Use a slim OpenJDK image as the base. OpenJDK 17 is specified in your parent POM.
    FROM openjdk:17-jdk-slim

    # Set the working directory inside the container
    WORKDIR /app

    # Copy the built JAR file from your Maven target directory into the container's /app directory.
    # Ensure you've run 'mvn clean install' in your project root before building this Docker image.
    COPY target/order-service-0.0.1-SNAPSHOT.jar app.jar

    # Expose the port that the Spring Boot application will run on (order-service runs on 8084).
    EXPOSE 8084

    # Define the command to run your Spring Boot application when the container starts.
    # We explicitly set server.port to 8084. The database URL will be picked from application.properties.
    ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8084"]

    # Optional: Add metadata for the Docker image
    LABEL maintainer="Abdul Bari <bariabdul11ab@gmail.com>"
    LABEL version="0.0.1"
    LABEL description="Docker image for IntelliMart Order Service"
    