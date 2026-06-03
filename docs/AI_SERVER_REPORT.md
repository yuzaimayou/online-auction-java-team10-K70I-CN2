# BÁO CÁO KỸ THUẬT: AI SERVER
## Hệ thống Đấu Giá Trực Tuyến — Microservice AI

**Dự án:** Online Auction System — Team 10 (K70I-CN2)
**Ngày báo cáo:** 03/06/2026
**Người viết:** Software Engineer
**Module:** `online-auction-ai`

---

## 1. TỔNG QUAN

AI Server là một **microservice độc lập** được xây dựng bằng Python, cung cấp khả năng **tìm kiếm ngữ nghĩa đa phương thức (multimodal semantic search)** cho hệ thống đấu giá trực tuyến. Dịch vụ này cho phép người dùng tìm kiếm và nhận gợi ý sản phẩm dựa trên nội dung ngữ nghĩa của cả **ảnh lẫn văn bản**, thay vì chỉ dựa vào từ khóa đơn thuần.

Dịch vụ hoạt động như một **vector search engine** — chuyển đổi dữ liệu sản phẩm (ảnh, tên, mô tả) thành các vector embedding 768 chiều, sau đó lưu trữ và tìm kiếm theo độ tương đồng cosine.

---

## 2. KIẾN TRÚC HỆ THỐNG

### 2.1. Vị trí trong kiến trúc tổng thể

```
┌─────────────────┐         HTTP          ┌──────────────────────┐
│  JavaFX Client  │ ───────────────────→  │   Java Server :8080  │
│  (Giao diện)    │                       │   (Business Logic)   │
└─────────────────┘                       └──────────┬───────────┘
                                                      │ HTTP REST
                                                      │ (localhost)
                                                      ▼
                                          ┌──────────────────────┐
                                          │  AI Server :8000     │
                                          │  (Python/FastAPI)    │
                                          └──────────┬───────────┘
                                                      │
                                                      ▼
                                          ┌──────────────────────┐
                                          │  SQLite + sqlite-vec │
                                          │  (Vector Database)   │
                                          └──────────────────────┘
```

### 2.2. Cấu trúc thư mục

```
online-auction-ai/
├── app/
│   ├── api/
│   │   ├── endpoints/
│   │   │   ├── products.py        # API tìm kiếm & gợi ý sản phẩm
│   │   │   └── docs.py            # API quản lý knowledge base
│   │   └── router.py              # Tổng hợp routing
│   ├── core/
│   │   ├── config.py              # Quản lý cấu hình (Pydantic Settings)
│   │   └── database.py            # Khởi tạo & quản lý database
│   ├── services/
│   │   ├── vision_service.py      # Embedding bằng SigLIP (ảnh + văn bản)
│   │   └── text_service.py        # Text embedding (dự phòng, chưa dùng)
│   ├── utils/
│   │   └── chunking.py            # Chia nhỏ tài liệu Markdown
│   └── main.py                    # Entry point FastAPI
├── engine.py                      # SigLIP engine (phiên bản thay thế)
├── requirements.txt               # Danh sách dependencies
├── .env                           # Biến môi trường
├── data/
│   └── vector_store.db            # SQLite database với vector extension
└── temp/                          # Thư mục xử lý file tạm
```

---

## 3. CÔNG NGHỆ SỬ DỤNG

| Thành phần | Công nghệ | Phiên bản | Mục đích |
|---|---|---|---|
| Web Framework | FastAPI | 0.136.1 | REST API async |
| ASGI Server | Uvicorn | 0.46.0 | Chạy FastAPI |
| AI Model | Google SigLIP | base-patch16-224 | Tạo vector embedding |
| Deep Learning | PyTorch | 2.11.0 | Inference engine |
| Model Hub | HuggingFace Transformers | 5.8.0 | Load model |
| Image Processing | Pillow | 12.2.0 | Đọc & xử lý ảnh |
| Vector Database | sqlite-vec | 0.1.9 | Lưu trữ & tìm kiếm vector |
| Configuration | Pydantic Settings | 2.14.1 | Quản lý cấu hình |
| GPU Acceleration | NVIDIA CUDA | 13.0+ | Tăng tốc inference |
| Semantic Similarity | sentence-transformers | 5.5.0 | Đã cài, chưa dùng |

