import asyncio
from datetime import datetime
from typing import Dict, List
from uuid import uuid4
from app.core.analytics_client import AnalyticsClient
import structlog

logger = structlog.get_logger()


class DataMerger:
    def __init__(self, analytics_client: AnalyticsClient):
        self.analytics_client = analytics_client
        self.customers: Dict[str, dict] = {}
        self.products: List[dict] = []
        self.merge_threshold = 5  # Merge when we have this many products

    async def add_customer(self, customer: dict):
        customer_id = customer.get("customer_id")
        self.customers[customer_id] = customer
        logger.debug("Added customer to buffer", customer_id=customer_id)
        await self._try_merge()

    async def add_product(self, product: dict):
        self.products.append(product)
        logger.debug("Added product to buffer", product_id=product.get("product_id"))
        await self._try_merge()

    async def _try_merge(self):
        if len(self.customers) > 0 and len(self.products) >= self.merge_threshold:
            await self._merge_and_send()

    async def _merge_and_send(self):
        if not self.customers or not self.products:
            return

        # Take first customer and current products
        customer_id, customer = next(iter(self.customers.items()))

        merged_data = {
            "merge_id": f"MERGE_{uuid4().hex[:8].upper()}",
            "customer_id": customer_id,
            "customer_name": f"{customer.get('first_name', '')} {customer.get('last_name', '')}",
            "customer_email": customer.get("email", ""),
            "products": [
                {
                    "product_id": p.get("product_id"),
                    "product_name": p.get("name"),
                    "price": p.get("price"),
                    "stock_level": p.get("stock_level"),
                }
                for p in self.products[:self.merge_threshold]
            ],
            "merge_timestamp": datetime.utcnow().isoformat() + "Z",
        }

        # Send to analytics
        success = await self.analytics_client.send_merged_data(merged_data)

        if success:
            # Clear processed data
            del self.customers[customer_id]
            self.products = self.products[self.merge_threshold:]
            logger.info("Merged and sent data", merge_id=merged_data["merge_id"])
        else:
            logger.error("Failed to send merged data")
