from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # Server
    server_port: int = 8084

    # Integration Producer
    producer_base_url: str = "http://localhost:8082"

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

    # Analytics Service
    analytics_service_url: str = "http://localhost:8083"
    analytics_username: str = "analytics_user"
    analytics_password: str = "analytics_password"

    class Config:
        env_file = ".env"


settings = Settings()
