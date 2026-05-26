package com.auction.shared.exception;

/**
 * Ném khi xảy ra lỗi tầng dữ liệu: không tìm thấy record, vi phạm ràng buộc, v.v.
 *
 * <p><b>Loại lỗi:</b> System Error
 *
 * <p><b>ErrorCode liên quan:</b>
 * <ul>
 *   <li>{@link ErrorCode#DATABASE_ERROR}</li>
 *   <li>{@link ErrorCode#DATA_INTEGRITY_ERROR}</li>
 *   <li>{@link ErrorCode#FAILED_TO_CREATE_BID_RECORD}</li>
 *   <li>{@link ErrorCode#FAILED_TO_UPDATE_BIDDER}</li>
 *   <li>{@link ErrorCode#FAILED_TO_PERSIST_AUTO_BID}</li>
 *   <li>{@link ErrorCode#FAILED_TO_EXTEND_AUCTION}</li>
 * </ul>
 *
 * <h3>Cách dùng</h3>
 * <pre>{@code
 * throw DataException.of(ErrorCode.DATABASE_ERROR, cause);
 * throw DataException.of(ErrorCode.FAILED_TO_CREATE_BID_RECORD,
 *         "auctionId=" + auctionId + ", userId=" + userId, cause);
 * }</pre>
 */
public class DataException extends AuctionException {

    private DataException(ErrorCode errorCode) {
        super(errorCode);
    }

    private DataException(ErrorCode errorCode, String detailedMessage) {
        super(errorCode, detailedMessage);
    }

    private DataException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    private DataException(ErrorCode errorCode, String detailedMessage, Throwable cause) {
        super(errorCode, detailedMessage, cause);
    }

    // Static factory methods

    public static DataException of(ErrorCode errorCode) {
        return new DataException(errorCode);
    }

    public static DataException of(ErrorCode errorCode, String detailedMessage) {
        return new DataException(errorCode, detailedMessage);
    }

    public static DataException of(ErrorCode errorCode, Throwable cause) {
        return new DataException(errorCode, cause);
    }

    public static DataException of(ErrorCode errorCode, String detailedMessage, Throwable cause) {
        return new DataException(errorCode, detailedMessage, cause);
    }
}