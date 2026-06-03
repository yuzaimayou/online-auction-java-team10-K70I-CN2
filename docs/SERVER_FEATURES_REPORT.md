# BÁO CÁO KỸ THUẬT: HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN

**Dự án:** Online Auction System – Team 10 (K70I-CN2)
**Module:** `online-auction-server`
**Ngày:** 2026-06-03

---

## I. TỔNG QUAN HỆ THỐNG

### 1.1 Mô tả chung

Server là backend của hệ thống đấu giá trực tuyến, cung cấp REST API và WebSocket để phục vụ các tính năng đấu giá theo thời gian thực. Hệ thống được xây dựng bằng Java thuần (không dùng framework), sử dụng SQLite làm cơ sở dữ liệu.

### 1.2 Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 25 |
| HTTP Server | `com.sun.net.httpserver` (built-in) |
| WebSocket | Java Raw Socket |
| Database | SQLite (xerial sqlite-jdbc) |
| Connection Pool | HikariCP 5.1.0 |
| Serialization | GSON |
| Email | Jakarta Mail 2.0.1 |
| AI Integration | Gemini API (qua Python AI Server) |
| Build Tool | Maven |

### 1.3 Kiến trúc tổng thể

```
┌─────────────────────────────────────────────────┐
│                  CLIENT (Frontend)               │
└────────────┬──────────────────────┬─────────────┘
             │ HTTP REST (port 8080) │ WebSocket (port 9090)
┌────────────▼──────────────────────▼─────────────┐
│              online-auction-server               │
│  ┌──────────────┐      ┌──────────────────────┐  │
│  │ HTTP Handlers│      │   Socket Handlers     │  │
│  └──────┬───────┘      └──────────┬────────────┘  │
│         │                         │               │
│  ┌──────▼─────────────────────────▼────────────┐  │
│  │               Services Layer                │  │
│  │  AuthService | BidService | WalletService   │  │
│  │  ItemService | AuctionSchedulerService      │  │
│  └──────────────────────┬───────────────────────┘  │
│  ┌───────────────────────▼──────────────────────┐  │
│  │             Repository Layer                 │  │
│  │  UserRepo | ItemRepo | BidRepo | WalletRepo  │  │
│  └──────────────────────┬───────────────────────┘  │
│  ┌───────────────────────▼──────────────────────┐  │
│  │          SQLite Database (auction.db)        │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
         │ HTTP
┌────────▼────────┐
│ AI Server :8000 │ (Python – Gemini Integration)
└─────────────────┘
```

---

## II. CÁC CHỨC NĂNG HỆ THỐNG

---

### CHỨC NĂNG 1: ĐĂNG KÝ TÀI KHOẢN

**Endpoint:** `POST /api/register`

**Mô tả:** Cho phép người dùng tạo tài khoản mới. Sau khi đăng ký thành công, hệ thống tự động gửi OTP về email để xác minh tài khoản.

**Request Body:**
```json
{
  "username": "string",
  "password": "string",
  "email": "string"
}
```

**Logic xử lý:**
1. Kiểm tra `username` đã tồn tại chưa
2. Kiểm tra `email` đã tồn tại chưa
3. Tạo user mới với `role=USER`, `status=Active`, `balance=10000` (số dư khởi tạo), `isVerify=false`
4. Gửi OTP qua email bất đồng bộ (async thread)

**Response:**
```json
{ "status": "OK", "message": "Register successful" }
```

**Mã lỗi:**

| Trường hợp | HTTP | Message |
|---|---|---|
| Username đã tồn tại | 409 | `"Username already exists"` |
| Email đã tồn tại | 409 | `"Email already exists"` |
| Lỗi hệ thống | 500 | `"Registration failed"` |

---

### CHỨC NĂNG 2: ĐĂNG NHẬP

**Endpoint:** `POST /api/login`

**Mô tả:** Xác thực người dùng bằng username/password. Trả về thông tin tài khoản khi thành công.

