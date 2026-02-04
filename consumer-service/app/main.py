import asyncio
from fastapi import FastAPI
from contextlib import asynccontextmanager

from app.api import health, trigger
from app.connectors import AnalyticsConnector
from app.aggregators import CustomerProductAggregator
from app.consumers import CustomerConsumer, ProductConsumer
import structlog

structlog.configure(
    processors=[
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.dev.ConsoleRenderer()
    ]
)

logger = structlog.get_logger()

# Initialize components
connector = AnalyticsConnector()
aggregator = CustomerProductAggregator(connector)
customer_consumer = CustomerConsumer(aggregator)
product_consumer = ProductConsumer(aggregator)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting consumer service")

    # Connect aggregator (Redis)
    await aggregator.connect()

    # Connect consumers (RabbitMQ) - share connection
    await customer_consumer.connect()
    await product_consumer.connect()

    # Start consuming
    asyncio.create_task(customer_consumer.start_consuming())
    asyncio.create_task(product_consumer.start_consuming())

    yield

    # Cleanup
    await customer_consumer.close()
    await product_consumer.close()
    await aggregator.close()
    logger.info("Consumer service stopped")


app = FastAPI(
    title="consumer-service",
    version="1.0.0",
    lifespan=lifespan
)

app.include_router(health.router)
app.include_router(trigger.router)


@app.get("/")
async def root():
    return {"service": "consumer-service", "status": "running"}
