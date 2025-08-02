IntelliMart Microservices E-Commerce
Project Description
IntelliMart is a modern, scalable e-commerce platform built on a microservices architecture using the Spring Boot ecosystem. It demonstrates the implementation of core e-commerce functionalities such as user authentication, product management, shopping cart, and order processing. The services are orchestrated via Spring Cloud components for service discovery and API routing, and the entire system is containerized for streamlined deployment with Docker.

This project emphasizes a decoupled, resilient design, leveraging asynchronous communication through a message broker and applying best practices for building enterprise-grade applications.

Key Features
User Management: Secure user registration, login, and JWT-based authentication via auth-service.

Product Catalog: A dedicated product-service for managing product information and inventory.

Shopping Cart: Real-time shopping cart management with a separate shopping-cart-service.

Order Processing: A centralized order-service to handle the complex business logic of placing orders.

Asynchronous Notifications: Uses RabbitMQ to send asynchronous notifications (e.g., order confirmations) via a notification-service.

API Gateway: A single entry point for all client requests, providing routing, load balancing, and cross-cutting concerns like security and resilience.

Admin Dashboard: A basic admin dashboard built with JSP, HTML, and JavaScript to manage core functionality.

Containerization: The entire application stack is containerized with Docker and orchestrated with Docker Compose.

Tech Stack
Backend Services
Language: Java 17

Framework: Spring Boot 3

Service Discovery: Spring Cloud Eureka

API Gateway: Spring Cloud Gateway

Asynchronous Messaging: RabbitMQ

Database: MySQL 8.0

Data Access: Spring Data JPA

Authentication: JWT (JSON Web Tokens)

Resilience: Resilience4J

Containerization & Deployment
Container Runtime: Docker

Orchestration: Docker Compose

CI/CD: Jenkins (optional, for automation)

Architecture
The system follows a classic microservices architecture with the following components:

eureka-server: The central service registry. All microservices register with it to be discoverable.

api-gateway: The front-facing service that handles all incoming requests and routes them to the appropriate microservice based on the path.

auth-service: Handles user authentication, including user registration, login, and JWT token generation and validation.

product-service: Manages the product catalog, including inventory.

shopping-cart-service: Manages user-specific shopping cart data.

order-service: Handles the order lifecycle, including communicating with other services to confirm inventory and send notifications.

notification-service: A message consumer that listens to RabbitMQ events and processes notifications.

rabbitmq: A lightweight message broker used for inter-service asynchronous communication.

mysql: The database for all persistent data across services.

+-----------+            +-----------------+
|   Client  | ---+-----> |  API Gateway    |
+-----------+    |       +-----------------+
                 |       /      |       |      \
                 |      /       |       |       \
                 v     v        v       v        v
         +-------------+  +------------+  +-------------+  +-------------+
         | Auth Service|  | Product    |  | Order       |  | Shopping    |
         +-------------+  | Service    |  | Service     |  | Cart Service|
         |  (MySQL)    |  +------------+  +-------------+  +-------------+
         +-------------+       |           |   |
                               |           |   |
                    +----------+-----------+   |
                    |         |                v
                    |       +---------------------+
                    |       |    RabbitMQ         |
                    |       +---------------------+
                    |          |
                    v          v
          +-------------+  +-------------+
          | Inventory   |  | Notification|
          | Management  |  | Service     |
          +-------------+  +-------------+

(This is a simplified diagram. A more detailed UML diagram would show communication protocols and data flows.)

Getting Started
Prerequisites
Docker: Docker is essential for containerizing and running all services.

Maven: Used to build the Java microservice JAR files.

Java 17: The required JDK version for the project.

Building the Services
Navigate to the project's root directory and run the following Maven command to build all the microservice JAR files.

mvn clean install

Running the Application
After the build is successful, use Docker Compose to start all the services:

docker-compose up --build

This command will build the Docker images (if they are not already built), create the necessary containers, and link them together as defined in the docker-compose.yml file.

Accessing the Services
Once the containers are up and running, you can access the following dashboards:

Eureka Dashboard: http://localhost:8761
(Here you can monitor the health and registration status of all microservices.)

API Gateway: http://localhost:8081

Swagger UI (API Docs): http://localhost:8081/swagger-ui.html
(This provides a comprehensive and interactive documentation of all your API endpoints.)

API Endpoints
All services are exposed via the API Gateway. The following table provides a brief overview of the main endpoints. For full details, refer to the Swagger UI.

Service

Endpoint

Description

Auth Service

POST /api/auth/register

Registers a new user.



POST /api/auth/login

Authenticates a user and returns a JWT token.

Product Service

GET /api/products

Retrieves a list of all products.



POST /api/products

Creates a new product.

Shopping Cart

POST /api/cart/add

Adds an item to the shopping cart.

Order Service

POST /api/orders

Places a new order.



GET /api/orders/{userId}

Retrieves a user's order history.

License
This project is licensed under the MIT License.
