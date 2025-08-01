# Use OpenJDK 17 base image
FROM openjdk:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy the jar from target
COPY target/*.jar app.jar

# Expose the port the service listens on (e.g., 8082 for product)
EXPOSE 8082

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]


# Optional: Add metadata labels for the Docker image.
LABEL maintainer="Abdul Bari <bariabdul11ab@gmail.com>"
LABEL version="0.0.1"
LABEL description="Docker image for IntelliMart Auth Service"