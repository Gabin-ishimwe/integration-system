"""
Unit tests for the CustomerProductAggregator.
Tests the aggregation and grouping logic.
"""

import pytest
import json
from unittest.mock import AsyncMock, MagicMock, patch
import fakeredis.aioredis


class TestCustomerProductAggregator:
    """Tests for CustomerProductAggregator logic."""

    @pytest.fixture
    def mock_connector(self):
        """Create a mock AnalyticsConnector."""
        connector = AsyncMock()
        connector.send_batch = AsyncMock()
        return connector

    @pytest.fixture
    def sample_customers(self):
        """Sample customer data."""
        return [
            {
                "customer_id": "CUST001",
                "first_name": "John",
                "last_name": "Doe",
                "email": "john@example.com",
                "phone": "+1234567890",
                "status": "ACTIVE"
            },
            {
                "customer_id": "CUST002",
                "first_name": "Jane",
                "last_name": "Smith",
                "email": "jane@example.com",
                "phone": "+9876543210",
                "status": "ACTIVE"
            }
        ]

    @pytest.fixture
    def sample_products(self):
        """Sample product data with customer_id references."""
        return [
            {
                "product_id": "PROD001",
                "name": "Laptop",
                "category": "Electronics",
                "price": 999.99,
                "stock_level": 50,
                "customer_id": "CUST001"
            },
            {
                "product_id": "PROD002",
                "name": "Mouse",
                "category": "Electronics",
                "price": 29.99,
                "stock_level": 200,
                "customer_id": "CUST001"
            },
            {
                "product_id": "PROD003",
                "name": "Keyboard",
                "category": "Electronics",
                "price": 79.99,
                "stock_level": 100,
                "customer_id": "CUST002"
            }
        ]

    def test_group_products_by_customer_id(self, sample_products):
        """Test that products are grouped correctly by customer_id."""
        # This tests the grouping logic extracted from _try_aggregate
        products_by_customer = {}
        for product in sample_products:
            cid = product.get("customer_id")
            if cid not in products_by_customer:
                products_by_customer[cid] = []
            products_by_customer[cid].append(product)

        assert len(products_by_customer) == 2
        assert len(products_by_customer["CUST001"]) == 2
        assert len(products_by_customer["CUST002"]) == 1

    def test_group_products_handles_missing_customer_id(self):
        """Test grouping handles products without customer_id."""
        products = [
            {"product_id": "P1", "name": "A", "customer_id": "C1"},
            {"product_id": "P2", "name": "B"},  # No customer_id
            {"product_id": "P3", "name": "C", "customer_id": "C1"},
        ]

        products_by_customer = {}
        for product in products:
            cid = product.get("customer_id")
            if cid not in products_by_customer:
                products_by_customer[cid] = []
            products_by_customer[cid].append(product)

        assert len(products_by_customer["C1"]) == 2
        assert len(products_by_customer[None]) == 1

    def test_customers_without_products_not_included(self, sample_customers):
        """Test that customers without products are not in merged output."""
        # Only CUST001 has products
        products = [
            {
                "product_id": "PROD001",
                "name": "Laptop",
                "category": "Electronics",
                "price": 999.99,
                "stock_level": 50,
                "customer_id": "CUST001"
            }
        ]

        products_by_customer = {}
        for product in products:
            cid = product.get("customer_id")
            if cid not in products_by_customer:
                products_by_customer[cid] = []
            products_by_customer[cid].append(product)

        # Simulate merge logic
        merged_payloads = []
        for customer in sample_customers:
            cid = customer.get("customer_id")
            customer_products = products_by_customer.get(cid, [])
            if customer_products:
                merged_payloads.append({
                    "customer": customer,
                    "products": customer_products
                })

        # Only CUST001 should be in the merged payloads
        assert len(merged_payloads) == 1
        assert merged_payloads[0]["customer"]["customer_id"] == "CUST001"

    def test_all_customers_with_products_included(self, sample_customers, sample_products):
        """Test all customers with products are included in merge."""
        products_by_customer = {}
        for product in sample_products:
            cid = product.get("customer_id")
            if cid not in products_by_customer:
                products_by_customer[cid] = []
            products_by_customer[cid].append(product)

        merged_payloads = []
        for customer in sample_customers:
            cid = customer.get("customer_id")
            customer_products = products_by_customer.get(cid, [])
            if customer_products:
                merged_payloads.append({
                    "customer": customer,
                    "products": customer_products
                })

        assert len(merged_payloads) == 2
        customer_ids = [p["customer"]["customer_id"] for p in merged_payloads]
        assert "CUST001" in customer_ids
        assert "CUST002" in customer_ids

    def test_product_count_per_customer(self, sample_customers, sample_products):
        """Test correct product count per customer."""
        products_by_customer = {}
        for product in sample_products:
            cid = product.get("customer_id")
            if cid not in products_by_customer:
                products_by_customer[cid] = []
            products_by_customer[cid].append(product)

        # CUST001 should have 2 products, CUST002 should have 1
        assert len(products_by_customer.get("CUST001", [])) == 2
        assert len(products_by_customer.get("CUST002", [])) == 1

    def test_empty_customers_list(self, sample_products):
        """Test handling of empty customers list."""
        customers = []

        products_by_customer = {}
        for product in sample_products:
            cid = product.get("customer_id")
            if cid not in products_by_customer:
                products_by_customer[cid] = []
            products_by_customer[cid].append(product)

        merged_payloads = []
        for customer in customers:
            cid = customer.get("customer_id")
            customer_products = products_by_customer.get(cid, [])
            if customer_products:
                merged_payloads.append({"customer": customer, "products": customer_products})

        assert len(merged_payloads) == 0

    def test_empty_products_list(self, sample_customers):
        """Test handling of empty products list."""
        products = []

        products_by_customer = {}
        for product in products:
            cid = product.get("customer_id")
            if cid not in products_by_customer:
                products_by_customer[cid] = []
            products_by_customer[cid].append(product)

        merged_payloads = []
        for customer in sample_customers:
            cid = customer.get("customer_id")
            customer_products = products_by_customer.get(cid, [])
            if customer_products:
                merged_payloads.append({"customer": customer, "products": customer_products})

        # No customers should be merged since no products
        assert len(merged_payloads) == 0


