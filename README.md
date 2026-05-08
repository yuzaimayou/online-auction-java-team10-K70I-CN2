## 🚀 Hướng dẫn Cài đặt & Khởi chạy (Dành cho Giảng viên chấm bài)

Dự án này sử dụng kiến trúc Web Server hiện đại, bao gồm **Nginx** đóng vai trò Reverse Proxy (phục vụ file tĩnh tốc độ
cao) và **Java HTTP/WebSocket Server** xử lý logic.

Để hệ thống hoạt động chính xác (đặc biệt là tính năng load ảnh sản phẩm và đấu giá Realtime), vui lòng thực hiện 2 bước
khởi chạy sau:

### 1. Khởi động Nginx (Bắt buộc)

Hệ thống sử dụng Nginx ở cổng 80 để phân luồng HTTP và WebSocket.
👉 **[Hướng dẫn cài đặt Nginx chi tiết](docs/nginx/SETUP_GUIDE.md)**
*(File cấu hình nginx mẫu tại [`docs/nginx/nginx-local.conf.example`](docs/nginx/nginx-local.conf.example)).*

### 2. Khởi động Server & Client

- Mở project bằng IntelliJ IDEA / Eclipse.
- Chạy hàm `main` trong file `com.auction.server.MainServer` (Khởi động pool Database và WebSocket).
- Chạy app JavaFX tại file `com.auction.client.MainClient`.