### 3.1. Model AI — Google SigLIP

**SigLIP** (Sigmoid Loss for Language-Image Pre-training) là model của Google được huấn luyện để hiểu mối quan hệ giữa ảnh và văn bản trong **cùng một không gian vector 768 chiều**.

Ưu điểm so với CLIP truyền thống:
- Sử dụng sigmoid loss thay vì softmax — cho phép tìm kiếm linh hoạt hơn
- Hiệu quả với batch nhỏ
- Tốt hơn trong zero-shot image classification

---

## 4. CÁC API ENDPOINT

**Base URL:** `http://127.0.0.1:8000`

### 4.1. Kiểm tra trạng thái

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/` | Health check — kiểm tra server đang chạy |

**Response mẫu:**
```json
{"message": "AI Server is running smoothly! 🚀"}
```

---

### 4.2. Products API — `/products`

#### `POST /products/index-product/{item_id}`

**Mục đích:** Index một sản phẩm vào vector database.

**Parameters:**

| Tên | Loại | Bắt buộc | Mô tả |
|---|---|---|---|
| `item_id` | path param | Có | ID định danh sản phẩm |
| `name` | form field | Có | Tên sản phẩm |
| `description` | form field | Có | Mô tả sản phẩm |
| `files` | file upload | Có | Ảnh sản phẩm (nhiều ảnh) |

**Luồng xử lý:**
```
1. Nhận request với ảnh + metadata
2. Lưu metadata vào bảng items_info
3. Với mỗi ảnh: encode → vector 768 chiều → lưu vào vec_items (field_type="image")
4. Encode tên sản phẩm → vector → lưu (field_type="name")
5. Encode mô tả → vector → lưu (field_type="description")
6. Xóa file tạm
7. Trả về kết quả
```

**Response mẫu (thành công):**
```json
{
  "status": "success",
  "message": "Product indexed successfully",
  "item_id": "item_123"
}
```

---

#### `GET /products/recommend`

**Mục đích:** Tìm kiếm sản phẩm tương tự theo truy vấn văn bản.

**Parameters:**

| Tên | Loại | Mặc định | Mô tả |
|---|---|---|---|
| `prompt` | query string | Bắt buộc | Câu truy vấn tìm kiếm |
| `top_k` | query int | 5 | Số kết quả trả về |

**Luồng xử lý:**
```
1. Encode prompt → vector 768 chiều
2. Tìm top-50 vector gần nhất trong vec_items (tất cả field_type)
3. Áp dụng trọng số:
   - image:       weight = 1.3  (ưu tiên cao nhất)
   - description: weight = 1.0
   - name:        weight = 0.8
4. Tính final_score = vector_distance × field_weight
5. Gộp kết quả theo item_id, giữ điểm tốt nhất
6. Sắp xếp & trả về top_k
```

**Response mẫu:**
```json
{
  "query": "đồng hồ cổ vintage",
  "recommendations": [
    {
      "id": "item_042",
      "name": "Đồng hồ Seiko 1975",
      "description": "Đồng hồ cổ điển...",
      "image_index": 0,
      "distance": 0.182,
      "vector_score": 0.818,
      "field_weight": 1.3,
      "final_score": 0.927
    }
  ]
}
```

---

### 4.3. Docs API — `/docs`

#### `POST /docs/index-docs`

**Mục đích:** Nạp tài liệu Markdown vào knowledge base.

| Tên | Loại | Mô tả |
|---|---|---|
| `file` | file upload | Tài liệu `.md` cần index |

**Luồng xử lý:** Đọc file → phân tích heading → chia nhỏ (chunk) theo cấu trúc → lưu từng chunk vào `docs_info`.

**Response:**
```json
{"chunks_indexed": 12}
```

---

#### `GET /docs/get-docs`

**Mục đích:** Lấy toàn bộ tài liệu đã index, dùng cho RAG context.

**Response:** Văn bản định dạng sẵn chứa tiêu đề, đường dẫn (breadcrumb) và nội dung từng chunk.

---

## 5. CƠ SỞ DỮ LIỆU

**Engine:** SQLite với extension `sqlite-vec`
**File:** `data/vector_store.db`

### 5.1. Schema

```sql
-- Metadata sản phẩm
CREATE TABLE items_info (
    item_id     TEXT PRIMARY KEY,
    name        TEXT,
    description TEXT
);

