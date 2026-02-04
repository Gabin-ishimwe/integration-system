"""
Base consumer class for RabbitMQ message handling.
"""

import json
from abc import ABC, abstractmethod
from aio_pika import connect_robust, IncomingMessage
from app.config.settings import settings
import structlog

logger = structlog.get_logger()


class BaseConsumer(ABC):
    def __init__(self):
        self.connection = None
        self.channel = None

    async def connect(self):
        self.connection = await connect_robust(
            host=settings.rabbitmq_host,
            port=settings.rabbitmq_port,
            login=settings.rabbitmq_user,
            password=settings.rabbitmq_password,
        )
        self.channel = await self.connection.channel()
        await self.channel.set_qos(prefetch_count=10)
        logger.info("Connected to RabbitMQ", consumer=self.__class__.__name__)

    @abstractmethod
    def get_queue_name(self) -> str:
        """Return the queue name to consume from."""
        pass

    @abstractmethod
    async def process_message(self, data: list):
        """Process the extracted message data."""
        pass

    async def start_consuming(self):
        queue = await self.channel.declare_queue(
            self.get_queue_name(), durable=True
        )
        await queue.consume(self._on_message)
        logger.info("Consuming from queue", queue=self.get_queue_name())

    async def _on_message(self, message: IncomingMessage):
        async with message.process():
            data = json.loads(message.body.decode())
            items = data.get("data", data)
            if not isinstance(items, list):
                items = [items]
            logger.info(
                "Received message",
                consumer=self.__class__.__name__,
                item_count=len(items),
            )
            await self.process_message(items)

    async def close(self):
        if self.connection:
            await self.connection.close()
