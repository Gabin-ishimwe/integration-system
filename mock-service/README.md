# Mock Service

WireMock-based service that simulates external CRM and Inventory APIs for development and testing.

## Technology Stack

- Java 17
- Spring Boot 3.2.0
- WireMock 3.3.1

## Ports

| Port | Service |
|------|---------|
| 8080 | Spring Boot application |
| 8081 | WireMock server (mock APIs) |

## Mock Endpoints

### CRM API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /crm/api/customers | Get paginated customers |
| POST | /crm/api/customers | Create customer |

### Inventory API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /inventory/api/products | Get paginated products |

## Authentication

Mock endpoints expect Bearer token authentication:

```
Authorization: Bearer mock-token
```

## Running Locally

### Prerequisites

- Java 17
- Maven 3.8+

### Start the Service

```bash
# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run

# Or run the JAR
java -jar target/mock-service-1.0.0.jar
```

### With Docker

```bash
# Build image
docker build -t mock-service .

# Run container
docker run -p 8080:8080 -p 8081:8081 mock-service
```

## WireMock Stubs

Mock responses are configured in the WireMock stub mappings. The service returns sample customer and product data for testing the integration pipeline.

### Sample Customer Response

```json
{
  "content": [
    {
      "customer_id": "CUST001",
      "first_name": "John",
      "last_name": "Doe",
      "email": "john@example.com",
      "phone": "123-456-7890",
      "status": "ACTIVE"
    }
  ],
  "page": 0,
  "size": 100,
  "total": 1
}
```

### Sample Product Response

```json
{
  "content": [
    {
      "product_id": "PROD001",
      "name": "Widget",
      "category": "Electronics",
      "price": 29.99,
      "stock_level": 100,
      "customer_id": "CUST001"
    }
  ],
  "page": 0,
  "size": 100,
  "total": 1
}
```
