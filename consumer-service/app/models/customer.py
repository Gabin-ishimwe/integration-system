from pydantic import BaseModel, Field
from datetime import datetime
from enum import Enum
from typing import Optional


class CustomerStatus(str, Enum):
    ACTIVE = "ACTIVE"
    INACTIVE = "INACTIVE"
    SUSPENDED = "SUSPENDED"


class Customer(BaseModel):
    customer_id: str = Field(..., alias="customer_id")
    first_name: str = Field(..., alias="first_name")
    last_name: str = Field(..., alias="last_name")
    email: str
    phone: str
    registration_date: datetime = Field(..., alias="registration_date")
    status: CustomerStatus

    class Config:
        populate_by_name = True


class CustomerMessage(BaseModel):
    correlation_id: str = Field(..., alias="correlation_id")
    timestamp: datetime
    source: str
    data: Customer

    class Config:
        populate_by_name = True
