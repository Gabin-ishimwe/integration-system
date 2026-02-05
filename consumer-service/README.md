# Consumer Service

FastAPI service for consuming RabbitMQ messages, aggregating data in Redis, and forwarding to the analytics service.

## Technology Stack

- Python 3.11
- FastAPI 0.109
- aio-pika (async RabbitMQ)
- Redis (async)
- JSONata (data transformation)

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /health | Health check |
| GET | / | Service status |
| POST | /api/trigger/fetch-all | Trigger full data fetch |
| POST | /api/trigger/fetch-customers | Trigger customer fetch |
| POST | /api/trigger/fetch-products | Trigger product fetch |

## Configuration

Configuration is in `app/config/settings.py` using Pydantic settings:

```python
PRODUCER_BASE_URL = "http://localhost:8082"
RABBITMQ_URL = "amqp://admin:admin123@localhost:5672/"
REDIS_URL = "redis://localhost:6379"
ANALYTICS_BASE_URL = "http://localhost:8083"
```

Environment variables override defaults.

## RabbitMQ Queues

Consumes from:
- `customer.data.queue`
- `inventory.data.queue`

## Running Locally

### Prerequisites

- Python 3.11
- RabbitMQ running on localhost:5672
- Redis running on localhost:6379
- Analytics service running on localhost:8083

### Install Dependencies

```bash
cd consumer-service
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### Start the Service

```bash
# Development
uvicorn app.main:app --host 0.0.0.0 --port 8084 --reload

# Production
uvicorn app.main:app --host 0.0.0.0 --port 8084
```

### With Docker

```bash
# Build image
docker build -t consumer-service .

# Run container
docker run -p 8084:8084 \
  -e RABBITMQ_URL=amqp://admin:admin123@host.docker.internal:5672/ \
  -e REDIS_URL=redis://host.docker.internal:6379 \
  consumer-service
```

## API Documentation

Swagger UI: http://localhost:8084/docs

ReDoc: http://localhost:8084/redoc

OpenAPI JSON: http://localhost:8084/openapi.json

## Data Aggregation Flow

1. Customer consumer receives customer data, stores in Redis
2. Product consumer receives product data, stores in Redis
3. When both are available, aggregator merges data
4. Data is transformed using JSONata schema
5. Merged data is sent to analytics service
6. Redis keys are cleared after successful send

## JSONata Transformation Example

We use JSONata to dynamically transform JSON structures. Here's how it works in `app/mappings/analytics_schema.py`:

**Input data:**
```json
{
  "customer": {
    "customer_id": "C001",
    "first_name": "John",
    "last_name": "Doe",
    "email": "john@example.com"
  },
  "products": [
    { "product_id": "P1", "name": "Widget", "price": 29.99 },
    { "product_id": "P2", "name": "Gadget", "price": 49.99 }
  ]
}
```

**JSONata schema:**
```jsonata
{
  "customer": {
    "id": customer.customer_id,
    "name": customer.first_name & " " & customer.last_name
  },
  "products": products.{ "id": product_id, "name": name },
  "summary": {
    "total_products": $count(products),
    "total_value": $sum(products.price)
  }
}
```

**Output:**
```json
{
  "customer": { "id": "C001", "name": "John Doe" },
  "products": [
    { "id": "P1", "name": "Widget" },
    { "id": "P2", "name": "Gadget" }
  ],
  "summary": { "total_products": 2, "total_value": 79.98 }
}
```

Key JSONata features used:
- **Path navigation**: `customer.first_name` accesses nested fields
- **String concatenation**: `&` operator joins strings
- **Array mapping**: `products.{ ... }` transforms each item
- **Aggregations**: `$count()`, `$sum()` for calculations
