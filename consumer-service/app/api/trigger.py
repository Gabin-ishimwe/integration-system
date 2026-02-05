"""
Trigger endpoints - calls integration-producer callbacks to fetch data.
"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
import httpx
from app.config.settings import settings
import structlog

logger = structlog.get_logger()


class TriggerResponse(BaseModel):
    status: str
    customers_published: Optional[int] = None
    products_published: Optional[int] = None
    timestamp: str


router = APIRouter(prefix="/api/trigger", tags=["trigger"])


@router.post(
    "/fetch-all",
    response_model=TriggerResponse,
    summary="Trigger fetch all",
    description="Triggers the integration-producer to fetch both customers and products from external systems"
)
async def trigger_fetch_all():
    logger.info("Triggering fetch-all from integration-producer")

    async with httpx.AsyncClient() as client:
        response = await client.post(f"{settings.producer_base_url}/api/callback/fetch-all")
        return response.json()


@router.post(
    "/fetch-customers",
    response_model=TriggerResponse,
    summary="Trigger fetch customers",
    description="Triggers the integration-producer to fetch customers from the CRM system"
)
async def trigger_fetch_customers():
    logger.info("Triggering fetch-customers from integration-producer")

    async with httpx.AsyncClient() as client:
        response = await client.post(f"{settings.producer_base_url}/api/callback/fetch-customers")
        return response.json()


@router.post(
    "/fetch-products",
    response_model=TriggerResponse,
    summary="Trigger fetch products",
    description="Triggers the integration-producer to fetch products from the Inventory system"
)
async def trigger_fetch_products():
    logger.info("Triggering fetch-products from integration-producer")

    async with httpx.AsyncClient() as client:
        response = await client.post(f"{settings.producer_base_url}/api/callback/fetch-products")
        return response.json()
