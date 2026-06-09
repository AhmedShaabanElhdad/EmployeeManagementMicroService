# Employee Management System (EMS) - Microservices

A robust, production-grade Employee Management System built with Java 17, Spring Boot 3, and a modern microservices
architecture. This project demonstrates advanced distributed systems patterns including Saga, Transactional Outbox, and
gRPC.

## 🏗 System Architecture

```mermaid
graph TD
    Client[Client/Browser] -->|HTTP| Gateway[API Gateway :8080]
    
    subgraph Infrastructure
        Eureka[Eureka Server :8761]
        Redis[Redis Cache]
        Rabbit[RabbitMQ]
        Kafka[Kafka Broker]
        Zipkin[Zipkin Tracing]
    end

    Gateway --> Auth[Auth Service :8081]
    Gateway --> Employee[Employee Service :9000]
    Gateway --> Dept[Department Service :8082]

    Employee <-->|gRPC| Dept
    
    Employee -.->|Transactional Outbox| Rabbit
    Rabbit -.-> Notification[Notification Service]
    
    Auth -.->|Async Kafka| Employee
    
    Employee == Saga Flow ==> Kafka
    Kafka ==> Payroll[Payroll Service :8084]
    Payroll == Response ==> Kafka
    Kafka ==> Employee
    
    Notification -->|Email| SMTP[Email Server]
```

## 🛠 Tech Stack

- **Framework**: Spring Boot 3.3.5, Spring Cloud 2023.0.3
- **Language**: Java 17
- **Communication**:
    - **REST**: External & Internal API calls (Feign)
    - **gRPC**: High-performance internal service-to-service communication
    - **Kafka**: Asynchronous event-driven flows (Saga Pattern)
    - **RabbitMQ**: Reliable messaging (Transactional Outbox Pattern)
- **Data**: PostgreSQL (Database per service), Redis (Distributed Caching)
- **Observability**: Zipkin (Distributed Tracing), Spring Actuator, Micrometer
- **Deployment**: Docker Compose, Kubernetes (Kustomize)

## 📦 Services Overview

1. **API Gateway**: Central entry point. Handles routing, rate limiting (Redis), and JWT authentication.
2. **Auth Service**: Manages user accounts, login, and token generation. Uses Kafka to notify Employee service.
3. **Employee Service**: The core domain service. Implements **Transactional Outbox** for consistency and **Saga**
   orchestration.
4. **Department Service**: Manages department data. Provides a **gRPC server** for Employee service.
5. **Payroll Service**: Participates in the **Saga flow** to automatically provision payroll for new employees.
6. **Notification Service**: Consumes RabbitMQ messages to send registration emails.
7. **Eureka Server**: Service discovery registry.
8. **Shared Module**: Common DTOs, Exceptions, and gRPC Proto contracts.

## 🚀 How to Run Locally

### Prerequisites

- Docker & Docker Compose
- Java 17 (if running without Docker)
- Gradle 8.x

### 1. Build the project

From the root directory:

```bash
./gradlew clean build -x test
```

### 2. Run with Docker Compose

This will spin up all microservices and infrastructure (Postgres, Kafka, etc.):

```bash
docker compose up --build
```

Access the system via the API Gateway at `http://localhost:8080`.

### 3. Run with Kubernetes 🚴🏾

If you have a k8s cluster (Minikube/Docker Desktop):

```bash
kubectl apply -k k8s/
```

## 🔐 Security

- **JWT Authentication**: Tokens are issued by the Auth Service and validated at the API Gateway.
- **RBAC (Role-Based Access Control)**:
    - **USER**: Standard access to employee features.
    - **ADMIN**: Access to administrative endpoints (`/api/v1/admin/**`).
    - **SERVICE**: Reserved for secure internal service-to-service communication.
- **Internal API Protection**: All endpoints under `/internal/**` are restricted to the `SERVICE` role using Spring Security's `.requestMatchers("/internal/**").hasRole("SERVICE")`.
- **Service-to-Service Auth**: 
    - Services use a `Feign RequestInterceptor` to automatically inject a System JWT token with the `SERVICE` role for internal calls.
    - `JwtAuthenticationConverter` is configured across microservices to correctly map the custom `role` claim from tokens to Spring Security authorities.
- **BCrypt**: Passwords are securely hashed using BCrypt before storage.

## 📈 Distributed Patterns Applied

- **Saga Pattern (Choreography)**: Ensures data consistency across Employee and Payroll services using Kafka.
- **Transactional Outbox**: Guarantees that database updates and message publishing are atomic.
- **Circuit Breaker (Resilience4j)**: Handles failures in downstream services gracefully.
- **Database per Service**: Ensures loose coupling and independent scalability.
