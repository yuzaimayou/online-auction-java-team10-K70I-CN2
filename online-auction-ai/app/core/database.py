import sqlite3

import sqlite_vec

from app.core.config import settings


def get_db_connection():
    # Sử dụng đường dẫn từ file config
    conn = sqlite3.connect(settings.DATABASE_PATH)
    conn.enable_load_extension(True)
    sqlite_vec.load(conn)
    conn.enable_load_extension(False)
    return conn


def init_db():
    db = get_db_connection()

    # Dọn dẹp bảng cũ
    db.execute("DROP TABLE IF EXISTS vec_docs;")
    db.execute("DROP TABLE IF EXISTS docs_info;")
    db.execute("DROP TABLE IF EXISTS vec_items;")
    db.execute("DROP TABLE IF EXISTS items_info;")

    # Bảng Vector cho Tài liệu (384 chiều)
    db.execute("""
        CREATE VIRTUAL TABLE IF NOT EXISTS vec_docs USING vec0(
            embedding float[384]
        );
    """)

    # Bảng Text cho Tài liệu
    db.execute("""
               CREATE TABLE IF NOT EXISTS docs_info
               (
                   id
                   INTEGER
                   PRIMARY
                   KEY
                   AUTOINCREMENT,
                   chunk_id
                   TEXT,
                   doc_name
                   TEXT,
                   content
                   TEXT
               );
               """)

    db.execute("CREATE VIRTUAL TABLE IF NOT EXISTS vec_items USING vec0(embedding float[768]);")
    db.execute("""
               CREATE TABLE IF NOT EXISTS items_info
               (
                   id
                   INTEGER
                   PRIMARY
                   KEY
                   AUTOINCREMENT,
                   item_id
                   TEXT,
                   type
                   TEXT
               );
               """)

    # GHI CHÚ: Sau này bạn thêm bảng vec_items (Sản phẩm) vào đây nhé
    db.commit()
    db.close()
