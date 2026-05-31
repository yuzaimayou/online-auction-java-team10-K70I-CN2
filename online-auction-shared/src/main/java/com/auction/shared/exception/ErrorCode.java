package com.auction.shared.exception;

/**
 * Enum định nghĩa toàn bộ error code trong hệ thống đấu giá.
 *
 * <p>Mỗi ErrorCode mang theo:
 * <ul>
 *   <li>{@code code}       – mã định danh duy nhất (dùng trong log, response body)</li>
 *   <li>{@code message}    – thông điệp mặc định (tiếng Việt, hiển thị cho end-user)</li>
 *   <li>{@code type}       – phân loại BUSINESS / SYSTEM (không phụ thuộc vào ordinal)</li>
 *   <li>{@code httpStatus} – HTTP status code gợi ý khi map sang REST response</li>
 * </ul>
 *
 * <p><b>Quy ước đặt tên prefix:</b>
 * <pre>
 *   BID_xxx      → lỗi liên quan đến đặt giá
 *   AUCTION_xxx  → lỗi liên quan đến phiên đấu giá
 *   ITEM_xxx     → lỗi liên quan đến sản phẩm
 *   WALLET_xxx   → lỗi liên quan đến ví
 *   AUTO_BID_xxx → lỗi liên quan đến auto-bid
 *   INPUT_xxx    → lỗi validate đầu vào
 *   DB_xxx       → lỗi database
 *   CONN_xxx     → lỗi kết nối
 *   DATA_xxx     → lỗi toàn vẹn dữ liệu
 *   ERR_xxx      → lỗi hệ thống chung
 * </pre>
 */
public enum ErrorCode {

    // BUSINESS ERRORS – lỗi do logic nghiệp vụ, client có thể xử lý được
    INVALID_BID(
            "BID_001",
            "Giá đặt phải lớn hơn giá hiện tại cộng với bidStep",
            ErrorType.BUSINESS, 422),

    BID_PRICE_TOO_LOW(
            "BID_002",
            "Giá đặt không đạt yêu cầu tối thiểu",
            ErrorType.BUSINESS, 422),

    INVALID_BID_PARAMETERS(
            "BID_003",
            "Tham số bid không hợp lệ (maxBid ≤ 0 hoặc increment ≤ 0)",
            ErrorType.BUSINESS, 400),

    SELLER_CANNOT_BID(
            "BID_004",
            "Người bán không thể đặt giá cho sản phẩm của chính mình",
            ErrorType.BUSINESS, 403),

    SAME_USER_CONSECUTIVE_BID(
            "BID_005",
            "Không thể đặt giá liên tiếp khi đang là người trả giá cao nhất",
            ErrorType.BUSINESS, 422),

    AUCTION_ALREADY_CLOSED(
            "AUCTION_001",
            "Phiên đấu giá đã kết thúc",
            ErrorType.BUSINESS, 409),

    AUCTION_NOT_STARTED(
            "AUCTION_002",
            "Phiên đấu giá chưa bắt đầu",
            ErrorType.BUSINESS, 409),

    ITEM_NOT_FOUND(
            "ITEM_001",
            "Không tìm thấy sản phẩm",
            ErrorType.BUSINESS, 404),

    INSUFFICIENT_BALANCE(
            "WALLET_001",
            "Số dư tài khoản không đủ để thực hiện giao dịch",
            ErrorType.BUSINESS, 422),

    INVALID_INPUT(
            "INPUT_001",
            "Thông tin đầu vào không hợp lệ",
            ErrorType.BUSINESS, 400),

    // SYSTEM ERRORS – lỗi hạ tầng / nội bộ, client không xử lý được

    DATABASE_ERROR(
            "DB_001",
            "Lỗi cơ sở dữ liệu",
            ErrorType.SYSTEM, 503),

    CONNECTION_ERROR(
            "CONN_001",
            "Lỗi kết nối",
            ErrorType.SYSTEM, 503),

    DATA_INTEGRITY_ERROR(
            "DATA_001",
            "Lỗi toàn vẹn dữ liệu",
            ErrorType.SYSTEM, 500),

    WALLET_OPERATION_FAILED(
            "WALLET_002",
            "Không thể thực hiện thao tác ví",
            ErrorType.SYSTEM, 500),

    FAILED_TO_CREATE_BID_RECORD(
            "BID_006",
            "Không thể tạo bản ghi bid trong cơ sở dữ liệu",
            ErrorType.SYSTEM, 500),

    FAILED_TO_UPDATE_BIDDER(
            "BID_007",
            "Không thể cập nhật thông tin người đặt giá",
            ErrorType.SYSTEM, 500),

    FAILED_TO_PERSIST_AUTO_BID(
            "AUTO_BID_001",
            "Không thể lưu cấu hình auto-bid",
            ErrorType.SYSTEM, 500),

    FAILED_TO_EXTEND_AUCTION(
            "AUCTION_003",
            "Không thể kéo dài thời gian phiên đấu giá",
            ErrorType.SYSTEM, 500),

    UNEXPECTED_ERROR(
            "ERR_999",
            "Lỗi không mong muốn trong hệ thống",
            ErrorType.SYSTEM, 500);

    // -------------------------------------------------------------------------

    private final String code;
    private final String message;
    private final ErrorType type;
    private final int httpStatus;

    ErrorCode(String code, String message, ErrorType type, int httpStatus) {
        this.code = code;
        this.message = message;
        this.type = type;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public ErrorType getType() {
        return type;
    }

    /** HTTP status code gợi ý khi map sang REST response. */
    public int getHttpStatus() {
        return httpStatus;
    }

    /** @return true nếu đây là lỗi nghiệp vụ mà client có thể xử lý. */
    public boolean isBusinessError() {
        return type == ErrorType.BUSINESS;
    }

    /** @return true nếu đây là lỗi hệ thống nội bộ. */
    public boolean isSystemError() {
        return type == ErrorType.SYSTEM;
    }
}