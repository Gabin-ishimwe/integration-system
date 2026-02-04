"""
Trigger endpoints - calls integration-producer callbacks to fetch data.
"""

from fastapi import APIRouter
import httpx
from app.config.settings import settings
import structlog

logger = structlog.get_logger()

router = APIRouter(prefix="/api/trigger", tags=["trigger"])


@router.post("/fetch-all")
async def trigger_fetch_all():
    """Trigger fetch of all data from integration-producer."""
    logger.info("Triggering fetch-all from integration-producer")

    async with httpx.AsyncClient() as client:
        response = await client.post(f"{settings.producer_base_url}/api/callback/fetch-all")
        return response.json()


@router.post("/fetch-customers")
async def trigger_fetch_customers():
    """Trigger fetch of customers from integration-producer."""
    logger.info("Triggering fetch-customers from integration-producer")

    async with httpx.AsyncClient() as client:
        response = await client.post(f"{settings.producer_base_url}/api/callback/fetch-customers")
        return response.json()


@router.post("/fetch-products")
async def trigger_fetch_products():
    """Trigger fetch of products from integration-producer."""
    logger.info("Triggering fetch-products from integration-producer")

    async with httpx.AsyncClient() as client:
        response = await client.post(f"{settings.producer_base_url}/api/callback/fetch-products")
        return response.json()
