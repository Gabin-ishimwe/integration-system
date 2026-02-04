"""
Message aggregator - stores customer and product data in Redis,
merges with JSONata when both are available, and sends to analytics.
"""

import json
from datetime import datetime
from uuid import uuid4
import redis.asyncio as redis
import jsonata
import httpx
from app.config.settings import settings
import structlog

logger = structlog.get_logger()

ANALYTIC_SCHEMA = """
{
    "merge_id": $merge_id,
    "customer": {
        "id": customer.customer_id,
        "name": customer.first_name & " " & customer.last_name,
        "email": customer.email,
        "phone": customer.phone,
        "status": customer.status
    },
    "products": products.{
        "id": product_id,
        "name": name,
        "category": category,
        "price": price,
        "stock_level": stock_level
    },
    "summary": {
        "total_products": $count(products),
        "total_value": $sum(products.price)
    },
    "timestamp": $timestamp
}
"""


class MessageAggregator:
    def __init__(self):
        self.redis: redis.Redis = None
        self.token: str = None

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

        # Transform and collect each customer with their products for batch send
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
                merged = self._transform(customer, customer_products)
                logger.debug(
                    "Merged payload ready for analytics",
                    customer_id=cid,
                    merge_id=merged.get("merge_id"),
                )
                merged_payloads.append(merged)
        if merged_payloads:
            await self._send_to_analytics(merged_payloads)

        await self.redis.delete("customers", "products")
        logger.info("Aggregation complete")

    def _transform(self, customer: dict, products: list) -> dict:
        """Transform customer and products using JSONata analytic_schema."""
        expr = jsonata.Jsonata(ANALYTIC_SCHEMA)
        expr.register_binding("merge_id", f"MERGE_{uuid4().hex[:8].upper()}")
        expr.register_binding("timestamp", datetime.utcnow().isoformat() + "Z")
        return expr.evaluate({"customer": customer, "products": products})

    async def _get_token(self) -> str:
        if self.token:
            return self.token

        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{settings.analytics_service_url}/auth/token",
                json={
                    "username": settings.analytics_username,
                    "password": settings.analytics_password,
                },
            )
            response.raise_for_status()
            self.token = response.json()["access_token"]
            return self.token

    async def _send_to_analytics(self, merged_data: dict):
        """Send merged data to analytics service."""
        try:
            token = await self._get_token()
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"{settings.analytics_service_url}/analytics/api/data",
                    json=merged_data,
                    headers={"Authorization": f"Bearer {token}"},
                )
                response.raise_for_status()
                logger.info(
                    "Sent batch to analytics",
                    batch_size=1,
                    merge_id=merged_data.get("merge_id"),
                )
        except Exception as e:
            logger.error("Failed to send to analytics", error=str(e))
