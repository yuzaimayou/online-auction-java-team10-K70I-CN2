import sqlite3

import sqlite_vec
from dotenv import load_dotenv
from fastapi import FastAPI

# Load biến môi trường từ file .env
load_dotenv()

app = FastAPI(
    title="Auction AI Microservice",
    description="Dịch vụ AI xử lý ảnh và gợi ý sản phẩm"
)


@app.get("/")
def health_check():
    try:
        # Khởi tạo một kết nối SQLite tạm trên RAM để test
        db = sqlite3.connect(':memory:')

        # Cho phép tải extension và load sqlite-vec
        db.enable_load_extension(True)
        sqlite_vec.load(db)
        db.enable_load_extension(False)

        # Query thử version của sqlite-vec để xác nhận đã load thành công
        vec_version = db.execute("select vec_version()").fetchone()[0]

        return {
            "status": "success",
            "message": "AI Microservice đang chạy ngon lành!",
            "sqlite_vec_version": vec_version
        }
    except Exception as e:
        return {
            "status": "error",
            "message": str(e)
        }
