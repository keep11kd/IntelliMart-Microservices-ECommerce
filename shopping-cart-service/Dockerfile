    # Use a slim OpenJDK image as the base. OpenJDK 17 is specified in your parent POM.
    FROM openjdk:17-jdk-slim

    # Set the working directory inside the container
    WORKDIR /app

    # Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
    # Copy the built JAR file from your Maven target directory into the container's /app directory.
    # Ensure you've run 'mvn clean install' in your project root before building this Docker image.
    COPY target/shopping-cart-service-0.0.1-SNAPSHOT.jar app.jar

    # Expose the port that the Spring Boot application will run on (shopping-cart-service runs on 8083).
    EXPOSE 8083

    # Define the command to run your Spring Boot application when the container starts.
    # We explicitly set server.port to 8083. The database URL will be picked from application.properties.
    ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8083"]

    # Optional: Add metadata for the Docker image
    LABEL maintainer="Abdul Bari <bariabdul11ab@gmail.com>"
    LABEL version="0.0.1"
    LABEL description="Docker image for IntelliMart Shopping Cart Service"
    