import os
import sqlite3

import sqlite_vec
from dotenv import load_dotenv

load_dotenv()


def get_db_connection():
    db_path = os.getenv("DATABASE_PATH")
    db = sqlite3.connect(db_path)
    db.enable_load_extension(True)
    sqlite_vec.load(db)
    db.enable_load_extension(False)
    return db


def init_vector_db():
    db = get_db_connection()
    # Tạo bảng ảo để lưu trữ vector sản phẩm ( SigLIP-base tạo ra vector 768 chiều)
    db.execute("""
        CREATE VIRTUAL TABLE IF NOT EXISTS vec_items USING vec0(
            item_id TEXT PRIMARY KEY,
            embedding FLOAT[768]
        )
    """)
    db.commit()
    db.close()


def search_similar_items(query_vector, top_k=4):
    db = get_db_connection()
    # Sử dụng hàm vec_distance_cosine để tìm các sản phẩm gần nhất
    rows = db.execute("""
                      SELECT v.item_id,
                             i.name,
                             i.current_price,
                             vec_distance_cosine(v.embedding, ?) as distance
                      FROM vec_items v
                               JOIN items i ON v.item_id = i.id
                      ORDER BY distance ASC LIMIT ?
                      """, [str(query_vector), top_k]).fetchall()
    db.close()
    return rows
