import os
import shutil
import struct
from typing import Annotated

import numpy as np
from fastapi import FastAPI, UploadFile, File

from database import init_vector_db, search_similar_items, get_db_connection
from engine import SigLIPEngine

app = FastAPI()
engine = SigLIPEngine()


@app.on_event("startup")
async def startup_event():
    init_vector_db()


# 1. Hàm tính trung bình cộng các vector
def average_embeddings(embeddings_list):
    # Biến danh sách vector thành ma trận numpy
    matrix = np.array(embeddings_list)
    # Tính trung bình theo cột (axis=0) để ép từ 5 vector (768 chiều) thành 1 vector (768 chiều)
    avg_vector = np.mean(matrix, axis=0)
    return avg_vector


# Hàm chuyển đổi mảng số thành nhị phân siêu tốc
def serialize_f32(vector):
    # Chuyển về numpy array, làm phẳng (flatten) và ép kiểu float32
    flat_vector = np.array(vector).flatten().astype(np.float32)
    # Trả về dữ liệu nhị phân
    return struct.pack(f"{len(flat_vector)}f", *flat_vector)


@app.post("/index-product/{item_id}")
async def index_product(
        item_id: str,
        # CÚ PHÁP CHUẨN FASTAPI: Khai báo rõ là File và thêm description
        files: Annotated[
            list[UploadFile], File(description="Multiple files as UploadFile")
        ]
):
    if (len(files) > 5):
        return {"status": "error", "message": "Chỉ được phép tải lên tối đa 5 ảnh"}

    all_embeddings = []

    for file in files:
        temp_path = f"temp_{file.filename}"
        with open(temp_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        # Tạo vector cho từng ảnh
        emb = engine.encode_image(temp_path)
        all_embeddings.append(emb)

        # Xóa file rác ngay sau khi lấy xong vector
        os.remove(temp_path)

    # NẾU CÓ NHIỀU ẢNH -> Tính trung bình cộng. NẾU CÓ 1 ẢNH -> Giữ nguyên.
    final_embedding = average_embeddings(all_embeddings)

    # Ép kiểu nhị phân để lưu DB (Sử dụng lại hàm serialize_f32 bạn đã viết)
    embedding_bytes = serialize_f32(final_embedding)

    # Lưu 1 vector duy nhất vào Database
    db = get_db_connection()
    db.execute("DELETE FROM vec_items WHERE item_id = ?", [item_id])
    db.execute("INSERT OR REPLACE INTO vec_items(item_id, embedding) VALUES (?, ?)",
               [item_id, embedding_bytes])
    db.commit()
    db.close()

    return {
        "status": "indexed",
        "item_id": item_id,
        "images_processed": len(files)
    }


@app.get("/recommend")
async def recommend(prompt: str):
    # Tìm kiếm sản phẩm dựa trên mô tả văn bản của người dùng
    query_vec = engine.encode_text(prompt)
    results = search_similar_items(query_vec)

    formatted_results = [
        {"id": r[0], "name": r[1], "price": r[2], "similarity": 1 - r[3]}
        for r in results
    ]
    return {"recommendations": formatted_results}
