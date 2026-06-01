# Tài liệu Thư viện AI Microservice - Hệ thống Đấu giá (Team 10)

Tài liệu này mô tả chi tiết các thư viện Python được sử dụng trong module `online-auction-ai`. Các thư viện này được lựa chọn để hỗ trợ kiến trúc **Multimodal RAG** (truy xuất đa phương thức), cho phép tìm kiếm sản phẩm đấu giá bằng hình ảnh thông qua mô hình **SigLIP**.

---

## 1. Nhóm Web & Giao tiếp (Backend API)

Đây là tầng giao tiếp giúp Java Server (Main Server) có thể gửi yêu cầu và nhận kết quả từ AI.

### **FastAPI**
- **Vai trò:** Framework web hiện đại, hiệu năng cao.
- **Công dụng:** Dùng để định nghĩa các điểm cuối (endpoints) API. Trong dự án, nó tiếp nhận hình ảnh/text từ Java, chuyển tiếp đến AI Engine và trả về kết quả gợi ý dưới dạng JSON.
- **Tại sao chọn:** Tự động sinh tài liệu Swagger UI (giúp Team 10 dễ dàng test API mà không cần viết code Java ngay), hỗ trợ xử lý bất đồng bộ (Asynchronous) cực tốt.

### **Uvicorn**
- **Vai trò:** ASGI Web Server.
- **Công dụng:** Là môi trường thực thi để chạy ứng dụng FastAPI. Nó lắng nghe các kết nối từ Java Server qua cổng mạng (mặc định là 8000).

---

## 2. Nhóm Cơ sở dữ liệu Vector (Vector Search)

Thay vì tìm kiếm theo từ khóa thông thường, nhóm này giúp tìm kiếm theo "độ tương đồng" về đặc điểm sản phẩm.

### **sqlite-vec**
- **Vai trò:** Tiện ích mở rộng (extension) tìm kiếm vector cho SQLite.
- **Công dụng:** Cho phép lưu trữ các mảng số (embeddings) từ hình ảnh sản phẩm vào file `auction.db`. Nó hỗ trợ các hàm toán học để tính khoảng cách Cosine giữa các sản phẩm, giúp tìm ra các món đồ "trông giống" nhau nhất.
- **Tại sao chọn:** Tích hợp trực tiếp vào file SQLite hiện có của dự án, không cần cài đặt thêm server Database phức tạp.

---

## 3. Nhóm AI & Deep Learning (Embedding Engine)

Đây là "bộ não" của hệ thống, có nhiệm vụ hiểu nội dung của hình ảnh.

### **Transformers (Hugging Face)**
- **Vai trò:** Thư viện giao tiếp với các mô hình AI tiên tiến.
- **Công dụng:** Dùng để tải và chạy mô hình **SigLIP (Google)**. Nó cung cấp các công cụ để chuyển đổi dữ liệu thô (ảnh, chữ) thành định dạng mà AI hiểu được.

### **Torch (PyTorch) & Torchvision**
- **Vai trò:** Framework Deep Learning nền tảng.
- **Công dụng:** Thực hiện các phép tính ma trận khổng lồ khi mô hình AI xử lý ảnh. `torchvision` cung cấp các thuật toán chuẩn hóa hình ảnh chuyên biệt để tăng độ chính xác cho AI.

### **Pillow (PIL)**
- **Vai trò:** Thư viện xử lý hình ảnh.
- **Công dụng:** Mở, thay đổi kích thước (resize) và định dạng lại các file ảnh gửi từ Java Server trước khi nạp chúng vào mô hình AI.

---

## 4. Nhóm Tiện ích (Utilities)

### **Python-dotenv**
- **Vai trò:** Quản lý cấu hình môi trường.
- **Công dụng:** Đọc file `.env` để lấy các thông số như `DATABASE_URL` hay `API_KEY`. Việc này giúp tách biệt code và các cấu hình nhạy cảm, dễ dàng deploy lên server DigitalOcean sau này.

---

## Tóm tắt luồng hoạt động
1. **Java Client (JavaFX)** gửi ảnh lên **Java Server**.
2. **Java Server** gọi API của **FastAPI (Python)**.
3. **Python** dùng **Pillow** mở ảnh, dùng **SigLIP (Transformers/Torch)** để tạo Vector.
4. **Python** dùng **sqlite-vec** để tìm các sản phẩm tương đồng trong `auction.db`.
5. **FastAPI** trả kết quả JSON về cho **Java Server** để hiển thị lên UI.

---
*Tài liệu được soạn thảo cho dự án Đấu giá trực tuyến - Team 10 - K70 UET.*
