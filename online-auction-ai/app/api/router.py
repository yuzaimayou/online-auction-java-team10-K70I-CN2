# app/api/router.py
from fastapi import APIRouter

from app.api.endpoints import docs, products

api_router = APIRouter()

# Nhúng router của Docs
api_router.include_router(
    docs.router,
    prefix="/docs",
    tags=["Knowledge Base (RAG)"]
)

# Nhúng router của Sản phẩm
api_router.include_router(
    products.router,
    prefix="/products",
    tags=["Product Search"]
)
