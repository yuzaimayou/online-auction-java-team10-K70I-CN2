from sentence_transformers import SentenceTransformer

print("Đang khởi động não bộ Text (MiniLM-L6-v2)...")
text_model = SentenceTransformer('all-MiniLM-L6-v2')
print("Khởi động Text Model thành công!")


def get_text_embedding(text: str):
    """Nhận vào đoạn văn bản, nhả ra vector 384 chiều"""
    return text_model.encode(text)
