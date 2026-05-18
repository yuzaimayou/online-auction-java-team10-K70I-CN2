import struct

from fastapi import APIRouter, UploadFile, File, Query

from app.core.database import get_db_connection
from app.services.text_service import get_text_embedding
from app.utils.chunking import chunk_markdown_syntax  # Chứa hàm cắt văn bản bạn đã viết

router = APIRouter()


# Hàm phụ trợ chuyển đổi sang nhị phân
def serialize_f32(vector):
    return struct.pack('%sf' % len(vector), *vector)


@router.post("/index-docs")
async def index_documents(file: UploadFile = File(...)):
    db = get_db_connection()
    try:
        content_bytes = await file.read()
        text_content = content_bytes.decode('utf-8')
        doc_name = file.filename

        chunks = chunk_markdown_syntax(text_content, max_chars=800)

        for i, chunk_text in enumerate(chunks):
            chunk_id = f"doc_{doc_name}_chunk_{i}"

            # Lưu Text trước
            cursor = db.execute(
                "INSERT INTO docs_info(chunk_id, doc_name, content) VALUES (?, ?, ?)",
                [chunk_id, doc_name, chunk_text]
            )
            auto_id = cursor.lastrowid

            # Nhúng Vector và Lưu Vector
            emb = get_text_embedding(chunk_text)
            emb_bytes = serialize_f32(emb)

            db.execute("INSERT INTO vec_docs(rowid, embedding) VALUES (?, ?)", [auto_id, emb_bytes])

        db.commit()
        return {"status": "success", "message": f"Đã nhúng {len(chunks)} đoạn."}
    finally:
        db.close()


@router.post("/search-docs")
async def search_docs(query: str = Query(...), top_k: int = 3):
    query_vector = get_text_embedding(query)
    query_bytes = serialize_f32(query_vector)

    db = get_db_connection()
    try:
        rows = db.execute("""
                          SELECT d.content, d.doc_name, vec_distance_cosine(v.embedding, ?) as distance
                          FROM vec_docs v
                                   INNER JOIN docs_info d ON v.rowid = d.id
                          ORDER BY distance ASC LIMIT ?
                          """, [query_bytes, top_k]).fetchall()

        results = []
        for row in rows:
            if 1.0 - row[2] > 0.15:
                results.append({"doc_name": row[1], "content": row[0], "similarity": 1.0 - row[2]})

        return {"results": results}
    finally:
        db.close()
