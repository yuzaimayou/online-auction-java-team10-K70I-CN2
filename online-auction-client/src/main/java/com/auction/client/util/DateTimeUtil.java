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
    private DateTimeUtil() {}

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return "—";
        return dateTime.format(DISPLAY_FORMAT);
    }
}