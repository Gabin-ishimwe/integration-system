# Integration System

A microservices-based data integration platform that fetches data from external systems (CRM and Inventory), processes it through message queues, aggregates it, and stores analytics data.

## Architecture Overview

```
                    External Systems (Mock Service)
                    /crm/api/customers    /inventory/api/products
                              |
                              v
                    +-------------------+
                    | integration-      |
                    | producer          | (Port 8082)
                    | [Spring Boot]     |
                    +-------------------+
                              |
              +---------------+---------------+
              |                               |
              v                               v
    customer.data.queue            inventory.data.queue
              |                               |
              +---------------+---------------+
                              |
                              v
                    +-------------------+
                    | consumer-service  | (Port 8084)
                    | [FastAPI/Python]  |
                    +-------------------+
                              |
                    Aggregates in Redis
                              |
                              v
                    +-------------------+
                    | analytic-service  | (Port 8083)
                    | [Spring Boot]     |
                    +-------------------+
                              |
                              v
                         PostgreSQL
```

## Microservices

| Service | Technology | Port | Description |
|---------|-----------|------|-------------|
| integration-producer | Java 17, Spring Boot 3.2 | 8082 | Fetches data from external APIs and publishes to RabbitMQ |
| consumer-service | Python 3.11, FastAPI | 8084 | Consumes messages, aggregates data in Redis, sends to analytics |
| analytic-service | Java 17, Spring Boot 3.2 | 8083 | Stores and serves merged customer-product analytics data |
| mock-service | Java 17, Spring Boot + WireMock | 8080/8081 | Simulates external CRM and Inventory APIs |

## Infrastructure Components

| Component | Version | Port | Purpose |
|-----------|---------|------|---------|
| RabbitMQ | 3.12 | 5672 (AMQP), 15672 (UI) | Message broker |
| Redis | 7.2 | 6379 | Data aggregation cache |
| PostgreSQL | 15 | 5432 | Analytics data persistence |

## Data Flow

1. **integration-producer** fetches customers from CRM API and products from Inventory API
2. Data is published to RabbitMQ queues (`customer.data.queue`, `inventory.data.queue`)
3. **consumer-service** consumes both queues asynchronously
4. Data is temporarily stored in Redis until both customers and products are available
5. When aggregation is complete, merged data is transformed using JSONata schema
6. Transformed data is sent to **analytic-service** for storage
7. Analytics can be queried or exported as CSV

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 17 (for local development)
- Python 3.11 (for local development)
- Maven (for local development)

### Run with Docker Compose

```bash
# Start all services
docker-compose up -d

# Or use production compose
docker-compose -f docker-compose.prod.yml up -d
```

### Verify Services

```bash
# Check RabbitMQ
curl http://localhost:15672  # UI (admin/admin123)

# Check services health
curl http://localhost:8082/actuator/health  # producer
curl http://localhost:8083/actuator/health  # analytics
curl http://localhost:8084/health           # consumer
```

### Trigger Data Flow

```bash
# Fetch all data
curl -X POST http://localhost:8084/api/trigger/fetch-all

# Or trigger via producer directly
curl -X POST http://localhost:8082/api/callback/fetch-all

# View analytics data
curl http://localhost:8083/analytics/api/customers
```

## API Documentation (Swagger)

| Service | Swagger UI URL |
|---------|---------------|
| integration-producer | http://localhost:8082/swagger-ui.html |
| analytic-service | http://localhost:8083/swagger-ui.html |
| consumer-service | http://localhost:8084/docs |

## Configuration

Each service uses environment-specific configuration:

- **Local development**: Uses `localhost` for all connections
- **Docker environment**: Uses Docker network hostnames (e.g., `rabbitmq`, `redis`, `postgres`)

Key environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| RABBITMQ_HOST | localhost | RabbitMQ hostname |
| RABBITMQ_PORT | 5672 | RabbitMQ port |
| REDIS_HOST | localhost | Redis hostname |
| POSTGRES_HOST | localhost | PostgreSQL hostname |

## Project Structure

```
integration-system/
├── analytic-service/       # Analytics data storage (Spring Boot)
├── integration-producer/   # Data fetcher and publisher (Spring Boot)
├── consumer-service/       # Message consumer and aggregator (FastAPI)
├── mock-service/           # External API simulator (WireMock)
├── docker-compose.yml      # Development environment
└── docker-compose.prod.yml # Production environment
```

## Development

See individual service README files for specific development instructions:

- [analytic-service/README.md](./analytic-service/README.md)
- [integration-producer/README.md](./integration-producer/README.md)
- [consumer-service/README.md](./consumer-service/README.md)
- [mock-service/README.md](./mock-service/README.md)
