package com.auction.shared.exception;

/**
 * Ném khi một lần đặt giá không thỏa mãn điều kiện nghiệp vụ.
 *
 * <p><b>Loại lỗi:</b> Business Error
 *
 * <p><b>ErrorCode liên quan:</b>
 * <ul>
 *   <li>{@link ErrorCode#INVALID_BID}</li>
 *   <li>{@link ErrorCode#BID_PRICE_TOO_LOW}</li>
 *   <li>{@link ErrorCode#INVALID_BID_PARAMETERS}</li>
 *   <li>{@link ErrorCode#SELLER_CANNOT_BID}</li>
 *   <li>{@link ErrorCode#SAME_USER_CONSECUTIVE_BID}</li>
 * </ul>
 *
 * <h3>Cách dùng</h3>
 * <pre>{@code
 * throw InvalidBidException.of(ErrorCode.INVALID_BID);
 * throw InvalidBidException.of(ErrorCode.BID_PRICE_TOO_LOW,
 *         "Giá tối thiểu là " + minRequired + ", bạn đặt " + bidAmount);
 * }</pre>
 */
public class InvalidBidException extends AuctionException {

    private InvalidBidException(ErrorCode errorCode) {
        super(errorCode);
    }

    private InvalidBidException(ErrorCode errorCode, String detailedMessage) {
        super(errorCode, detailedMessage);
    }

    private InvalidBidException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    private InvalidBidException(ErrorCode errorCode, String detailedMessage, Throwable cause) {
        super(errorCode, detailedMessage, cause);
    }

    // -------------------------------------------------------------------------
    // Static factory methods
    // -------------------------------------------------------------------------

    public static InvalidBidException of(ErrorCode errorCode) {
        return new InvalidBidException(errorCode);
    }

    public static InvalidBidException of(ErrorCode errorCode, String detailedMessage) {
        return new InvalidBidException(errorCode, detailedMessage);
    }

    public static InvalidBidException of(ErrorCode errorCode, Throwable cause) {
        return new InvalidBidException(errorCode, cause);
    }

    public static InvalidBidException of(ErrorCode errorCode, String detailedMessage, Throwable cause) {
        return new InvalidBidException(errorCode, detailedMessage, cause);
    }
}