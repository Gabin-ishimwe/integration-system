from fastapi import APIRouter
from datetime import datetime
from pydantic import BaseModel


class HealthResponse(BaseModel):
    status: str
    service: str
    timestamp: str


router = APIRouter(tags=["health"])


@router.get(
    "/health",
    response_model=HealthResponse,
    summary="Health check",
    description="Returns the health status of the consumer service"
)
async def health_check():
    return {
        "status": "healthy",
        "service": "consumer-service",
        "timestamp": datetime.utcnow().isoformat()
    }
