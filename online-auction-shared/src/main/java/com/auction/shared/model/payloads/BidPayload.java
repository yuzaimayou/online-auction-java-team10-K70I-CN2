package com.auction.shared.model.payloads;

/**
 * DTO truyền thông tin bid giữa client ↔ server ↔ broadcast.
 *
 * Lưu ý thiết kế:
 *  - bidTime luôn được server tự sinh (LocalDateTime.now()) tại thời điểm ghi DB,
 *    KHÔNG lấy từ client để tránh giả mạo thời gian.
 *  - Constructor 2-tham-số (itemId, userId, bidPrice) đã bị xóa để ngăn code mới
 *    vô tình set bidTime = now() ở tầng payload thay vì tầng service.
 *  - Tất cả field là bất biến (không có setter) — payload chỉ đọc sau khi tạo.
 */
public class BidPayload {

    private final String itemId;
    private final String userId;
    private final Double bidPrice;
    private final String bidTime;

    /** Constructor mặc định cho deserialization (Jackson / Gson). */
    public BidPayload() {
        this.itemId   = null;
        this.userId   = null;
        this.bidPrice = null;
        this.bidTime  = null;
    }

    /**
     * Constructor đầy đủ — dùng khi server broadcast kết quả bid ra client.
     * bidTime phải được truyền vào từ BidService (LocalDateTime.now().toString()
     * tại thời điểm ghi DB), KHÔNG để tầng này tự sinh.
     */
    public BidPayload(String itemId, String userId, Double bidPrice, String bidTime) {
        this.itemId   = itemId;
        this.userId   = userId;
        this.bidPrice = bidPrice;
        this.bidTime  = bidTime;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getItemId()   { return itemId;   }
    public String getUserId()   { return userId;   }
    public Double getBidPrice() { return bidPrice; }
    public String getBidTime()  { return bidTime;  }
}