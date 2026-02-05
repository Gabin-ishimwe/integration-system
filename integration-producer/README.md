# Integration Producer

Spring Boot service for fetching data from external systems and publishing to RabbitMQ queues.

## Technology Stack

- Java 17
- Spring Boot 3.2.0
- Spring AMQP (RabbitMQ)
- Spring Data Redis
- Resilience4j (Circuit Breaker)
- SpringDoc OpenAPI (Swagger)

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/callback/fetch-all | Fetch and publish customers and products |
| POST | /api/callback/fetch-customers | Fetch and publish customers only |
| POST | /api/callback/fetch-products | Fetch and publish products only |

## Configuration

Application configuration is in `src/main/resources/application.yml`:

```yaml
server:
  port: 8082

spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin123

integration:
  crm:
    base-url: http://localhost:8081
  inventory:
    base-url: http://localhost:8081
```

## RabbitMQ Setup

**Exchange:** `integration.exchange` (Direct)

**Queues:**
- `customer.data.queue` (routing key: `customer.data`)
- `inventory.data.queue` (routing key: `inventory.data`)

## Running Locally

### Prerequisites

- Java 17
- Maven 3.8+
- RabbitMQ running on localhost:5672
- Redis running on localhost:6379
- Mock service running on localhost:8081

### Start the Service

```bash
# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run

# Or run the JAR
java -jar target/integration-producer-1.0.0.jar
```

### With Docker

```bash
# Build image
docker build -t integration-producer .

# Run container
docker run -p 8082:8082 \
  -e SPRING_RABBITMQ_HOST=host.docker.internal \
  integration-producer
```

## API Documentation

Swagger UI: http://localhost:8082/swagger-ui.html

OpenAPI JSON: http://localhost:8082/v3/api-docs

## Message Format

Messages published to RabbitMQ follow this structure:

```json
{
  "correlationId": "uuid",
  "timestamp": "2024-01-01T00:00:00Z",
  "source": "integration-producer",
  "data": [...]
}
```
