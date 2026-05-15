import os
import sqlite3
import struct

import numpy as np
import sqlite_vec
from dotenv import load_dotenv

load_dotenv()


# Hàm chuyển đổi mảng số thành nhị phân siêu tốc
def serialize_f32(vector):
    flat_vector = np.array(vector).flatten().astype(np.float32)
    return struct.pack(f"{len(flat_vector)}f", *flat_vector)


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
    # 2. Bảng lưu Vector Tài liệu HDSD
    db.execute("""
        CREATE VIRTUAL TABLE IF NOT EXISTS vec_docs USING vec0(
            chunk_id TEXT PRIMARY KEY,
            embedding float[384]
        );
    """)

    # 3. Bảng chuẩn SQLite lưu nội dung chữ của tài liệu
    db.execute("""
               CREATE TABLE IF NOT EXISTS docs_info
               (
                   chunk_id
                   TEXT
                   PRIMARY
                   KEY,
                   doc_name
                   TEXT,
                   content
                   TEXT
               );
               """)
    db.commit()
    db.close()


def search_similar_items(query_vector, top_k=10):
    db = get_db_connection()
    # Sử dụng hàm vec_distance_cosine để tìm các sản phẩm gần nhất
    rows = db.execute("""
                      SELECT item_id,
                             vec_distance_cosine(v.embedding, ?) as distance
                      FROM vec_items v
                      ORDER BY distance ASC LIMIT ?
                      """, [serialize_f32(query_vector), top_k]).fetchall()
    db.close()
    return rows
