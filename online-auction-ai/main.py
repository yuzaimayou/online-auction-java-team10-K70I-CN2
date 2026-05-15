import os
import shutil
import struct
from typing import Annotated

import numpy as np
from fastapi import FastAPI, UploadFile, File, Form
from fastapi.params import Query
from sentence_transformers import SentenceTransformer

from chunking import chunk_markdown_file
from database import init_vector_db, search_similar_items, get_db_connection
from engine import SigLIPEngine

# Load mô hình chuyên trị Text siêu nhẹ (Chạy lần đầu sẽ mất vài chục giây tải model về)
text_embedder = SentenceTransformer('all-MiniLM-L6-v2')

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


@app.post("/index-docs")
async def index_documents(file: UploadFile = File(...)):
    db = get_db_connection()
    try:
        # 1. Đọc nội dung file Markdown
        content_bytes = await file.read()
        text_content = content_bytes.decode('utf-8')
        doc_name = file.filename
        # 2. Băm nhỏ văn bản
        chunks = chunk_markdown_file(text_content)
        # 3. Quét từng đoạn, nhúng Vector và Lưu DB
        for i, chunk_text in enumerate(chunks):
            chunk_id = f"doc_{doc_name}_chunk_{i}"

            # Nhúng đoạn văn bản thành Vector bằng SigLIP
            emb = text_embedder.encode(chunk_text)
            emb_bytes = serialize_f32(emb)

            # Lưu vào bảng Vector (để AI tìm kiếm)
            db.execute("INSERT OR REPLACE INTO vec_docs(chunk_id, embedding) VALUES (?, ?)",
                       [chunk_id, emb_bytes])

            # Lưu vào bảng Text (để trả về cho Java)
            db.execute("INSERT OR REPLACE INTO docs_info(chunk_id, doc_name, content) VALUES (?, ?, ?)",
                       [chunk_id, doc_name, chunk_text])

        db.commit()
        return {
            "status": "success",
            "message": f"Đã nhúng thành công file {doc_name} thành {len(chunks)} đoạn."
        }
    except Exception as e:
        return {"status": "error", "message": str(e)}
    finally:
        db.close()


@app.post("/search_docs")
async def search_docs(query: str = Query(...), top_k: int = 3):
    # 1. Nhúng câu hỏi của User thành Vector
    query_vector = engine.encode_text(query)
    query_bytes = serialize_f32(query_vector)

    db = get_db_connection()
    try:
        # 2. INNER JOIN giữa bảng Vector và bảng Text để lấy ngay nội dung chữ
        rows = db.execute("""
                          SELECT d.content, d.doc_name, vec_distance_cosine(v.embedding, ?) as distance
                          FROM vec_docs v
                                   INNER JOIN docs_info d ON v.chunk_id = d.chunk_id
                          ORDER BY distance ASC LIMIT ?
                          """, [query_bytes, top_k]).fetchall()

        results = []
        for row in rows:
            content = row[0]
            doc_name = row[1]
            similarity = 1.0 - row[2]

            # Chỉ lấy những đoạn có độ tương đồng đủ tốt (có thể tinh chỉnh ngưỡng này)
            if similarity > 0.15:
                results.append({
                    "doc_name": doc_name,
                    "content": content,
                    "similarity": similarity
                })

        return {"results": results}
    finally:
        db.close()


@app.post("/index-product/{item_id}")
async def index_product(
        item_id: str,
        name: Annotated[str, Form()],  # Nhận thêm tên từ Form
        description: Annotated[str, Form()],
        files: Annotated[list[UploadFile], File(description="Multiple files as UploadFile")]
):
    print("item_id:", item_id)
    print("name:", name)
    print("description:", description)
    print("files:", [file.filename for file in files])
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
    imgs_embedding = average_embeddings(all_embeddings)

    # tao vector cho phần mô tả văn bản (để AI có thể so sánh Chữ và Ảnh)
    full_text = f"Name: {name}. Description: {description}"
    text_emb = engine.encode_text(full_text)

    # Hợp nhất Đa phương thức (Multimodal Fusion)
    final_embedding = (np.array(imgs_embedding) * 8 + np.array(text_emb) * 2) / 10

    # Ép kiểu nhị phân để lưu DB (Sử dụng lại hàm serialize_f32 bạn đã viết)
    embedding_bytes = serialize_f32(final_embedding)

    # Lưu 1 vector duy nhất vào Database
    # Lưu vào Database (Xóa cũ - Ghi mới để tránh lỗi Unique)
    db = get_db_connection()
    try:
        db.execute("DELETE FROM vec_items WHERE item_id = ?", [item_id])
        db.execute("INSERT OR REPLACE INTO vec_items(item_id, embedding) VALUES (?, ?)",
                   [item_id, embedding_bytes])
        db.commit()
    except Exception as e:
        return {"status": "error", "message": str(e)}
    finally:
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
        {"id": r[0], "similarity": 1 - r[1]}
        for r in results
    ]
    return {"recommendations": formatted_results}
