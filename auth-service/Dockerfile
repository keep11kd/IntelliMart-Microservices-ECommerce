# Use a slim OpenJDK image as the base. OpenJDK 17 is specified in your parent POM.
# This image is optimized for smaller size and security.
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the built JAR file from your Maven target directory into the container's /app directory.
# Assuming your JAR is named 'auth-service-0.0.1-SNAPSHOT.jar' after a successful Maven build.
# Adjust the path if your JAR name or location is different.
COPY target/auth-service-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that the Spring Boot application will run on.
# Your auth-service typically runs on port 8080 by default, or as configured in application.properties.
EXPOSE 8089

# Define the command to run your Spring Boot application when the container starts.
# 'java -jar app.jar' starts the application.
# '-Dspring.profiles.active=prod' can be added if you have a 'prod' profile.
# '-Dspring.datasource.url=jdbc:mysql://host.docker.internal:3306/intellimart_auth_db'
# This is crucial for connecting to MySQL running on your host machine from inside Docker.
# Replace 'intellimart_auth_db' with your actual database name if different.
# Ensure your application.properties has the correct username and password.
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.datasource.url=jdbc:mysql://host.docker.internal:3306/intellimart_auth_db"]

# Optional: Add metadata for the Docker image
LABEL maintainer="Your Name <your.email@example.com>"
LABEL version="0.0.1"
LABEL description="Docker image for IntelliMart Auth Service"