**Request Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Logic xử lý:**
1. Tra cứu user theo `username`
2. So khớp `password`
3. Kiểm tra trạng thái: nếu `status=Suspended` → từ chối đăng nhập
4. Trả về `UserResponseDTO` (id, username, email, role, balance, isVerify,…)

**Response (thành công):**
```json
{
  "status": "OK",
  "data": {
    "id": "uuid",
    "username": "string",
    "email": "string",
    "role": "USER | Admin",
    "balance": 10000.0,
    "frozenBalance": 0.0,
    "isVerify": true
  }
}
```

---

### CHỨC NĂNG 3: XÁC MINH EMAIL (OTP)

**Endpoints:**
- `POST /api/send-otp?email=...` — Gửi lại OTP
- `POST /api/verify-account` — Xác minh OTP

**Mô tả:** Hệ thống hai bước để xác minh địa chỉ email của người dùng.

**Luồng xử lý:**
1. Gửi OTP: Sinh mã 6 chữ số → lưu vào cache (in-memory) → gửi email qua SMTP
2. Xác minh: So khớp `{email, otp}` → cập nhật `isVerify=true`

---

### CHỨC NĂNG 4: QUẢN LÝ SẢN PHẨM ĐẤU GIÁ

#### 4.1 Danh sách sản phẩm

**Endpoint:** `GET /api/items`

**Query Parameters:**

| Tham số | Kiểu | Mô tả |
|---|---|---|
| `sort` | `newest \| price_low \| price_high` | Tiêu chí sắp xếp |
| `category` | string | Lọc theo danh mục |
| `page` | int | Phân trang (10 items/trang) |
| `caller` | `ADMIN` | Hiển thị cả sản phẩm bị cấm |
| `sellerId` | string | Lọc theo người bán |

**Tính năng:**
- Phân trang 10 sản phẩm/trang
- Lọc theo danh mục, sắp xếp theo giá/thời gian
- Admin mode xem được cả sản phẩm `BANNED`
- Mặc định sắp xếp theo `end_time`

#### 4.2 Chi tiết sản phẩm

**Endpoint:** `GET /api/items/{itemId}?userId=...`

Trả về đầy đủ thông tin sản phẩm kèm **giá thầu cuối cùng của người dùng** trong phiên đấu giá đó.

#### 4.3 Tạo sản phẩm

**Endpoint:** `POST /api/items`

**Payload:**

| Trường | Mô tả |
|---|---|
| `name` | Tên sản phẩm |
| `description` | Mô tả |
| `category` | Danh mục |
| `startingPrice` | Giá khởi điểm |
| `bidStep` | Bước giá tối thiểu |
| `startTime` | Thời gian bắt đầu |
| `endTime` | Thời gian kết thúc |
| `sellerId` | ID người bán |
| `imagesPath` | Danh sách ảnh |

#### 4.4 Cập nhật / Xóa sản phẩm

- `PUT /api/items/{itemId}` — Cập nhật thông tin
- `DELETE /api/items/{itemId}` — Xóa sản phẩm

---

### CHỨC NĂNG 5: ĐẶT GIÁ THẦU (BID) – REAL-TIME QUA WEBSOCKET

**Kết nối:** WebSocket tại port `9090`

**Mô tả:** Đây là chức năng cốt lõi của hệ thống. Toàn bộ quá trình đấu giá diễn ra theo thời gian thực qua kết nối socket.

#### 5.1 Các sự kiện WebSocket

| Sự kiện (Action) | Hướng | Mô tả |
|---|---|---|
| `JOIN_ROOM` | Client → Server | Tham gia phòng đấu giá theo `itemId` |
| `LEAVE_ROOM` | Client → Server | Rời phòng đấu giá |
| `BID` | Client → Server | Đặt giá thầu thủ công |
| `AUTO_BID_REGISTER` | Client → Server | Đăng ký đặt giá tự động |
| `CANCEL_AUTO_BID` | Client → Server | Hủy đặt giá tự động |
| `GET_AUTO_BID_STATUS` | Client → Server | Kiểm tra trạng thái auto-bid |
| *(broadcast)* | Server → All Clients | Thông báo giá mới cho cả phòng |
| `ITEM_BANNED` | Server → All Clients | Thông báo sản phẩm bị cấm |