-- Bảng vector sản phẩm (virtual table của sqlite-vec)
CREATE VIRTUAL TABLE vec_items USING vec0(
    item_id      TEXT,
    field_type   TEXT,          -- "image" | "name" | "description"
    image_index  INTEGER,       -- chỉ số ảnh (-1 nếu là text)
    embedding    FLOAT[768],    -- vector 768 chiều
    +content     TEXT           -- nội dung gốc (stored field)
);

-- Metadata tài liệu knowledge base
CREATE TABLE docs_info (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    chunk_id TEXT,
    doc_name TEXT,
    title    TEXT,
    path     TEXT,              -- breadcrumb: "Chương 1 > Mục 2 > ..."
    content  TEXT
);

-- Bảng vector tài liệu (hiện đã tắt)
CREATE VIRTUAL TABLE vec_docs USING vec0(
    embedding FLOAT[384]
);
```

### 5.2. Chiến lược lập chỉ mục

Mỗi sản phẩm được lập chỉ mục thành **nhiều vector** trong cùng một bảng:

```
item_id: "phone_001"
├── embedding[0] — image_0      (field_type="image",       image_index=0)
├── embedding[1] — image_1      (field_type="image",       image_index=1)
├── embedding[2] — name         (field_type="name",        image_index=-1)
└── embedding[3] — description  (field_type="description", image_index=-1)
```

Điều này cho phép tìm kiếm đa chiều từ cùng một câu truy vấn.

---

## 6. LUỒNG DỮ LIỆU TÍCH HỢP VỚI JAVA SERVER

### 6.1. Index sản phẩm (khi tạo mới)

```
[Java Client]
    │  multipart/form-data (ảnh + metadata)
    ▼
[Java Server]
    │  POST /products/index-product/{itemId}
    │  multipart (name, description, files[])
    ▼
[AI Server]
    │  1. Encode từng ảnh → vector 768D
    │  2. Encode tên, mô tả → vector 768D
    │  3. INSERT INTO vec_items
    ▼
[SQLite Vector DB]
```

### 6.2. Tìm kiếm / Gợi ý sản phẩm

```
[Java Client]
    │  gửi search prompt
    ▼
[Java Server — AiServiceClient]
    │  GET /products/recommend?prompt={query}&top_k=5
    ▼
[AI Server]
    │  1. Encode prompt → vector 768D
    │  2. KNN search top-50 theo cosine distance
    │  3. Weight & merge → top_k kết quả
    ▼
[Java Server]
    │  Map → AIResponse, AIResponseItem
    ▼
[Java Client]
    │  Hiển thị gợi ý
```

### 6.3. Tích hợp Knowledge Base (RAG)

```
[Java Server — AiServiceClient]
    │  GET /docs/get-docs
    │  (Cache 12 giờ phía Java)
    ▼
[AI Server]
    │  Trả về toàn bộ docs context
    ▼
[Java Server]
    │  Kết hợp với câu hỏi người dùng → RAG prompt
    │  Gọi LLM (Gemini/GPT) để sinh câu trả lời
