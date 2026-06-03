# Hệ Thống Đấu Giá Trực Tuyến — Hướng Dẫn Khởi Chạy

## Tổng Quan Kiến Trúc

```
┌─────────────────────────────────────────────────────┐
│                   MÁY CHỦ (SERVER)                  │
│                                                     │
│  ┌──────────┐   ┌─────────────┐   ┌─────────────┐  │
│  │  Nginx   │   │ Java Server │   │  AI Server  │  │
│  │ :80      │──▶│ HTTP  :8080 │   │ FastAPI:8000│  │
│  │          │──▶│ Socket:9090 │   │             │  │
│  └──────────┘   └─────────────┘   └─────────────┘  │
└─────────────────────────────────────────────────────┘
          ▲
          │ HTTP / Socket
          │
┌─────────────────┐
│  JavaFX Client  │  (chạy trên máy người dùng)
└─────────────────┘
```

| Thành phần  | Cổng | Giao thức | Mô tả                               |
|-------------|------|-----------|-------------------------------------|
| Nginx       | 80   | HTTP      | Reverse proxy, phục vụ ảnh tĩnh    |
| Java Server | 8080 | HTTP REST | API nghiệp vụ                       |
| Java Server | 9090 | Socket    | Cập nhật đấu giá thời gian thực    |
| AI Server   | 8000 | HTTP REST | Tìm kiếm ngữ nghĩa, gợi ý sản phẩm |

---

## Yêu Cầu Hệ Thống

| Phần mềm | Phiên bản tối thiểu | Ghi chú                      |
|----------|---------------------|------------------------------|
| Java JDK | 25                  | Dùng cho cả server và client |
| Maven    | 3.6+                | Build Java modules           |
| Python   | 3.10+               | Chạy AI server               |
| Nginx    | 1.18+               | Reverse proxy                |

---

## Cấu Trúc Thư Mục

```
online-auction-java-team10-K70I-CN2/
├── online-auction-server/    # Java backend
├── online-auction-client/    # JavaFX desktop client
├── online-auction-ai/        # Python FastAPI AI server
├── online-auction-shared/    # Shared models/DTOs
├── dataBase/
│   ├── auction.db            # SQLite database chính
│   └── images/               # Ảnh sản phẩm (Nginx phục vụ)
├── docs/
│   └── nginx/
│       └── nginx-local.conf.example
└── .env                      # Biến môi trường (API keys)
```

---

## Thứ Tự Khởi Động

Phải khởi động **đúng thứ tự** sau:

```
1. Nginx         (bắt buộc trước tiên)
2. AI Server
3. Java Server
4. Client        (sau khi server đã sẵn sàng)
```

---

## Kịch Bản A — Chạy Local (chỉ trên máy cài server)

### A1. Cài Đặt Nginx

**Ubuntu / Debian**
```bash
sudo apt update && sudo apt install nginx -y
sudo systemctl enable nginx
```

**Fedora / RHEL**
```bash
sudo dnf install nginx -y
sudo systemctl enable nginx
```

**macOS**
```bash
brew install nginx
```

**Windows:** Tải từ https://nginx.org/en/download.html, giải nén và thêm thư mục vào `PATH`.

---

### A2. Cấu Hình Nginx (Local)

Tạo file config (thay đường dẫn `alias` thành đường dẫn thực trên máy bạn):

**Linux/macOS:** lưu vào `/etc/nginx/conf.d/auction.conf`

```nginx
server {
    listen 80;
    server_name 127.0.0.1 localhost;

    # Phục vụ ảnh sản phẩm từ thư mục project
    location /images/ {
        # *** THAY ĐƯỜNG DẪN NÀY ***
        alias /home/ducanh/Project/online-auction-java-team10-K70I-CN2/dataBase/images/;
        expires 0;
        add_header Cache-Control "no-cache";
        autoindex off;
    }

    # WebSocket — Java Socket server (cổng 9090)
    location /ws/ {
        proxy_pass http://127.0.0.1:9090;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host $host;
    }

    # HTTP REST API — Java HTTP server (cổng 8080)
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

Kiểm tra và áp dụng:
```bash
sudo nginx -t          # Kiểm tra cú pháp
sudo systemctl start nginx
```

---

### A3. Khởi Động AI Server

```bash
cd online-auction-ai

# Lần đầu — tạo môi trường ảo và cài thư viện
python -m venv .venv
source .venv/bin/activate        # Windows: .venv\Scripts\activate
pip install -r requirements.txt

# Các lần sau — chỉ cần kích hoạt và chạy
source .venv/bin/activate
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Server sẵn sàng khi thấy:
```
INFO:     Uvicorn running on http://0.0.0.0:8000
```

> Lần đầu chạy sẽ tải model `google/siglip-base-patch16-224` (~400 MB) — cần internet và ít nhất 4 GB RAM.

---

### A4. Khởi Động Java Server

**Cách 1 — IntelliJ IDEA (khuyến nghị):**
Mở project → tìm `MainServer.java` → nhấn **Run**.