#### 5.2 Luồng đặt giá (BidService)

```
Người dùng gửi BID
        │
        ▼
[1] Lấy lock theo itemId (AuctionLockManager - synchronized)
        │
        ▼
[2] Validate:
    - Item tồn tại & đang diễn ra (ONGOING)?
    - User tồn tại & không bị ban?
    - Giá thầu > current_price + bid_step?
    - User có đủ số dư không?
        │
        ▼
[3] Transaction:
    - Freeze số tiền từ balance → frozen_balance (người mới)
    - Unfreeze số tiền của người thầu trước (nếu bị outbid)
    - Ghi bid vào bảng bids
    - Cập nhật current_price & current_bidder_id của item
        │
        ▼
[4] Anti-Sniping:
    - Nếu còn < 60 giây đến end_time?
    → Gia hạn thêm 60 giây
        │
        ▼
[5] Auto-Bid Resolver:
    - Kiểm tra các auto-bid khác trong phòng
    - Tự động đặt giá phản hồi nếu đủ điều kiện
        │
        ▼
[6] Broadcast kết quả đến tất cả clients trong phòng
        │
        ▼
    Giải phóng lock
```

#### 5.3 Tính năng Auto-Bid (Đặt giá tự động)

Người dùng cấu hình:
- `maxBid`: Giá tối đa sẵn sàng trả
- `increment`: Bước tăng tự động mỗi lần bị outbid

Hệ thống sẽ tự động đặt giá thay người dùng khi có người khác thầu cao hơn, cho đến khi đạt `maxBid`.

#### 5.4 Cơ chế chống Race Condition

- `AuctionLockManager` dùng `ConcurrentHashMap<itemId, Object>` để tạo lock riêng biệt cho từng phiên đấu giá
- Toàn bộ luồng bid được wrapped trong `synchronized(lock)`
- Database transaction đảm bảo tính nhất quán của dữ liệu tài chính

---

### CHỨC NĂNG 6: VÍ ĐIỆN TỬ (WALLET)

**Mô tả:** Mỗi tài khoản có ví điện tử với 2 loại số dư:
- **`balance`**: Số dư khả dụng
- **`frozenBalance`**: Số dư bị tạm giữ (đang tham gia đấu giá)

#### 6.1 Nạp tiền

**Endpoint:** `POST /api/wallet/deposit`

```json
{ "userId": "uuid", "amount": 50000 }
```

- Validate user tồn tại & không bị ban
- Cộng `amount` vào `balance`
- Ghi log giao dịch `DEPOSIT`

#### 6.2 Kiểm tra số dư

**Endpoint:** `GET /api/wallet/balance?userId=...`

```json
{
  "data": {
    "balance": 45000.0,
    "frozenBalance": 5000.0
  }
}
```

#### 6.3 Vòng đời tài chính trong đấu giá

```
Đặt giá       → balance    ↓  |  frozenBalance ↑  (FREEZE_BID)
Bị outbid     → balance    ↑  |  frozenBalance ↓  (UNFREEZE_BID)
Thắng đấu giá → frozenBalance ↓ (AUCTION_PAYMENT) → seller balance ↑
Thua đấu giá  → frozenBalance ↓ (UNFREEZE_BID)
```

Tất cả giao dịch đều được ghi vào bảng `wallet_transactions` để audit.

---

### CHỨC NĂNG 7: TỰ ĐỘNG KẾT THÚC & THANH TOÁN ĐẤU GIÁ

**Service:** `AuctionSchedulerService` + `AuctionSettlementService`

**Mô tả:** Hệ thống tự động quản lý vòng đời của phiên đấu giá.

#### 7.1 Scheduler (chạy mỗi 5 giây)

```
Cron Job (5s interval)
   │
   ├─► Tìm item UPCOMING có start_time đã đến → đổi sang ONGOING
   │
   └─► Tìm item ONGOING có end_time đã qua → gọi Settlement
```

