from pydantic import BaseModel, Field
from typing import Optional


class AuthRequest(BaseModel):
    username: str
    password: str


class TokenResponse(BaseModel):
    access_token: str = Field(..., alias="access_token")
    token_type: str = Field(..., alias="token_type")
    expires_in: int = Field(..., alias="expires_in")
    scope: Optional[str] = None

    class Config:
        populate_by_name = True
