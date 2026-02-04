"""
Product consumer - handles inventory.data.queue messages.
"""

from app.config.settings import settings
from .base import BaseConsumer
import structlog

logger = structlog.get_logger()


class ProductConsumer(BaseConsumer):
    def __init__(self, aggregator):
        super().__init__()
        self.aggregator = aggregator

    def get_queue_name(self) -> str:
        return settings.rabbitmq_inventory_queue

    async def process_message(self, products: list):
        logger.info(
            "Processing products",
            product_count=len(products),
        )
        await self.aggregator.add_products(products)
