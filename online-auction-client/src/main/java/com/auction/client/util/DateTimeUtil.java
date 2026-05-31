package com.auction.client.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Util format datetime dùng chung toàn client.
 * Controller không nên tự xử lý chuỗi thời gian.
 */
public class DateTimeUtil {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final DateTimeFormatter CHART_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final DateTimeFormatter BID_HISTORY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private DateTimeUtil() {}

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return "—";
        return dateTime.format(DISPLAY_FORMAT);
    }

    /** Dùng cho chart trục X (HH:mm:ss) */
    public static String formatForChart(LocalDateTime dateTime) {
        if (dateTime == null) return "—";
        return dateTime.format(CHART_FORMAT);
    }

    /** Dùng cho bid history row (dd/MM/yyyy HH:mm:ss) */
    public static String formatForBidHistory(LocalDateTime dateTime) {
        if (dateTime == null) return "—";
        return dateTime.format(BID_HISTORY_FORMAT);
    }

}