import asyncio
from fastapi import FastAPI
from contextlib import asynccontextmanager

from app.api import health
from app.core.message_aggregator import MessageAggregator
from app.core.rabbitmq_consumer import RabbitMQConsumer
import structlog

structlog.configure(
    processors=[
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.dev.ConsoleRenderer()
    ]
)

logger = structlog.get_logger()

aggregator = MessageAggregator()
consumer = RabbitMQConsumer(aggregator)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting consumer service")
    await aggregator.connect()
    await consumer.connect()
    asyncio.create_task(consumer.start_consuming())
    yield
    await consumer.close()
    await aggregator.close()
    logger.info("Consumer service stopped")


app = FastAPI(
    title="consumer-service",
    version="1.0.0",
    lifespan=lifespan
)

app.include_router(health.router)


@app.get("/")
async def root():
    return {"service": "consumer-service", "status": "running"}
