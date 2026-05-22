import struct

from fastapi import APIRouter, UploadFile, File, Query

from app.core.database import get_db_connection
from app.services.text_service import get_text_embedding
from app.utils.chunking import chunk_markdown_by_headings  # Chứa hàm cắt văn bản bạn đã viết

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

        chunks = chunk_markdown_by_headings(text_content)
        for chunk in chunks:
            print("TITLE:", chunk["title"])
            print("PATH:", chunk["path"])
            print("CONTENT:", chunk["content"][:200])
            print("---")
        inserted_chunks = 0
        for i, chunk in enumerate(chunks):
            title = chunk.get("title", "")
            path = chunk.get("path", "")
            content = chunk.get("content", "")
            if not content.strip():
                continue
            chunk_id = f"{doc_name}_chunk_{i}"
            embedding_text = """
            title:{title}
            path:{path}
            content:{content}
            """.strip()
            # Save the text first
            cursor = db.execute(
                """
                INSERT INTO docs_info
                    (chunk_id, doc_name, title, path, content)
                VALUES (?, ?, ?, ?, ?)
                """,
                [chunk_id, doc_name, title, path, content]
            )
            auto_id = cursor.lastrowid
            # embedding
            emb = get_text_embedding(embedding_text)
            emb_bytes = serialize_f32(emb)
            # save vector
            db.execute(
                """
                INSERT INTO vec_docs(rowid, embedding)
                VALUES (?, ?)
                """,
                [auto_id, emb_bytes]
            )
            inserted_chunks += 1

        db.commit()
        return {"status": "success", "message": f"Đã nhúng {inserted_chunks} đoạn."}
    except Exception as e:
        db.rollback()
        print("Error indexing document:", e)
        return {"status": "error", "message": str(e)}
    finally:
        db.close()


@router.post("/search-docs")
async def search_docs(query: str = Query(...), top_k: int = 3):
    db = get_db_connection()
    try:
        query_vector = get_text_embedding(query)
        query_bytes = serialize_f32(query_vector)

        rows = db.execute("""
                          SELECT d.id,
                                 d.chunk_id,
                                 d.doc_name,
                                 d.title,
                                 d.path,
                                 d.content,
                                 v.distance
                          FROM vec_docs v
                                   JOIN docs_info d ON v.rowid = d.id
                          WHERE v.embedding MATCH ?
                            AND k = ?
                          ORDER BY v.distance
                          """, [query_bytes, top_k]).fetchall()

        results = []
        for row in rows:
            results.append({
                "id": row["id"],
                "chunk_id": row["chunk_id"],
                "doc_name": row["doc_name"],
                "title": row["title"],
                "path": row["path"],
                "content": row["content"],
                "distance": row["distance"]
            })

        return {
            "status": "success",
            "query": query,
            "results": results
        }
    except Exception as e:
        print("SEARCH DOCS ERROR:", str(e))
        return {
            "status": "error",
            "message": str(e)
        }
    finally:
        db.close()
