"""
RabbitMQ consumer - listens to customer and inventory queues.
"""

import json
from aio_pika import connect_robust, IncomingMessage
from app.config.settings import settings
import structlog

logger = structlog.get_logger()


class RabbitMQConsumer:
    def __init__(self, aggregator):
        self.aggregator = aggregator
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
        logger.info("Connected to RabbitMQ")

    async def start_consuming(self):
        customer_queue = await self.channel.declare_queue(
            settings.rabbitmq_customer_queue, durable=True
        )
        inventory_queue = await self.channel.declare_queue(
            settings.rabbitmq_inventory_queue, durable=True
        )

        await customer_queue.consume(self._on_customers)
        await inventory_queue.consume(self._on_products)
        logger.info("Consuming from queues")

    async def _on_customers(self, message: IncomingMessage):
        async with message.process():
            data = json.loads(message.body.decode())
            customers = data.get("data", data)
            if not isinstance(customers, list):
                customers = [customers]
            logger.info(
                "Received customer message from RabbitMQ",
                customer_count=len(customers),
            )
            await self.aggregator.add_customers(customers)

    async def _on_products(self, message: IncomingMessage):
        async with message.process():
            data = json.loads(message.body.decode())
            products = data.get("data", data)
            if not isinstance(products, list):
                products = [products]
            logger.info(
                "Received product message from RabbitMQ",
                product_count=len(products),
            )
            await self.aggregator.add_products(products)

    async def close(self):
        if self.connection:
            await self.connection.close()
