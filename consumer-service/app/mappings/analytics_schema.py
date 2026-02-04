"""
Analytics data transformation schema using JSONata.
"""

from datetime import datetime
from uuid import uuid4
import jsonata

ANALYTIC_SCHEMA = """
{
    "merge_id": merge_id,
    "customer": {
        "id": customer.customer_id,
        "name": customer.first_name & " " & customer.last_name,
        "email": customer.email,
        "phone": customer.phone,
        "status": customer.status
    },
    "products": $append([], products.{
        "id": product_id,
        "name": name,
        "category": category,
        "price": price,
        "stock_level": stock_level
    }),
    "summary": {
        "total_products": $count(products),
        "total_value": $sum(products.price)
    },
    "timestamp": timestamp
}
"""


def transform_customer_products(customer: dict, products: list) -> dict:
    """Transform customer and products using JSONata analytic_schema."""
    expr = jsonata.Jsonata(ANALYTIC_SCHEMA)
    data = {
        "customer": customer,
        "products": products,
        "merge_id": f"MERGE_{uuid4().hex[:8].upper()}",
        "timestamp": datetime.utcnow().isoformat() + "Z"
    }
    return expr.evaluate(data)
