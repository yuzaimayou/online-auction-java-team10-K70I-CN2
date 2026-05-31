# app/api/endpoints/products.py
import os
import shutil
import struct
from typing import List

from fastapi import APIRouter, UploadFile, File, Form, Query

from app.core.database import get_db_connection
from app.services.vision_service import encode_product_image, encode_product_text

router = APIRouter()


def serialize_f32(vector: list[float]) -> bytes:
    """Ép kiểu mảng float thành bytes nhị phân cho sqlite-vec"""
    return struct.pack('%sf' % len(vector), *vector)


@router.post("/index-product/{item_id}")
async def index_product(
        item_id: str,
        name: str = Form(...),
        description: str = Form(""),
        files: List[UploadFile] = File(...)
):
    db = get_db_connection()
    try:
        # Xóa các vector cũ của sản phẩm này (nếu đang update)
        db.execute("DELETE FROM items_info WHERE item_id = ?", [item_id])
        # Các vector tương ứng trong vec_items sẽ thành dữ liệu rác,
        # nhưng không sao vì ta JOIN bằng items_info.id

        # 1. Xử lý và lưu Vector Hình ảnh
        for i, file in enumerate(files):
            temp_path = f"temp/temp_{file.filename}"
            with open(temp_path, "wb") as buffer:
                shutil.copyfileobj(file.file, buffer)

            # Lưu bảng Info lấy ID số nguyên
            cursor = db.execute(
                "INSERT INTO items_info(item_id, type) VALUES (?, ?)",
                [item_id, f"image_{i}"]
            )
            auto_id = cursor.lastrowid

            # Nhúng và lưu bảng Vector
            img_emb = encode_product_image(temp_path)
            db.execute("INSERT INTO vec_items(rowid, embedding) VALUES (?, ?)",
                       [auto_id, serialize_f32(img_emb)])

            os.remove(temp_path)

        # 2. Xử lý và lưu Vector Văn bản
        if name:
            full_text = f"Sản phẩm: {name}. Chi tiết: {description}"
            cursor = db.execute(
                "INSERT INTO items_info(item_id, type) VALUES (?, ?)",
                [item_id, "text"]
            )
            auto_id = cursor.lastrowid

            text_emb = encode_product_text(full_text)
            db.execute("INSERT INTO vec_items(rowid, embedding) VALUES (?, ?)",
                       [auto_id, serialize_f32(text_emb)])

        db.commit()
        return {"status": "success", "message": f"Đã lưu sản phẩm {item_id}"}
    except Exception as e:
        return {"status": "error", "message": str(e)}
    finally:
        db.close()


@router.get("/recommend")
async def recommend_products(prompt: str = Query(...), top_k: int = 5):
    # Dùng SigLIP để nhúng câu hỏi (Vector 768 chiều)
    query_vector = encode_product_text(prompt)
    query_bytes = serialize_f32(query_vector)

    db = get_db_connection()
    try:
        # INNER JOIN cấu trúc mới
        rows = db.execute("""
                          SELECT i.item_id, vec_distance_cosine(v.embedding, ?) as distance
                          FROM vec_items v
                                   INNER JOIN items_info i ON v.rowid = i.id
                          ORDER BY distance ASC LIMIT 50
                          """, [query_bytes]).fetchall()

        # Nhóm kết quả: Chỉ lấy điểm cao nhất của từng Sản phẩm
        best_matches = {}
        for row in rows:
            real_id = row[0]
            similarity = 1.0 - row[1]
            if real_id not in best_matches or similarity > best_matches[real_id]:
                best_matches[real_id] = similarity

        # Sắp xếp và lọc Threshold
        sorted_results = sorted(best_matches.items(), key=lambda x: x[1], reverse=True)

        final_results = []
        if sorted_results:
            top_score = sorted_results[0][1]
            for item_id, sim in sorted_results:
                if sim >= (top_score - 0.05):  # Dynamic Threshold
                    final_results.append({"id": item_id, "similarity": sim})
                if len(final_results) >= top_k:
                    break

        return {"recommendations": final_results}
    finally:
        db.close()
