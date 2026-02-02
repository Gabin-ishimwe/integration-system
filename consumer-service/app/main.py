import asyncio
from fastapi import FastAPI
from contextlib import asynccontextmanager

from app.config.settings import settings
from app.api import health, queue, trigger
from app.core.analytics_client import AnalyticsClient
from app.core.data_merger import DataMerger
from app.core.rabbitmq_consumer import RabbitMQConsumer
import structlog

structlog.configure(
    processors=[
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.dev.ConsoleRenderer()
    ]
)

logger = structlog.get_logger()

# Global instances
analytics_client = AnalyticsClient()
data_merger = DataMerger(analytics_client)
rabbitmq_consumer = RabbitMQConsumer(data_merger)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("Starting consumer service", app_name=settings.app_name)

    # Set instances for trigger endpoints
    trigger.set_instances(data_merger, analytics_client)

    try:
        await rabbitmq_consumer.connect()
        asyncio.create_task(rabbitmq_consumer.start_consuming())
        logger.info("Consumer service started successfully")
    except Exception as e:
        logger.error("Failed to start consumer", error=str(e))

    yield

    # Shutdown
    logger.info("Shutting down consumer service")
    await rabbitmq_consumer.close()


app = FastAPI(
    title=settings.app_name,
    description="Consumer service for processing integration messages",
    version="1.0.0",
    lifespan=lifespan
)

app.include_router(health.router, tags=["Health"])
app.include_router(queue.router, prefix="/api/v1", tags=["Queue"])
app.include_router(trigger.router, prefix="/api/v1", tags=["Trigger"])


@app.get("/")
async def root():
    return {"message": f"Welcome to {settings.app_name}", "version": "1.0.0"}
