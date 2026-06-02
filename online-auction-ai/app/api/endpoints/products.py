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
        db.execute("""
                   INSERT OR
                   REPLACE
                   INTO items_info(item_id, name, description)
                   VALUES (?, ?, ?)
                   """, [item_id, name, description])

        # 1. Xử lý và lưu Vector Hình ảnh
        os.makedirs('temp', exist_ok=True)
        for i, file in enumerate(files):
            temp_path = f"temp/temp_{file.filename}"
            with open(temp_path, "wb") as buffer:
                shutil.copyfileobj(file.file, buffer)

            # Nhúng và lưu bảng Vector
            img_emb = encode_product_image(temp_path)
            db.execute("""INSERT INTO vec_items(item_id, field_type, image_index, embedding)
                          VALUES (?, ?, ?, ?)""",
                       [item_id, "image", i, serialize_f32(img_emb)])

            os.remove(temp_path)

        # 2. Xử lý và lưu Vector Văn bản
        name_emb = encode_product_text(name)
        desc_emb = encode_product_text(description)
        db.execute("""
                   INSERT INTO vec_items(item_id, field_type, image_index, embedding, content)
                   VALUES (?, ?, ?, ?, ?)
                   """, [item_id, "name", -1, serialize_f32(name_emb), name])

        db.execute("""
                   INSERT INTO vec_items(item_id, field_type, image_index, embedding, content)
                   VALUES (?, ?, ?, ?, ?)
                   """, [item_id, "description", -1, serialize_f32(desc_emb), description])
        db.commit()
        return {"status": "success", "message": f"Đã lưu sản phẩm {item_id}"}
    except Exception as e:
        return {"status": "error", "message": str(e)}
    finally:
        db.close()


def get_field_weight(field_type: str) -> float:
    weight = {
        "name": 0.8,
        "description": 1.0,
        "image": 1.3
    }
    return weight.get(field_type, 1.0)


def distance_to_score(distance: float) -> float:
    return 1 / (1 + distance)


@router.get("/recommend")
async def recommend_products(prompt: str = Query(...), top_k: int = 5):
    # Dùng SigLIP để nhúng câu hỏi (Vector 768 chiều)
    query_emb = encode_product_text(prompt)
    query_bytes = serialize_f32(query_emb)

    db = get_db_connection()
    try:
        # INNER JOIN cấu trúc mới
        rows = db.execute("""
                          SELECT rowid,
                                 item_id,
                                 field_type,
                                 image_index,
                                 content,
                                 distance
                          FROM vec_items
                          WHERE embedding MATCH ?
                            AND k = ?
                          """, [query_bytes, 50]).fetchall()
        best_item = {}
        for row in rows:
            vector_score = distance_to_score(row["distance"])
            weight = get_field_weight(row["field_type"])
            final_score = vector_score * weight

            item_id = row["item_id"]
            result = {
                "item_id": item_id,
                "field_type": row["field_type"],
                "image_index": row["image_index"],
                "content": row["content"],
                "distance": row["distance"],
                "vector_score": vector_score,
                "field_weight": weight,
                "final_score": final_score
            }
            if item_id not in best_item:
                best_item[item_id] = result
            else:
                if final_score > best_item[item_id]["final_score"]:
                    best_item[item_id] = result
        ranked_results = sorted(best_item.values(), key=lambda x: x["final_score"], reverse=True)
        ranked_results = ranked_results[:top_k]

        recommendations = []
        for result in ranked_results:
            item = db.execute("""
                              SELECT item_id, name, description
                              FROM items_info
                              WHERE item_id = ?
                              """, [result["item_id"]]).fetchone()

            recommendations.append({
                "id": result["item_id"],
                "name": item["name"] if item else None,
                "description": item["description"] if item else None,
                "image_index": result["image_index"],
                "distance": result["distance"],
                "vector_score": result["vector_score"],
                "field_weight": result["field_weight"],
                "final_score": result["final_score"]
            })

        return {
            "query": prompt,
            "recommendations": recommendations
        }
    except Exception as e:
        print(e)
        return {
            "status": "error",
            "message": str(e)
        }
    finally:
        db.close()
