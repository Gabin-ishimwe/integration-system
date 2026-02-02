from fastapi import APIRouter
from datetime import datetime

router = APIRouter()

# Reference to global instances (set from main.py)
_data_merger = None
_analytics_client = None


def set_instances(data_merger, analytics_client):
    global _data_merger, _analytics_client
    _data_merger = data_merger
    _analytics_client = analytics_client


@router.post("/trigger/merge")
async def trigger_merge():
    """Manually trigger data merge and send to analytics"""
    if _data_merger is None:
        return {"status": "error", "message": "Data merger not initialized"}

    customers_count = len(_data_merger.customers)
    products_count = len(_data_merger.products)

    # Force merge if we have any data
    if customers_count > 0 and products_count > 0:
        await _data_merger._merge_and_send()
        return {
            "status": "completed",
            "message": "Merge triggered",
            "customers_processed": 1,
            "products_processed": min(products_count, _data_merger.merge_threshold),
            "timestamp": datetime.utcnow().isoformat()
        }

    return {
        "status": "skipped",
        "message": "Not enough data to merge",
        "customers_buffered": customers_count,
        "products_buffered": products_count,
        "timestamp": datetime.utcnow().isoformat()
    }


@router.get("/trigger/status")
async def get_buffer_status():
    """Get current buffer status"""
    if _data_merger is None:
        return {"status": "error", "message": "Data merger not initialized"}

    return {
        "customers_buffered": len(_data_merger.customers),
        "products_buffered": len(_data_merger.products),
        "merge_threshold": _data_merger.merge_threshold,
        "ready_to_merge": len(_data_merger.customers) > 0 and len(_data_merger.products) >= _data_merger.merge_threshold,
        "timestamp": datetime.utcnow().isoformat()
    }


@router.post("/trigger/clear")
async def clear_buffers():
    """Clear all buffered data"""
    if _data_merger is None:
        return {"status": "error", "message": "Data merger not initialized"}

    _data_merger.customers.clear()
    _data_merger.products.clear()

    return {
        "status": "cleared",
        "message": "All buffers cleared",
        "timestamp": datetime.utcnow().isoformat()
    }
