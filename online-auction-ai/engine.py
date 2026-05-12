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
        image = Image.open(image_path).convert("RGB")
        inputs = self.processor(images=image, return_tensors="pt").to(self.device)
        with torch.no_grad():
            image_features = self.model.get_image_features(**inputs)
        # Chuẩn hóa vector về độ dài 1 (quan trọng cho Cosine Similarity)
        return image_features[0].cpu().tolist()

    def encode_text(self, text):
        inputs = self.processor(text=[text], padding="max_length", return_tensors="pt").to(self.device)
        with torch.no_grad():
            text_features = self.model.get_text_features(**inputs)
        return text_features[0].cpu().tolist()
