from pydantic import BaseModel, Field
from datetime import datetime
from decimal import Decimal
from typing import List


class ProductSummary(BaseModel):
    product_id: str = Field(..., alias="product_id")
    product_name: str = Field(..., alias="product_name")
    price: Decimal
    stock_level: int = Field(..., alias="stock_level")

    class Config:
        populate_by_name = True


class MergedData(BaseModel):
    merge_id: str = Field(..., alias="merge_id")
    customer_id: str = Field(..., alias="customer_id")
    customer_name: str = Field(..., alias="customer_name")
    customer_email: str = Field(..., alias="customer_email")
    products: List[ProductSummary]
    merge_timestamp: datetime = Field(..., alias="merge_timestamp")

    class Config:
        populate_by_name = True
