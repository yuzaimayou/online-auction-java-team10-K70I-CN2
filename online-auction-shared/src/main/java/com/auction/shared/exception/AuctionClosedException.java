package com.auction.shared.exception;

/**
 * Ném khi phiên đấu giá đã kết thúc hoặc chưa bắt đầu.
 *
 * <p><b>Loại lỗi:</b> Business Error
 *
 * <p><b>ErrorCode liên quan:</b>
 * <ul>
 *   <li>{@link ErrorCode#AUCTION_ALREADY_CLOSED}</li>
 *   <li>{@link ErrorCode#AUCTION_NOT_STARTED}</li>
 * </ul>
 *
 * <h3>Cách dùng</h3>
 * <pre>{@code
 * throw AuctionClosedException.of(ErrorCode.AUCTION_ALREADY_CLOSED);
 * throw AuctionClosedException.of(ErrorCode.AUCTION_NOT_STARTED,
 *         "Phiên #" + auctionId + " bắt đầu lúc " + startTime);
 * }</pre>
 */
public class AuctionClosedException extends AuctionException {

    private AuctionClosedException(ErrorCode errorCode) {
        super(errorCode);
    }

    private AuctionClosedException(ErrorCode errorCode, String detailedMessage) {
        super(errorCode, detailedMessage);
    }

    private AuctionClosedException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    private AuctionClosedException(ErrorCode errorCode, String detailedMessage, Throwable cause) {
        super(errorCode, detailedMessage, cause);
    }

    // -------------------------------------------------------------------------
    // Static factory methods – API công khai duy nhất
    // -------------------------------------------------------------------------

    public static AuctionClosedException of(ErrorCode errorCode) {
        return new AuctionClosedException(errorCode);
    }

    public static AuctionClosedException of(ErrorCode errorCode, String detailedMessage) {
        return new AuctionClosedException(errorCode, detailedMessage);
    }

    public static AuctionClosedException of(ErrorCode errorCode, Throwable cause) {
        return new AuctionClosedException(errorCode, cause);
    }

    public static AuctionClosedException of(ErrorCode errorCode, String detailedMessage, Throwable cause) {
        return new AuctionClosedException(errorCode, detailedMessage, cause);
    }
}