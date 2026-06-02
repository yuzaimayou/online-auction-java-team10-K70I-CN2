package com.auction.server.service.bid;

import com.auction.shared.exception.AuctionClosedException;
import com.auction.shared.exception.ErrorCode;
import com.auction.shared.exception.InvalidBidException;
import com.auction.shared.model.account.User;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;

import java.time.LocalDateTime;

/**
 * Tập trung toàn bộ validate nghiệp vụ cho bid và auto-bid.
 * Không phụ thuộc DB — chỉ làm việc với object đã được load.
 */
public class BidValidator {
    private final double epsilon;

    public BidValidator(double epsilon) {
        this.epsilon = epsilon;
    }

    /**
     * Validate cho manual bid.
     */
    public void validateForManualBid(Item item, User user, String userId, double bidPrice)
            throws InvalidBidException, AuctionClosedException {

        validateItemAndUser(item, user, userId);
        validateStatus(item);
        validateAuctionTime(item);

        double minAllowed = item.getHighestCurrentPrice() + item.getBidStep();
        if (bidPrice + epsilon < minAllowed) {
            throw InvalidBidException.of(ErrorCode.BID_PRICE_TOO_LOW,
                    String.format("Giá bid %.2f thấp hơn tối thiểu %.2f", bidPrice, minAllowed));
        }
    }

    /**
     * Validate cho auto-bid registration.
     */
    public void validateForAutoBid(Item item, User user, String userId,
                                   double maxBid, double increment)
            throws InvalidBidException, AuctionClosedException {

        validateItemAndUser(item, user, userId);
        validateStatus(item);
        validateAuctionTime(item);

        if (increment + epsilon < item.getBidStep()) {
            throw InvalidBidException.of(ErrorCode.INVALID_BID_PARAMETERS,
                    String.format("Increment %.2f thấp hơn bidStep %.2f", increment, item.getBidStep()));
        }
    }

    // Helpers

    private void validateItemAndUser(Item item, User user, String userId)
            throws InvalidBidException {

        if (item.getSellerId().equals(userId)) {
            throw InvalidBidException.of(ErrorCode.SELLER_CANNOT_BID,
                    "Người bán không được tự đặt giá - itemId=" + item.getId());
        }
        if (user != null && "Suspended".equalsIgnoreCase(user.getStatus())) {
            throw InvalidBidException.of(ErrorCode.INVALID_INPUT,
                    "User bị cấm không được đặt giá - userId=" + userId);
        }
    }

    private void validateStatus(Item item) throws InvalidBidException {
        AuctionStatus stored = item.getStoredStatus(); // AuctionStatus.BANNED...
        AuctionStatus computed = item.getStatus();

        if (stored == AuctionStatus.BANNED
                || stored == AuctionStatus.ENDED
                || computed != AuctionStatus.ONGOING) {
            throw InvalidBidException.of(ErrorCode.INVALID_BID,
                    "Item không ở trạng thái đấu giá hợp lệ - itemId=" + item.getId());
        }
    }

    private void validateAuctionTime(Item item) throws AuctionClosedException {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(item.getEndTime())) {
            throw AuctionClosedException.of(ErrorCode.AUCTION_ALREADY_CLOSED,
                    "Phiên đấu giá đã kết thúc lúc " + item.getEndTime());
        }
        if (now.isBefore(item.getStartTime())) {
            throw AuctionClosedException.of(ErrorCode.AUCTION_NOT_STARTED,
                    "Phiên đấu giá chưa bắt đầu, sẽ bắt đầu lúc " + item.getStartTime());
        }
    }
}
