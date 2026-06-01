from contextlib import asynccontextmanager

from fastapi import FastAPI

# 1. BẮT BUỘC IMPORT api_router TỪ FILE router.py MỚI
from app.api.router import api_router
from app.core.database import init_db


@asynccontextmanager
async def lifespan(app: FastAPI):
    print("🚀 Đang khởi động Server...")
    init_db()
    yield
    print("💤 Đang tắt Server...")


app = FastAPI(
    title="Hệ thống AI Đấu giá",
    description="Microservice quản lý Vector Search cho Hình ảnh và Tài liệu",
    version="1.0.0",
    lifespan=lifespan
)

# 2. ĐÂY LÀ DÒNG QUYẾT ĐỊNH: Chỉ cần 1 dòng này, nó sẽ tự động kéo cả Docs và Products vào
app.include_router(api_router)


@app.get("/")
def root():
    return {"message": "AI Server is running smoothly! 🚀"}