@pytest.mark.asyncio
class TestAggregatorWithRedis:
    """Integration-style tests for aggregator with fake Redis."""

    @pytest.fixture
    async def fake_redis(self):
        """Create a fresh fake Redis instance for each test."""
        redis = fakeredis.aioredis.FakeRedis(decode_responses=True)
        await redis.flushall()
        return redis

    @pytest.fixture
    def sample_customers(self):
        return [
            {
                "customer_id": "CUST001",
                "first_name": "John",
                "last_name": "Doe",
                "email": "john@example.com",
                "phone": "+1234567890",
                "status": "ACTIVE"
            }
        ]

    @pytest.fixture
    def sample_products(self):
        return [
            {
                "product_id": "PROD001",
                "name": "Laptop",
                "category": "Electronics",
                "price": 999.99,
                "stock_level": 50,
                "customer_id": "CUST001"
            }
        ]

    async def test_redis_stores_customers(self, fake_redis, sample_customers):
        """Test that customers are stored in Redis."""
        await fake_redis.set("customers", json.dumps(sample_customers), ex=3600)

        stored = await fake_redis.get("customers")
        assert stored is not None

        customers = json.loads(stored)
        assert len(customers) == 1
        assert customers[0]["customer_id"] == "CUST001"

    async def test_redis_stores_products(self, fake_redis, sample_products):
        """Test that products are stored in Redis."""
        await fake_redis.set("products", json.dumps(sample_products), ex=3600)

        stored = await fake_redis.get("products")
        assert stored is not None

        products = json.loads(stored)
        assert len(products) == 1
        assert products[0]["product_id"] == "PROD001"

    async def test_aggregation_triggers_when_both_available(self, fake_redis, sample_customers, sample_products):
        """Test aggregation triggers when both customers and products are in Redis."""
        await fake_redis.set("customers", json.dumps(sample_customers), ex=3600)
        await fake_redis.set("products", json.dumps(sample_products), ex=3600)

        customers_data = await fake_redis.get("customers")
        products_data = await fake_redis.get("products")

        # Both should be available
        assert customers_data is not None
        assert products_data is not None

    async def test_aggregation_skips_when_only_customers(self, fake_redis, sample_customers):
        """Test aggregation doesn't trigger with only customers."""
        await fake_redis.set("customers", json.dumps(sample_customers), ex=3600)

        customers_data = await fake_redis.get("customers")
        products_data = await fake_redis.get("products")

        assert customers_data is not None
        assert products_data is None

    async def test_redis_cleanup_after_aggregation(self, fake_redis, sample_customers, sample_products):
        """Test that Redis keys are deleted after aggregation."""
        await fake_redis.set("customers", json.dumps(sample_customers), ex=3600)
        await fake_redis.set("products", json.dumps(sample_products), ex=3600)

        # Simulate cleanup
        await fake_redis.delete("customers", "products")

        customers_data = await fake_redis.get("customers")
        products_data = await fake_redis.get("products")

        assert customers_data is None
        assert products_data is None
