from fastapi import APIRouter, HTTPException
from datetime import datetime

router = APIRouter()

# Global state for queue consumption
queue_state = {
    "paused": False,
    "processed_count": 0,
    "error_count": 0
}

@router.get("/queue/status")
async def get_queue_status():
    return {
        "status": "paused" if queue_state["paused"] else "running",
        "processed_count": queue_state["processed_count"],
        "error_count": queue_state["error_count"],
        "timestamp": datetime.utcnow().isoformat()
    }

@router.post("/queue/pause")
async def pause_queue():
    queue_state["paused"] = True
    return {
        "message": "Queue consumption paused",
        "status": "paused",
        "timestamp": datetime.utcnow().isoformat()
    }

@router.post("/queue/resume")
async def resume_queue():
    queue_state["paused"] = False
    return {
        "message": "Queue consumption resumed",
        "status": "running",
        "timestamp": datetime.utcnow().isoformat()
    }

@router.get("/mock-service/health")
async def check_mock_service_health():
    # TODO: Implement actual health check for mock service
    return {
        "mock_service": "pending",
        "timestamp": datetime.utcnow().isoformat()
    }
