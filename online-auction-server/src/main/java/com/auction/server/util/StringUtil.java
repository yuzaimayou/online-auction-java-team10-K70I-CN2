package com.auction.server.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class StringUtil {
    public static String removeAccents(String str) {
        if (str == null) return "";
        // Chuyển đổi các ký tự có dấu thành ký tự cơ bản + dấu rời rạc
        String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

        // Loại bỏ các dấu rời rạc, xử lý riêng chữ Đ và ép về chữ thường
        return pattern.matcher(temp)
                .replaceAll("")
                .replace('đ', 'd').replace('Đ', 'd')
                .toLowerCase();
    }
}