#### 7.2 Quy trình thanh toán (Settlement)

```
Item hết hạn
     │
[1] Kiểm tra idempotency (wallet_transactions có payment chưa?)
     │
[2] Không có bid → Đánh dấu ENDED (không xử lý tài chính)
     │
[3] Có bid:
    - Trừ winning_price từ frozen_balance của người thắng
    - Cộng winning_price vào balance của người bán
    - Ghi log AUCTION_PAYMENT
    - Đánh dấu item ENDED
    - Gửi email chúc mừng người thắng (CongratulationsService)
```

---

### CHỨC NĂNG 8: LỊCH SỬ ĐẤU GIÁ

#### 8.1 Lịch sử đặt giá của một sản phẩm

**Endpoint:** `GET /api/history/{itemId}`

Trả về toàn bộ lịch sử các lượt đặt giá: ai đặt, giá bao nhiêu, thời điểm nào.

#### 8.2 Các phiên đấu giá của người dùng

**Endpoint:** `GET /api/mybids?userId=...`

Trả về danh sách tất cả các phiên đấu giá người dùng đã tham gia, kèm trạng thái thắng/thua.

---

### CHỨC NĂNG 9: QUẢN TRỊ VIÊN (ADMIN)

#### 9.1 Quản lý người dùng

**Endpoints:**
- `GET /api/users` — Lấy toàn bộ danh sách người dùng
- `POST /api/users/ban` — Khóa tài khoản người dùng

**Logic ban user:**
- Không thể tự ban bản thân
- Không thể ban tài khoản Admin khác
- Khi ban: `status=Suspended` + vô hiệu hóa toàn bộ auto-bid của user đó (trong 1 transaction)

#### 9.2 Quản lý sản phẩm

**Endpoint:** `PUT /api/items/ban/{itemId}`

**Logic ban item:**
- Đổi trạng thái sang `BANNED`
- Broadcast sự kiện `ITEM_BANNED` đến tất cả clients đang xem sản phẩm đó qua WebSocket

---

### CHỨC NĂNG 10: CHATBOT AI

**Endpoint:** `POST /api/chatbot`

**Mô tả:** Tích hợp AI (Gemini) để hỗ trợ người dùng thông qua giao diện chat.

**Luồng xử lý:**

```
Người dùng gửi câu hỏi (plain text)
        │
        ▼
  QuestionAnalyzer phân loại intent
        │
   ┌────┴─────────────────────────────────┐
   │            Intent Categories         │
   ▼            ▼            ▼            ▼
APP_SUPPORT  ITEM_ADVICE  CUSTOMER_CARE  OUT_OF_SCOPE
(Hỗ trợ     (Tư vấn      (Chăm sóc     (Ngoài phạm
 ứng dụng)   sản phẩm)    khách hàng)   vi hỗ trợ)
   │            │            │            │
   └────────────┴────────────┴────────────┘
                      │
                 Gọi AI Server (Gemini)
                 tại http://127.0.0.1:8000
                      │
                 Trả về AIResponse
```

---

## III. SƠ ĐỒ DATABASE

```
users          items               bids
─────────      ──────────────      ──────────────
id (PK)        id (PK)             id (PK, AUTO)
username       name                item_id (FK)
password       description         user_id (FK)
email          start_price         bid_price
role           current_price       bid_time
status         seller_id (FK)
balance        current_bidder_id
frozen_balance start_time
isVerify       end_time
               bid_step
               category            auto_bids
               image_path          ────────────────
               status              id (PK, AUTO)
               create_at           item_id (FK)
               search_name         user_id (FK)
                                   max_bid
wallet_transactions                 increment
───────────────────                registered_at
id (PK, AUTO)                      is_active
user_id (FK)                       UNIQUE(item_id, user_id)
type
amount
balance_before
balance_after
reference_id
created_at
```

---

## IV. DANH SÁCH TẤT CẢ ENDPOINTS

### REST API (HTTP – port 8080)

