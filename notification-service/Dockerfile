# Use OpenJDK 17 base image
FROM openjdk:17-jdk-alpine
# Install curl for health checks and debugging using apk
# The --no-cache option and rm -rf /var/cache/apk/* part helps keep the image size down
RUN apk add --no-cache curl && rm -rf /var/cache/apk/*

# Set working directory
WORKDIR /app

# Copy the jar from target
COPY target/*.jar app.jar

# Expose the port the service listens on (e.g., 8082 for product)
EXPOSE 8085

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]


# Optional: Add metadata labels for the Docker image.
LABEL maintainer="Abdul Bari <bariabdul11ab@gmail.com>"
LABEL version="0.0.1"
LABEL description="Docker image for IntelliMart Auth Service"