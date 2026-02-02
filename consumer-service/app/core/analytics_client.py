import httpx
from app.config.settings import settings
import structlog

logger = structlog.get_logger()


class AnalyticsClient:
    def __init__(self):
        self.base_url = settings.mock_service_url
        self.token = None

    async def _get_token(self) -> str:
        if self.token:
            return self.token

        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.base_url}/auth/token",
                json={
                    "username": settings.analytics_username,
                    "password": settings.analytics_password,
                },
            )
            response.raise_for_status()
            data = response.json()
            self.token = data["access_token"]
            return self.token

    async def send_merged_data(self, merged_data: dict) -> bool:
        try:
            token = await self._get_token()

            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"{self.base_url}/analytics/api/data",
                    json=merged_data,
                    headers={"Authorization": f"Bearer {token}"},
                )
                response.raise_for_status()

                logger.info(
                    "Sent merged data to analytics",
                    merge_id=merged_data.get("merge_id"),
                    status=response.status_code,
                )
                return True

        except Exception as e:
            logger.error("Failed to send to analytics", error=str(e))
            return False