**Cách 2 — Maven:**
```bash
# Build từ thư mục gốc
mvn clean package -DskipTests

# Chạy server
java -cp online-auction-server/target/online-auction-server-1.0-jar-with-dependencies.jar \
     com.auction.server.MainServer
```

Server sẵn sàng khi thấy:
```
INFO: HTTP server started on port 8080
INFO: Socket server started on port 9090
INFO: Auction scheduler started.
```

---

### A5. Cấu Hình Và Khởi Động Client

Mở file `online-auction-client/src/main/java/com/auction/client/util/AppConfig.java`, đặt:

```java
public static final String ServerIp = "127.0.0.1";
```

Chạy client:

**IntelliJ IDEA:** tìm `MainClient.java` → **Run**.

**Maven:**
```bash
cd online-auction-client
mvn javafx:run
```

---

## Kịch Bản B — Chạy LAN (nhiều máy dùng chung server)

Trong kịch bản này, **một máy** cài đặt và chạy Nginx + Java Server + AI Server. Các máy khác trong cùng mạng chỉ cần chạy Client.

### B1. Xác Định IP LAN Của Máy Chủ

```bash
# Linux / macOS
hostname -I      # hoặc: ip addr show

# Windows
ipconfig
```

Ghi lại địa chỉ IP LAN, ví dụ: `192.168.1.100`.

---

### B2. Cài Đặt Nginx (như kịch bản A)

---

### B3. Cấu Hình Nginx (LAN)

Tạo file config với `server_name` là IP LAN của máy chủ:

```nginx
server {
    listen 80;
    server_name 192.168.1.100;   # *** THAY BẰNG IP LAN CỦA MÁY CHỦ ***

    location /images/ {
        # *** THAY ĐƯỜNG DẪN NÀY ***
        alias /home/ducanh/Project/online-auction-java-team10-K70I-CN2/dataBase/images/;
        expires 0;
        add_header Cache-Control "no-cache";
        autoindex off;
    }

    location /ws/ {
        proxy_pass http://127.0.0.1:9090;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host $host;
    }

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

Áp dụng:
```bash
sudo nginx -t
sudo systemctl start nginx    # hoặc reload nếu đang chạy
```

Mở cổng firewall trên máy chủ:
```bash
# Fedora / RHEL
sudo firewall-cmd --permanent --add-port=80/tcp && sudo firewall-cmd --reload

# Ubuntu (ufw)
sudo ufw allow 80/tcp
```

---

### B4. Khởi Động AI Server (như kịch bản A3)

---

### B5. Khởi Động Java Server (như kịch bản A4)

---

### B6. Cấu Hình Client Trên Từng Máy Người Dùng

Mở file `online-auction-client/src/main/java/com/auction/client/util/AppConfig.java`, đặt IP LAN của máy chủ:

```java
public static final String ServerIp = "192.168.1.100";  // IP LAN của máy chủ
```

**Quan trọng:** Phải **build lại** client sau khi sửa file này.

Chạy client (như kịch bản A5).

> Mỗi máy muốn sử dụng client cần có Java 25 và Maven, hoặc chạy file JAR đã build sẵn.

---

## Biến Môi Trường

File `.env` ở thư mục gốc — cần có trước khi chạy Java server:

```env
MAIL_USERNAME=...      # SMTP Brevo — gửi email OTP
MAIL_PASSWORD=...      # SMTP password
GEMINI_API_KEY=...     # Google Gemini — chatbot hỗ trợ
```

File `online-auction-ai/.env`:

```env
DATABASE_PATH=data/vector_store.db
MODEL_ID=google/siglip-base-patch16-224
```

---

## Kiểm Tra Hệ Thống

```bash
# Nginx hoạt động
curl http://localhost/

# Java HTTP API
curl http://localhost:8080/api/items

# AI Server
curl http://localhost:8000/docs

# (LAN) từ máy khác
curl http://192.168.1.100/
```

---

## Xử Lý Sự Cố

**Nginx không khởi động:**
```bash
sudo nginx -t                      # Kiểm tra cú pháp config
sudo journalctl -u nginx -n 50    # Xem log lỗi
sudo lsof -i :80                   # Kiểm tra cổng 80 có bị chiếm không
```

**Client không kết nối được server:**
- Kiểm tra `ServerIp` trong `AppConfig.java` đúng chưa và đã build lại chưa.
- Kiểm tra Nginx: `sudo systemctl status nginx`
- Kiểm tra Java server lắng nghe: `ss -tlnp | grep 8080`
- Kịch bản LAN: đảm bảo hai máy cùng mạng và firewall đã mở cổng 80.

**AI Server lỗi tải model:**
- Kiểm tra kết nối internet (lần đầu tải model).
- Kiểm tra RAM còn trống (cần ít nhất 4 GB).
- Kiểm tra Python version: `python --version` (cần ≥ 3.10).

**Java Server lỗi database:**
Kiểm tra file `dataBase/auction.db` tồn tại và có quyền đọc/ghi.
