package com.auction.shared.model.payloads;

import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.util.List;

final class PayloadTestSupport {
    private PayloadTestSupport() {}

    static Gson gson() {
        return new Gson();
    }

    static AuthPayload authPayload() {
        return new AuthPayload("user", "secret", "user@example.com");
    }

    static RoomPayload roomPayload() {
        return new RoomPayload("item-1", "token-abc");
    }

    static VerifyPayload verifyPayload() {
        return new VerifyPayload("user@example.com", "123456");
    }

    static AuctionExtendedPayload auctionExtendedPayload() {
        return new AuctionExtendedPayload("item-2", "2026-06-01T12:00:00");
    }

    static AutoBidPayload autoBidPayload() {
        return new AutoBidPayload("item-3", "user-2", 5000.0, 100.0, true);
    }

    static BidPayload bidPayload() {
        return new BidPayload("item-4", "user-3", 1234.5, "2026-05-31T10:10:10");
    }

    static ItemPayload itemPayload() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 31, 10, 0);
        LocalDateTime end = start.plusHours(2);
        List<String[]> images = List.<String[]>of(new String[]{"cover.png", "image/png"});
        return new ItemPayload("Laptop", "Electronics", "Gaming laptop", images, start, end, 1000.0, 50.0, "seller-1");
    }
}


