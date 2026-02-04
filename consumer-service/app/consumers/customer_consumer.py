"""
Customer consumer - handles customer.data.queue messages.
"""

from app.config.settings import settings
from .base import BaseConsumer
import structlog

logger = structlog.get_logger()


class CustomerConsumer(BaseConsumer):
    def __init__(self, aggregator):
        super().__init__()
        self.aggregator = aggregator

    def get_queue_name(self) -> str:
        return settings.rabbitmq_customer_queue

    async def process_message(self, customers: list):
        logger.info(
            "Processing customers",
            customer_count=len(customers),
        )
        await self.aggregator.add_customers(customers)
