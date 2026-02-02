import asyncio
import json
from aio_pika import connect_robust, IncomingMessage
from app.config.settings import settings
from app.core.data_merger import DataMerger
import structlog

logger = structlog.get_logger()


class RabbitMQConsumer:
    def __init__(self, data_merger: DataMerger):
        self.data_merger = data_merger
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

        await customer_queue.consume(self._process_customer)
        await inventory_queue.consume(self._process_product)

        logger.info("Started consuming from queues")

    async def _process_customer(self, message: IncomingMessage):
        async with message.process():
            try:
                data = json.loads(message.body.decode())
                customer = data.get("data", data)
                await self.data_merger.add_customer(customer)
                logger.debug("Processed customer", customer_id=customer.get("customer_id"))
            except Exception as e:
                logger.error("Failed to process customer message", error=str(e))

    async def _process_product(self, message: IncomingMessage):
        async with message.process():
            try:
                data = json.loads(message.body.decode())
                product = data.get("data", data)
                await self.data_merger.add_product(product)
                logger.debug("Processed product", product_id=product.get("product_id"))
            except Exception as e:
                logger.error("Failed to process product message", error=str(e))

    async def close(self):
        if self.connection:
            await self.connection.close()
            logger.info("Disconnected from RabbitMQ")
