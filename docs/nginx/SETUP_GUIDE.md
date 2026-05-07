# 🚀 Hướng dẫn Cài đặt & Cấu hình Nginx (Dành cho Giảng viên/Người chấm bài)

Dự án Online Auction System sử dụng kiến trúc máy chủ phân tán (Web Server & Application Server) để đảm bảo hiệu năng xử
lý Real-time. **Nginx** được sử dụng làm Reverse Proxy với 3 nhiệm vụ chính:

1. Trực tiếp đọc và trả về file tĩnh (Hình ảnh sản phẩm) với tốc độ cao.
2. Phân luồng các request API (HTTP) vào cổng `8080` của Java Server.
3. Duy trì và phân luồng kết nối Đấu giá thời gian thực (WebSocket) vào cổng `9090`.

Để hệ thống hoạt động chính xác và không bị lỗi Load ảnh (404/403), Bạn vui lòng thiết lập Nginx theo các bước sau:

---

## Bước 1: Cài đặt Nginx

Tùy thuộc vào hệ điều hành Bạn đang sử dụng, vui lòng làm theo một trong các cách sau:

### I. Dành cho Windows

1. Tải bản Nginx Mainline (file `.zip`) tại: [https://nginx.org/en/download.html](https://nginx.org/en/download.html)
2. Giải nén thư mục vào ổ C (Ví dụ: `C:\nginx`). *Lưu ý: Không để trong thư mục có dấu tiếng Việt hoặc khoảng trắng.*

### II. Dành cho macOS

Mở Terminal và chạy lệnh thông qua Homebrew:

```bash
brew install nginx
```

### III. Dành cho Ubuntu/Debian

```bash
sudo apt update && sudo apt install nginx
```

### IV. Dành cho Fedora

```bash
sudo dnf install nginx
```

## Bước 2: Cấu hình Nginx

### 1. Xác định vị trí và thiết lập file cấu hình**

Tùy thuộc vào môi trường hệ điều hành đang sử dụng, bạn hãy tìm và mở file cấu hình tương ứng bằng Text Editor (VS Code,
Nano, Vim,...):

* **Trên Windows:** Mở trực tiếp file mặc định tại:
  `C:\nginx\conf\nginx.conf`

* **Trên macOS:** Đường dẫn cài đặt qua Homebrew sẽ khác nhau tùy thuộc vào dòng chip của máy:
    * **Mac Apple Silicon (M1/M2/M3...):** `/opt/homebrew/etc/nginx/nginx.conf`
    * **Mac Intel:** `/usr/local/etc/nginx/nginx.conf`

* **Trên Linux (Fedora / CentOS):**
  Tạo một file cấu hình mới dành riêng cho dự án đấu giá tại thư mục:
  `/etc/nginx/conf.d/auction.conf`

* **Trên Linux (Ubuntu / Debian):**
  Hệ thống này sử dụng cơ chế thư mục khác. Bạn cần xóa file mặc định và tạo file mới:
    1. Xóa file mặc định: `sudo rm /etc/nginx/sites-enabled/default`
    2. Tạo file cấu hình mới: `/etc/nginx/sites-available/auction.conf`
       *(Lưu ý: Sau khi cấu hình xong, bạn sẽ cần tạo symlink từ `sites-available` sang `sites-enabled` để Nginx nhận
       diện).*

### 2. Thêm cấu hình vào file:

Bạn hãy mở file [`nginx-local.conf.example`](docs/nginx/nginx-local.conf.example) đã được chuẩn bị sẵn trong mã nguồn dự
án, copy toàn bộ nội dung và dán vào file cấu hình Nginx vừa mở ở bước trên (lưu ý đặt bên trong block `http { ... }`).

Dưới đây là nội dung file cấu hình tham khảo:

```nginx
server {
    listen 80;
    server_name 127.0.0.1; # Chạy trên máy cá nhân

    # 1. TRẢ ẢNH TỪ THƯ MỤC PROJECT CỦA BẠN
    location /images/ {
        # Đổi đường dẫn này khớp chính xác với thư mục project trên máy bạn
        #vd: /home/ducanh/Project/online-auction-java-team10-K70I-CN2/dataBase/images/
        alias /DUONG_DAN_TUYET_DOI_TOI_THU_MUC_PROJECT/dataBase/images/;


        expires 0; # Ở môi trường dev, tắt cache để sửa ảnh là thấy ngay
        add_header Cache-Control "no-cache";
    }

    # 2. XỬ LÝ WEBSOCKET (CỔNG 9090)
    location /ws/ {
        proxy_pass http://127.0.0.1:9090;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host $host;
    }

    # 3. XỬ LÝ HTTP API LOGIC (CỔNG 8080)
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## Bước 3: Khởi động Nginx

Sau khi đã lưu file cấu hình, chạy lệnh khởi động/nạp lại Nginx:

- **Trên Windows:** Mở Command Prompt, điều hướng đến thư mục Nginx và chạy:
  ```cmd
  cd C:\nginx
  
  start nginx
  ```
- **Trên macOS:**
  ```bash
  brew services restart nginx
  ```
- **Trên Linux:**
  ```bash
  sudo nginx -t

  sudo systemctl restart nginx
  ```
  *(Lưu ý riêng trên Fedora: Nếu gặp lỗi 403 khi load ảnh do SELinux chặn thư mục Home, vui lòng chạy lệnh: sudo
  setsebool -P httpd_enable_homedirs 1)*

## Bước 4: Kiểm tra hoạt động

Sau khi Nginx đã chạy ngầm ở cổng 80:

1. Mở Project bằng IntelliJ IDEA / Eclipse.

2. Chạy hàm main tại MainServer.java để khởi động Database Connection Pool và Backend Server.

3. Chạy hàm main tại MainClient.java (JavaFX) để khởi động giao diện người dùng.

4. (Tùy chọn) Có thể mở nhiều Client cùng lúc để test tính năng đấu giá Realtime cạnh tranh (Auto-bid).