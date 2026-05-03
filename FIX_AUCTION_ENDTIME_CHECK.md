# FIX: Kiểm Tra Thời Gian Kết Thúc Phiên Đấu Giá

## **Vấn Đề Phát Hiện**
Hệ thống cho phép người dùng đặt giá (bid) và đăng ký đấu giá tự động (auto-bid) **ngay cả khi phiên đấu giá đã hết thời gian (endTime)**.

## **Nguyên Nhân**
- Hàm `BidService.placeBid()` không kiểm tra xem hiện tại (`now`) có vượt quá `endTime` của item hay không
- Hàm `BidService.registerAutoBid()` cũng không kiểm tra thời gian
- Chỉ `Auction.addBid()` có kiểm tra thời gian thong qua `isActive()`, nhưng hàm này không được gọi trong `BidService`

## **Giải Pháp**
Sửa file: **BidService.java**

### **1. Sửa hàm `registerAutoBid()` (dòng 49-55)**
```java
// [FIX] Kiểm tra xem phiên đấu giá đã hết thời gian chưa
LocalDateTime now = LocalDateTime.now();
if (now.isAfter(item.getEndTime())) {
    conn.rollback();
    System.out.println("Auto-bid registration rejected: auction has ended at " + item.getEndTime());
    return false;
}
```

**Logic:**
- So sánh thời gian hiện tại (`now`) với thời gian kết thúc phiên (`item.getEndTime()`)
- Nếu `now > endTime` → từ chối đăng ký auto-bid
- In ra message và return `false`

---

### **2. Sửa hàm `placeBid()` (dòng 101-107)**
```java
// [FIX] Kiểm tra xem phiên đấu giá đã hết thời gian chưa
LocalDateTime now = LocalDateTime.now();
if (now.isAfter(item.getEndTime())) {
    conn.rollback();
    System.out.println("Bid rejected: auction has ended at " + item.getEndTime() + ", current time: " + now);
    return false;
}
```

**Logic:**
- So sánh thời gian hiện tại (`now`) với thời gian kết thúc phiên (`item.getEndTime()`)
- Nếu `now > endTime` → từ chối bid
- In ra message chi tiết (bao gồm cả endTime và current time) và return `false`

---

## **Quy Trình Kiểm Tra Thứ Tự**

Khi người dùng gọi `placeBid()` hoặc `registerAutoBid()`:

1. **Kiểm tra dữ liệu đầu vào**: itemId, userId không rỗng
2. **Kiểm tra maxBid, increment > 0** (chỉ cho registerAutoBid)
3. **Lấy Item từ database**
4. **Kiểm tra item tồn tại và user không phải seller** ✓
5. **✨ [NEW] Kiểm tra thời gian: `now > endTime`** → Từ chối nếu hết hạn
6. **Kiểm tra giá tối thiểu** (minAllowedPrice, maxBid)
7. **Tạo bid/auto-bid** → Lưu vào database

---

## **Câu Lệnh SQL Liên Quan**
Không cần sửa database. `Item` class đã có các fields:
- `startTime` (LocalDateTime)
- `endTime` (LocalDateTime)
- Getter: `getEndTime()` trả về LocalDateTime

---

## **Test Case**
```
Scenario 1: Bid trước khi hết thời gian → ✅ Được phép
- now = 2026-05-01 10:00:00
- endTime = 2026-05-01 10:30:00
- Kết quả: Bid được chấp nhận

Scenario 2: Bid sau khi hết thời gian → ❌ Bị từ chối
- now = 2026-05-01 10:40:00
- endTime = 2026-05-01 10:30:00
- Kết quả: "Bid rejected: auction has ended at 2026-05-01T10:30, current time: 2026-05-01T10:40"

Scenario 3: Auto-Bid đăng ký sau khi hết thời gian → ❌ Bị từ chối
- now = 2026-05-01 10:40:00
- endTime = 2026-05-01 10:30:00
- Kết quả: "Auto-bid registration rejected: auction has ended at 2026-05-01T10:30"
```

---

## **Chú Ý**
- Sử dụng `LocalDateTime.now()` để lấy thời gian hệ thống hiện tại
- Sử dụng method **`isAfter()`** để so sánh: `now.isAfter(endTime)` có nghĩa `now > endTime`
- Cũng có thể dùng `!now.isBefore(endTime)` nhưng `isAfter()` rõ ràng hơn
- Tất cả được bảo vệ bởi `synchronized` và transaction (`conn.setAutoCommit(false)`)

