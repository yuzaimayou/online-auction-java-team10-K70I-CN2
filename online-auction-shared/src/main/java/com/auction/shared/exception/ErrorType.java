package com.auction.shared.exception;

/**
 * Phân loại error theo tính chất xử lý.
 *   <li>{@link #BUSINESS} – lỗi nghiệp vụ, có thể hiển thị thông báo cho người dùng</li>
 *   <li>{@link #SYSTEM}   – lỗi hạ tầng / kỹ thuật, cần alert kỹ thuật viên</li>
 */
public enum ErrorType {
    BUSINESS,
    SYSTEM
}