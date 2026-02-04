"""
Analytics service connector - handles outbound API calls to analytics service.
"""

from uuid import uuid4
import httpx
from app.config.settings import settings
import structlog

logger = structlog.get_logger()


class AnalyticsConnector:
    def __init__(self):
        self.token: str = None

    async def get_token(self) -> str:
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

    async def send_batch(self, merged_data: list):
        """Send merged data to analytics service."""
        try:
            request_body = {
                "batchNumber": uuid4().hex[:8].upper(),
                "data": merged_data,
            }
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"{settings.analytics_service_url}/analytics/api/data",
                    json=request_body
                )
                response.raise_for_status()
                logger.info(
                    "Sent batch to analytics",
                    batch_size=len(merged_data),
                )
        except Exception as e:
            logger.error("Failed to send to analytics", error=str(e))