| STT | Method | Path | Mô tả | Phân quyền |
|---|---|---|---|---|
| 1 | POST | `/api/register` | Đăng ký tài khoản | Public |
| 2 | POST | `/api/login` | Đăng nhập | Public |
| 3 | POST | `/api/send-otp` | Gửi OTP xác minh email | Public |
| 4 | POST | `/api/verify-account` | Xác minh OTP | Public |
| 5 | GET | `/api/items` | Danh sách sản phẩm đấu giá | Public |
| 6 | POST | `/api/items` | Tạo sản phẩm mới | User |
| 7 | GET | `/api/items/{itemId}` | Chi tiết sản phẩm | Public |
| 8 | PUT | `/api/items/{itemId}` | Cập nhật sản phẩm | User (owner) |
| 9 | DELETE | `/api/items/{itemId}` | Xóa sản phẩm | User (owner) |
| 10 | PUT | `/api/items/ban/{itemId}` | Cấm sản phẩm | Admin |
| 11 | GET | `/api/mybids` | Lịch sử đấu giá của user | User |
| 12 | GET | `/api/history/{itemId}` | Lịch sử bid của sản phẩm | Public |
| 13 | POST | `/api/wallet/deposit` | Nạp tiền vào ví | User |
| 14 | GET | `/api/wallet/balance` | Kiểm tra số dư ví | User |
| 15 | POST | `/api/auction/settle` | Thanh toán đấu giá | Internal |
| 16 | GET | `/api/users` | Danh sách tất cả user | Admin |
| 17 | POST | `/api/users/ban` | Khóa tài khoản user | Admin |
| 18 | POST | `/api/chatbot` | Chat với AI | User |

### WebSocket (port 9090)

| Sự kiện | Hướng | Mô tả |
|---|---|---|
| `JOIN_ROOM` | Client → Server | Tham gia phòng đấu giá |
| `LEAVE_ROOM` | Client → Server | Rời phòng đấu giá |
| `BID` | Client → Server | Đặt giá thầu thủ công |
| `AUTO_BID_REGISTER` | Client → Server | Đăng ký auto-bid |
| `CANCEL_AUTO_BID` | Client → Server | Hủy auto-bid |
| `GET_AUTO_BID_STATUS` | Client → Server | Trạng thái auto-bid |
| *(bid broadcast)* | Server → Room | Cập nhật giá mới real-time |
| `ITEM_BANNED` | Server → All | Thông báo sản phẩm bị cấm |

---

## V. ĐÁNH GIÁ KỸ THUẬT

### Điểm mạnh

| # | Điểm mạnh | Chi tiết |
|---|---|---|
| 1 | **Anti-sniping** | Tự động gia hạn 60s nếu có bid trong 60s cuối |
| 2 | **Race condition prevention** | Lock per-item với `synchronized` |
| 3 | **Idempotent settlement** | Kiểm tra trước khi thanh toán, tránh thanh toán trùng |
| 4 | **WAL mode SQLite** | Cải thiện concurrency cho database |
| 5 | **Auto-bid system** | Tự động phản hồi bid, nâng trải nghiệm người dùng |
| 6 | **Wallet audit log** | Ghi log toàn bộ giao dịch tài chính |

### Điểm cần cải thiện

| # | Vấn đề | Mức độ | Khuyến nghị |
|---|---|---|---|
| 1 | **Mật khẩu lưu plain text** | Nghiêm trọng | Dùng BCrypt/Argon2 để hash password |
| 2 | **Không có JWT/Session** | Cao | Tích hợp JWT để xác thực request |
| 3 | **userId truyền qua query param** | Trung bình | Dễ giả mạo, cần xác thực token |
| 4 | **Hardcode config** | Thấp | Dùng `application.properties` hoặc `.env` |
| 5 | **Không có CORS config** | Trung bình | Cần cấu hình CORS headers rõ ràng |
| 6 | **SQLite cho production** | Thấp | Nên chuyển sang PostgreSQL/MySQL khi scale |

---

*Báo cáo được tổng hợp từ phân tích toàn bộ source code tại `online-auction-server/src/main/java/com/auction/server/`.*
