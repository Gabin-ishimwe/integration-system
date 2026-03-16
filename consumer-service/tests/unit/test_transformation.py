"""
Unit tests for the JSONata transformation logic.
Tests the transform_customer_products function from analytics_schema.py
"""

import pytest
from app.mappings.analytics_schema import transform_customer_products


class TestTransformCustomerProducts:
    """Tests for customer-product transformation."""

    def test_transform_single_customer_with_products(self):
        """Test basic transformation with one customer and multiple products."""
        customer = {
            "customer_id": "CUST001",
            "first_name": "John",
            "last_name": "Doe",
            "email": "john.doe@example.com",
            "phone": "+1234567890",
            "status": "ACTIVE"
        }
        products = [
            {
                "product_id": "PROD001",
                "name": "Laptop",
                "category": "Electronics",
                "price": 999.99,
                "stock_level": 50
            },
            {
                "product_id": "PROD002",
                "name": "Mouse",
                "category": "Electronics",
                "price": 29.99,
                "stock_level": 200
            }
        ]

        result = transform_customer_products(customer, products)

        # Verify customer data
        assert result["customer"]["id"] == "CUST001"
        assert result["customer"]["name"] == "John Doe"
        assert result["customer"]["email"] == "john.doe@example.com"
        assert result["customer"]["phone"] == "+1234567890"
        assert result["customer"]["status"] == "ACTIVE"

        # Verify products
        assert len(result["products"]) == 2
        assert result["products"][0]["id"] == "PROD001"
        assert result["products"][0]["name"] == "Laptop"
        assert result["products"][1]["id"] == "PROD002"

        # Verify summary
        assert result["summary"]["total_products"] == 2
        assert result["summary"]["total_value"] == pytest.approx(1029.98, rel=1e-2)

        # Verify metadata
        assert result["merge_id"].startswith("MERGE_")
        assert "timestamp" in result

    def test_transform_customer_with_single_product(self):
        """Test transformation with one product."""
        customer = {
            "customer_id": "CUST002",
            "first_name": "Jane",
            "last_name": "Smith",
            "email": "jane@example.com",
            "phone": "+9876543210",
            "status": "ACTIVE"
        }
        products = [
            {
                "product_id": "PROD003",
                "name": "Keyboard",
                "category": "Electronics",
                "price": 79.99,
                "stock_level": 100
            }
        ]

        result = transform_customer_products(customer, products)

        assert result["customer"]["name"] == "Jane Smith"
        assert len(result["products"]) == 1
        assert result["summary"]["total_products"] == 1
        assert result["summary"]["total_value"] == pytest.approx(79.99, rel=1e-2)

    def test_transform_preserves_product_details(self):
        """Test that all product fields are preserved correctly."""
        customer = {
            "customer_id": "CUST003",
            "first_name": "Bob",
            "last_name": "Wilson",
            "email": "bob@example.com",
            "phone": "+1111111111",
            "status": "INACTIVE"
        }
        products = [
            {
                "product_id": "PROD004",
                "name": "Monitor",
                "category": "Electronics",
                "price": 299.99,
                "stock_level": 25
            }
        ]

        result = transform_customer_products(customer, products)

        product = result["products"][0]
        assert product["id"] == "PROD004"
        assert product["name"] == "Monitor"
        assert product["category"] == "Electronics"
        assert product["price"] == 299.99
        assert product["stock_level"] == 25

    def test_transform_generates_unique_merge_ids(self):
        """Test that each transformation generates a unique merge_id."""
        customer = {
            "customer_id": "CUST001",
            "first_name": "John",
            "last_name": "Doe",
            "email": "john@example.com",
            "phone": "+1234567890",
            "status": "ACTIVE"
        }
        products = [{"product_id": "P1", "name": "X", "category": "Y", "price": 10, "stock_level": 1}]

        result1 = transform_customer_products(customer, products)
        result2 = transform_customer_products(customer, products)

        assert result1["merge_id"] != result2["merge_id"]

    def test_transform_with_zero_price_products(self):
        """Test transformation handles zero-price products."""
        customer = {
            "customer_id": "CUST004",
            "first_name": "Free",
            "last_name": "User",
            "email": "free@example.com",
            "phone": "+0000000000",
            "status": "ACTIVE"
        }
        products = [
            {"product_id": "P1", "name": "Free Item", "category": "Promo", "price": 0, "stock_level": 999}
        ]

        result = transform_customer_products(customer, products)

        assert result["summary"]["total_value"] == 0
        assert result["summary"]["total_products"] == 1

    def test_transform_calculates_total_value_correctly(self):
        """Test that total_value sums all product prices."""
        customer = {
            "customer_id": "CUST005",
            "first_name": "Test",
            "last_name": "User",
            "email": "test@example.com",
            "phone": "+5555555555",
            "status": "ACTIVE"
        }
        products = [
            {"product_id": "P1", "name": "A", "category": "X", "price": 100.00, "stock_level": 1},
            {"product_id": "P2", "name": "B", "category": "X", "price": 200.00, "stock_level": 1},
            {"product_id": "P3", "name": "C", "category": "X", "price": 300.00, "stock_level": 1},
        ]

        result = transform_customer_products(customer, products)

        assert result["summary"]["total_products"] == 3
        assert result["summary"]["total_value"] == pytest.approx(600.00, rel=1e-2)
