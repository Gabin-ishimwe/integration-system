# Analytic Service

Spring Boot service for storing and serving merged customer-product analytics data.

## Technology Stack

- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- PostgreSQL 15
- SpringDoc OpenAPI (Swagger)

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /analytics/api/data | Ingest analytics batch data |
| GET | /analytics/api/customers | Get all customers with products |
| POST | /analytics/api/customers | Add customer via SOAP |
| POST | /analytics/api/refresh | Trigger full data refresh |
| POST | /analytics/api/refresh/customers | Trigger customer refresh |
| POST | /analytics/api/refresh/products | Trigger product refresh |
| GET | /analytics/api/customers/export | Export customers as CSV |
| GET | /analytics/api/products/export | Export products as CSV |

## Configuration

Application configuration is in `src/main/resources/application.yml`:

```yaml
server:
  port: 8083

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/analytics_db
    username: analytics_user
    password: analytics_password
```

## Running Locally

### Prerequisites

- Java 17
- Maven 3.8+
- PostgreSQL 15 running on localhost:5432

### Start the Service

```bash
# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run

# Or run the JAR
java -jar target/analytic-service-1.0.0.jar
```

### With Docker

```bash
# Build image
docker build -t analytic-service .

# Run container
docker run -p 8083:8083 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/analytics_db \
  analytic-service
```

## API Documentation

Swagger UI: http://localhost:8083/swagger-ui.html

OpenAPI JSON: http://localhost:8083/v3/api-docs

## Database Schema

**customers**
- id (PK)
- external_id (unique)
- name
- email
- phone
- status
- last_batch_number
- last_analytics_timestamp

**products**
- id (PK)
- external_id
- name
- category
- price
- stock_level
- customer_id (FK)
