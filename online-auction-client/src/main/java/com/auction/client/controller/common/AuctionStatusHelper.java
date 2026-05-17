package com.auction.client.controller.common;

import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.ItemSummary;

import java.time.LocalDateTime;

/**
 * Utility tập trung toàn bộ logic phân giải trạng thái đấu giá theo thời gian thực.
 * Dùng chung cho mọi controller — không viết lại logic này ở nơi khác.
 *
 * Hai nhóm API:
 *   - resolveEnum(...)      → AuctionStatus  (HomePageController, switch enum)
 *   - resolve(...)          → String         (TableHelper, label UI)
 *   - resolveUpperCase(...) → String uppercase (ItemPageController switch)
 */
public final class AuctionStatusHelper {

    // String constants — dùng cho UI labels
    public static final String UPCOMING = "Upcoming";
    public static final String ONGOING  = "Ongoing";
    public static final String ENDED    = "Ended";

    private AuctionStatusHelper() {}

    // ── Core logic (private)

    private static AuctionStatus computeEnum(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) return AuctionStatus.ENDED;
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) return AuctionStatus.UPCOMING;
        if (!now.isBefore(endTime))  return AuctionStatus.ENDED;
        return AuctionStatus.ONGOING;
    }

    /**
     * Dùng cho HomePageController và bất kỳ nơi nào cần switch trên enum.
     */
    public static AuctionStatus resolveEnum(ItemSummary item) {
        if (item == null) return AuctionStatus.ENDED;
        return computeEnum(item.getStartTime(), item.getEndTime());
    }

    /**
     * Overload tiện lợi khi chỉ có startTime / endTime (không có ItemSummary).
     */
    public static AuctionStatus resolveEnum(LocalDateTime startTime, LocalDateTime endTime) {
        return computeEnum(startTime, endTime);
    }

    /**
     * Dùng cho MyAuctionsTableHelper — trả về "Upcoming" / "Ongoing" / "Ended"
     * để gắn CSS class hoặc hiển thị label UI.
     */
    public static String resolve(ItemSummary item) {
        return toDisplayString(resolveEnum(item));
    }

    /**
     * Dùng cho ItemPageController — trả về "UPCOMING" / "ONGOING" / "ENDED"
     * để dùng với switch / equalsIgnoreCase.
     */
    public static String resolveUpperCase(LocalDateTime startTime, LocalDateTime endTime) {
        return computeEnum(startTime, endTime).name(); // enum.name() trả về uppercase
    }


    /**
     * Chuyển enum → display string (Title case).
     * Dùng khi đã có enum trong tay nhưng cần hiển thị label.
     */
    public static String toDisplayString(AuctionStatus status) {
        if (status == null) return ENDED;
        return switch (status) {
            case UPCOMING -> UPCOMING;
            case ONGOING  -> ONGOING;
            case ENDED    -> ENDED;
            case PAID -> null;
            case CANCELED -> null;
        };
    }
}