import os

import torch
from PIL import Image
from dotenv import load_dotenv
from transformers import AutoProcessor, AutoModel

load_dotenv()


class SigLIPEngine:
    def __init__(self):
        model_id = os.getenv("MODEL_ID", "google/siglip-base-patch16-224")
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.model = AutoModel.from_pretrained(model_id).to(self.device)
        self.processor = AutoProcessor.from_pretrained(model_id)

    def encode_image(self, image_path):
        # 1. Đọc và chuyển đổi ảnh về chuẩn RGB
        image = Image.open(image_path).convert("RGB")

        # 2. Đưa qua Processor để tạo pixel_values (Mảng 150.528 chiều)
        inputs = self.processor(images=image, return_tensors="pt").to(self.device)

        # 3. Nạp vào Model để trích xuất Embedding
        with torch.no_grad():
            outputs = self.model.get_image_features(**inputs)

        # 4. Xử lý an toàn định dạng trả về (Bảo vệ khỏi lỗi AttributeError)
        # Nếu outputs là Object Container, ta bóc lấy phần lõi

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

    def encode_text(self, text):
        # Xử lý tương tự cho văn bản để AI có thể so sánh Chữ và Ảnh
        inputs = self.processor(text=text, padding="max_length", return_tensors="pt").to(self.device)

        with torch.no_grad():
            outputs = self.model.get_text_features(**inputs)

        # Bóc tách an toàn cho Text
        if hasattr(outputs, 'text_embeds'):
            tensor_data = outputs.text_embeds
        elif hasattr(outputs, 'pooler_output'):
            tensor_data = outputs.pooler_output
        else:
            tensor_data = outputs

        embedding = tensor_data.cpu().numpy().flatten().tolist()

        return embedding
