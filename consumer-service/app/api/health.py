from fastapi import APIRouter
from datetime import datetime

from app.config.settings import settings

router = APIRouter()

@router.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "service": settings.app_name,
        "timestamp": datetime.utcnow().isoformat()
    }

@router.get("/health/ready")
async def readiness_check():
    # TODO: Add actual readiness checks (RabbitMQ, Redis, Mock Service)
    return {
        "status": "ready",
        "checks": {
            "rabbitmq": "pending",
            "redis": "pending",
            "mock_service": "pending"
        },
        "timestamp": datetime.utcnow().isoformat()
    }

@router.get("/health/live")
async def liveness_check():
    return {
        "status": "alive",
        "timestamp": datetime.utcnow().isoformat()
    }
