"""
Customer-Product aggregator - stores data in Redis,
merges when both are available, and sends to analytics.
"""

import json
import redis.asyncio as redis
from app.config.settings import settings
from app.mappings import transform_customer_products
from app.connectors import AnalyticsConnector
import structlog

logger = structlog.get_logger()


class CustomerProductAggregator:
    def __init__(self, connector: AnalyticsConnector):
        self.redis: redis.Redis = None
        self.connector = connector

    async def connect(self):
        self.redis = redis.Redis(
            host=settings.redis_host,
            port=settings.redis_port,
            decode_responses=True
        )
        await self.redis.ping()
        logger.info("Connected to Redis")

    async def close(self):
        if self.redis:
            await self.redis.close()

    async def add_customers(self, customers: list):
        logger.info(
            "Adding customers to Redis",
            customer_count=len(customers),
        )
        await self.redis.set("customers", json.dumps(customers), ex=3600)
        await self._try_aggregate()

    async def add_products(self, products: list):
        logger.info(
            "Adding products to Redis",
            product_count=len(products),
        )
        await self.redis.set("products", json.dumps(products), ex=3600)
        await self._try_aggregate()

    async def _try_aggregate(self):
        customers_data = await self.redis.get("customers")
        products_data = await self.redis.get("products")

        if not customers_data or not products_data:
            return

        customers = json.loads(customers_data)
        products = json.loads(products_data)
        logger.info(
            "Starting aggregation",
            customer_count=len(customers),
            product_count=len(products),
        )

        # Group products by customer_id
        products_by_customer = {}
        for product in products:
            cid = product.get("customer_id")
            if cid not in products_by_customer:
                products_by_customer[cid] = []
            products_by_customer[cid].append(product)

        # Transform and collect each customer with their products
        merged_payloads = []
        for customer in customers:
            cid = customer.get("customer_id")
            customer_products = products_by_customer.get(cid, [])
            if customer_products:
                logger.info(
                    "Merging customer with products",
                    customer_id=cid,
                    product_count=len(customer_products),
                )
                merged = transform_customer_products(customer, customer_products)
                logger.debug(
                    "Merged payload ready for analytics",
                    customer_id=cid,
                    merge_id=merged.get("merge_id"),
                )
                merged_payloads.append(merged)

        if merged_payloads:
            await self.connector.send_batch(merged_payloads)

        await self.redis.delete("customers", "products")
        logger.info("Aggregation complete")
