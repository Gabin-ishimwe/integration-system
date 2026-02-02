from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    app_name: str = "consumer-service"
    debug: bool = False

    # RabbitMQ
    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    rabbitmq_user: str = "admin"
    rabbitmq_password: str = "admin123"
    rabbitmq_customer_queue: str = "customer.data.queue"
    rabbitmq_inventory_queue: str = "inventory.data.queue"

    # Redis
    redis_host: str = "localhost"
    redis_port: int = 6379

    # Mock Service (WireMock)
    mock_service_url: str = "http://localhost:8081"
    analytics_username: str = "analytics_user"
    analytics_password: str = "analytics_password"

    # Server
    host: str = "0.0.0.0"
    port: int = 8083

    class Config:
        env_file = ".env"


@lru_cache()
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
