# app/services/vision_service.py
import torch
from PIL import Image
# Giả định bạn đang dùng transformers của HuggingFace cho SigLIP
from transformers import AutoProcessor, AutoModel

print("Đang khởi động (SigLIP-base)...")
# Tùy thuộc vào model cụ thể bạn đang dùng ở local, hãy đổi đường dẫn phù hợp
MODEL_NAME = "google/siglip-base-patch16-224"
processor = AutoProcessor.from_pretrained(MODEL_NAME)
model = AutoModel.from_pretrained(MODEL_NAME)
model.eval()  # Chuyển sang chế độ chạy suy luận (Inference)
print("✅ Khởi động SigLIP thành công!")


def encode_product_image(image_path: str) -> list[float]:
    """Đọc ảnh từ ổ cứng và nhúng thành Vector 768 chiều"""
    image = Image.open(image_path).convert("RGB")
    inputs = processor(images=image, return_tensors="pt")
    with torch.no_grad():
        outputs = model.get_image_features(**inputs)
    # Trả về list các số thực
    if hasattr(outputs, 'image_embeds'):
        tensor_data = outputs.image_embeds

    elif hasattr(outputs, 'pooler_output'):
        tensor_data = outputs.pooler_output

    elif hasattr(outputs, "last_hidden_state"):
        tensor_data = outputs.last_hidden_state

    elif isinstance(outputs, torch.Tensor):
        tensor_data = outputs
    else:
        raise ValueError("Unexpected output format from model")

        # 5. Ép kiểu về numpy, LÀM PHẲNG (để tránh mảng lồng mảng), và chuyển thành list
    embedding = tensor_data.cpu().numpy().flatten().tolist()

    return embedding


def encode_product_text(text: str) -> list[float]:
    """Nhúng mô tả sản phẩm thành Vector 768 chiều để so khớp chéo với ảnh"""
    inputs = processor(text=text, padding="max_length", return_tensors="pt")
    with torch.no_grad():
        outputs = model.get_text_features(**inputs)
    # Bóc tách an toàn cho Text
    if hasattr(outputs, 'text_embeds'):
        tensor_data = outputs.text_embeds
    elif hasattr(outputs, 'pooler_output'):
        tensor_data = outputs.pooler_output
    else:
        tensor_data = outputs
    embedding = tensor_data.cpu().numpy().flatten().tolist()
    return embedding
