package com.auction.shared.exception;

/**
 * Ném khi xảy ra lỗi kết nối cơ sở dữ liệu hoặc mạng.
 *
 * <p><b>Loại lỗi:</b> System Error
 *
 * <p><b>ErrorCode liên quan:</b>
 * <ul>
 *   <li>{@link ErrorCode#CONNECTION_ERROR}</li>
 * </ul>
 *
 * <h3>Cách dùng</h3>
 * <pre>{@code
 * throw ConnectionException.of(ErrorCode.CONNECTION_ERROR, cause);
 * throw ConnectionException.of(ErrorCode.CONNECTION_ERROR,
 *         "Không thể kết nối Redis tại " + host, cause);
 * }</pre>
 */
public class ConnectionException extends AuctionException {

    private ConnectionException(ErrorCode errorCode) {
        super(errorCode);
    }

    private ConnectionException(ErrorCode errorCode, String detailedMessage) {
        super(errorCode, detailedMessage);
    }

    private ConnectionException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    private ConnectionException(ErrorCode errorCode, String detailedMessage, Throwable cause) {
        super(errorCode, detailedMessage, cause);
    }

    // -------------------------------------------------------------------------
    // Static factory methods
    // -------------------------------------------------------------------------

    public static ConnectionException of(ErrorCode errorCode) {
        return new ConnectionException(errorCode);
    }

    public static ConnectionException of(ErrorCode errorCode, String detailedMessage) {
        return new ConnectionException(errorCode, detailedMessage);
    }

    public static ConnectionException of(ErrorCode errorCode, Throwable cause) {
        return new ConnectionException(errorCode, cause);
    }

    public static ConnectionException of(ErrorCode errorCode, String detailedMessage, Throwable cause) {
        return new ConnectionException(errorCode, detailedMessage, cause);
    }
}