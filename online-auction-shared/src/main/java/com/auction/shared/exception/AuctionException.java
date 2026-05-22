package com.auction.shared.exception;

/**
 * Base exception cho toàn bộ hệ thống đấu giá.
 *
 * <p>Mọi exception cụ thể đều kế thừa class này và KHÔNG cần override constructor –
 * thay vào đó dùng static factory methods {@code of(…)} được cung cấp sẵn tại đây.
 *
 * <p>Kế thừa {@link RuntimeException} để không buộc caller phải khai báo {@code throws}.
 *
 * <h3>Cách sử dụng</h3>
 * <pre>{@code
 * // Ném với ErrorCode mặc định
 * throw InvalidBidException.of(ErrorCode.INVALID_BID);
 *
 * // Ném với message bổ sung
 * throw InvalidBidException.of(ErrorCode.INVALID_BID,
 *         "Giá " + bidAmount + " thấp hơn mức tối thiểu " + minBid);
 *
 * // Wrap một exception gốc
 * throw DataException.of(ErrorCode.DATABASE_ERROR, cause);
 * }</pre>
 */
public abstract class AuctionException extends RuntimeException {

    private final ErrorCode errorCode;

    // -------------------------------------------------------------------------
    // Constructors – protected, dành cho subclass gọi qua super(...)
    // -------------------------------------------------------------------------

    protected AuctionException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected AuctionException(ErrorCode errorCode, String detailedMessage) {
        super(detailedMessage);
        this.errorCode = errorCode;
    }

    protected AuctionException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    protected AuctionException(ErrorCode errorCode, String detailedMessage, Throwable cause) {
        super(detailedMessage, cause);
        this.errorCode = errorCode;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /** Mã lỗi dạng String, dùng trong log và response body. */
    public String getErrorCodeString() {
        return errorCode.getCode();
    }

    /** HTTP status code gợi ý, lấy từ {@link ErrorCode}. */
    public int getHttpStatus() {
        return errorCode.getHttpStatus();
    }

    public boolean isBusinessError() {
        return errorCode.isBusinessError();
    }

    public boolean isSystemError() {
        return errorCode.isSystemError();
    }


    @Override
    public String toString() {
        return String.format("[%s] %s: %s",
                errorCode.getType(),
                errorCode.getCode(),
                getMessage());
    }
}