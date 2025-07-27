# Use a slim OpenJDK image as the base. OpenJDK 17 is specified in your parent POM.
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the built JAR file from your Maven target directory into the container's /app directory.
# Assuming your JAR is named 'product-service-0.0.1-SNAPSHOT.jar' after a successful Maven build.
# Adjust the path if your JAR name or location is different.
COPY target/product-service-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that the Spring Boot application will run on.
# Your product-service typically runs on port 8081 by default (or as configured).
EXPOSE 8082

# Define the command to run your Spring Boot application when the container starts.
# '-Dspring.datasource.url=jdbc:mysql://host.docker.internal:3306/intellimart_product_db'
# This is crucial for connecting to MySQL running on your host machine from inside Docker.
# Replace 'intellimart_product_db' with your actual database name if different.
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8082"]

# Optional: Add metadata for the Docker image
LABEL maintainer="Your Name <your.email@example.com>"
LABEL version="0.0.1"
LABEL description="Docker image for IntelliMart Product Service"
