import os
import shutil

from fastapi import FastAPI, UploadFile, File

from database import init_vector_db, search_similar_items, get_db_connection
from engine import SigLIPEngine

app = FastAPI()
engine = SigLIPEngine()


@app.on_event("startup")
async def startup_event():
    init_vector_db()


@app.post("/index-product/{item_id}")
async def index_product(item_id: str, file: UploadFile = File(...)):
    # 1. Lưu ảnh tạm thời
    temp_path = f"temp_{file.filename}"
    with open(temp_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    # 2. Tạo vector từ ảnh
    embedding = engine.encode_image(temp_path)

    # 3. Lưu vào Vector DB
    db = get_db_connection()
    db.execute("INSERT OR REPLACE INTO vec_items(item_id, embedding) VALUES (?, ?)",
               [item_id, str(embedding)])
    db.commit()
    db.close()

    os.remove(temp_path)
    return {"status": "indexed", "item_id": item_id}


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
