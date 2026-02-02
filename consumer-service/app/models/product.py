from pydantic import BaseModel, Field
from datetime import datetime
from decimal import Decimal
from typing import Optional


class Product(BaseModel):
    product_id: str = Field(..., alias="product_id")
    name: str
    category: str
    price: Decimal
    stock_level: int = Field(..., alias="stock_level")
    last_updated: datetime = Field(..., alias="last_updated")

    class Config:
        populate_by_name = True


class ProductMessage(BaseModel):
    correlation_id: str = Field(..., alias="correlation_id")
    timestamp: datetime
    source: str
    data: Product

    class Config:
        populate_by_name = True