```

---

## 7. CẤU HÌNH

**File `.env`:**
```env
DATABASE_PATH=data/vector_store.db
MODEL_ID=google/siglip-base-patch16-224
```

**Các thông số kết nối (phía Java):**
```
AI Server URL: http://127.0.0.1:8000
Cache TTL docs: 12 giờ
```

---

## 8. ĐIỂM MẠNH

| # | Điểm mạnh | Chi tiết |
|---|---|---|
| 1 | **Tìm kiếm ngữ nghĩa** | Hiểu ngữ nghĩa câu truy vấn, không chỉ dựa vào từ khóa |
| 2 | **Multimodal** | Kết hợp vector ảnh + văn bản trong cùng không gian embedding |
| 3 | **Weighted scoring** | Phân trọng số linh hoạt theo loại dữ liệu |
| 4 | **Async API** | FastAPI native async — hiệu năng cao, không blocking |
| 5 | **Tách biệt rõ ràng** | Microservice độc lập, dễ scale và maintain |
| 6 | **GPU ready** | Hỗ trợ CUDA cho inference nhanh ở production |
| 7 | **Lightweight DB** | SQLite + sqlite-vec — không cần hạ tầng phức tạp |

---

## 9. HẠN CHẾ VÀ ĐỀ XUẤT CẢI TIẾN

### 9.1. Hạn chế hiện tại

| # | Vấn đề | Mô tả |
|---|---|---|
| 1 | **Vector docs chưa hoàn thiện** | Bảng `vec_docs` và endpoint `search-docs` bị comment out — RAG chưa dùng được vector search |
| 2 | **Không có authentication** | API không có bảo vệ — ai có thể truy cập đều gọi được |
| 3 | **URL hardcode** | AI Server URL (`127.0.0.1:8000`) hardcode trong Java — không linh hoạt khi deploy |
| 4 | **Không có pagination** | Endpoint `get-docs` trả về toàn bộ dữ liệu — sẽ chậm khi knowledge base lớn |
| 5 | **Thiếu error handling** | Một số endpoint chưa xử lý đầy đủ trường hợp lỗi (file corrupt, model timeout) |
| 6 | **text_service.py chưa dùng** | Code dự phòng tồn tại nhưng không được tích hợp |

### 9.2. Đề xuất cải tiến

1. **Bật vector search cho docs** — Hoàn thiện `vec_docs` và endpoint `search-docs` để RAG hoạt động đúng nghĩa.
2. **Thêm API key authentication** — Đơn giản nhất là dùng HTTP header `X-API-Key`.
3. **Externalize config** — Đưa AI Server URL sang file cấu hình Java (application.properties).
4. **Thêm logging** — Ghi log request/response để debug dễ hơn ở production.
5. **Containerize** — Đóng gói thành Docker image để triển khai nhất quán.
6. **Health check endpoint chuẩn** — Thêm `/health` trả về trạng thái model, database.

---

## 10. HƯỚNG DẪN VẬN HÀNH

### Khởi động AI Server

```bash
cd online-auction-ai

# Tạo và kích hoạt virtual environment
python -m venv venv
source venv/bin/activate        # Linux/macOS
# hoặc: venv\Scripts\activate   # Windows

# Cài dependencies
pip install -r requirements.txt

# Chạy server
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### Kiểm tra hoạt động

```bash
# Health check
curl http://localhost:8000/

# Thử gợi ý sản phẩm
curl "http://localhost:8000/products/recommend?prompt=đồng+hồ+cổ&top_k=3"
```

### Truy cập tài liệu API tự động (Swagger UI)

```
http://localhost:8000/docs
http://localhost:8000/redoc
```

---

## 11. KẾT LUẬN

AI Server của hệ thống đấu giá trực tuyến là một thành phần kỹ thuật có giá trị cao, cung cấp khả năng tìm kiếm ngữ nghĩa multimodal thông qua mô hình SigLIP của Google. Hệ thống được xây dựng theo kiến trúc microservice rõ ràng, sử dụng các công nghệ hiện đại và phù hợp cho bài toán.

Các tính năng cốt lõi đã hoạt động tốt: **index sản phẩm**, **tìm kiếm ngữ nghĩa**, và **tích hợp knowledge base**. Phần RAG vector search (tìm kiếm trong tài liệu) đang trong giai đoạn phát triển và cần hoàn thiện thêm.

Với những cải tiến được đề xuất ở Mục 9, hệ thống có tiềm năng phục vụ tốt ở môi trường production với hiệu năng và độ tin cậy cao.

---

*Báo cáo được tạo bởi Software Engineer — dựa trên phân tích toàn bộ source code tại `online-auction-ai/`*
