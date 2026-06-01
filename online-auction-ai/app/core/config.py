from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    PROJECT_NAME: str = "Auction System AI"
    # Đường dẫn DB được đẩy ra thư mục data bên ngoài package app
    DATABASE_PATH: str = "data/vector_store.db"

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")


# Tạo một biến toàn cục để các file khác import
settings = Settings()
