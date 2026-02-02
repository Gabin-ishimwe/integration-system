from pydantic import BaseModel, Field
from datetime import datetime
from typing import Generic, TypeVar, List, Optional

T = TypeVar('T')


class PagedResponse(BaseModel, Generic[T]):
    content: List[T]
    page: int
    size: int
    total_elements: int = Field(..., alias="total_elements")
    total_pages: int = Field(..., alias="total_pages")
    has_next: bool = Field(..., alias="has_next")
    has_previous: bool = Field(..., alias="has_previous")

    class Config:
        populate_by_name = True


class ApiError(BaseModel):
    timestamp: datetime
    status: int
    error: str
    message: str
    path: str


class AnalyticsResponse(BaseModel):
    status: str
    message: str
    submission_id: Optional[str] = Field(None, alias="submission_id")
    timestamp: datetime

    class Config:
        populate_by_name = True
