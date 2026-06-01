package com.auction.shared.model.payloads;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PayloadSerializationTest {

    @Test
    void shouldRoundTripSimplePayloadsThroughGson() {
        var gson = PayloadTestSupport.gson();
        AuthPayload auth = PayloadTestSupport.authPayload();
        RoomPayload room = PayloadTestSupport.roomPayload();
        VerifyPayload verify = PayloadTestSupport.verifyPayload();
        AuctionExtendedPayload extended = PayloadTestSupport.auctionExtendedPayload();
        AutoBidPayload autoBid = PayloadTestSupport.autoBidPayload();
        BidPayload bid = PayloadTestSupport.bidPayload();

        AuthPayload authCopy = gson.fromJson(gson.toJson(auth), AuthPayload.class);
        RoomPayload roomCopy = gson.fromJson(gson.toJson(room), RoomPayload.class);
        VerifyPayload verifyCopy = gson.fromJson(gson.toJson(verify), VerifyPayload.class);
        AuctionExtendedPayload extendedCopy = gson.fromJson(gson.toJson(extended), AuctionExtendedPayload.class);
        AutoBidPayload autoBidCopy = gson.fromJson(gson.toJson(autoBid), AutoBidPayload.class);
        BidPayload bidCopy = gson.fromJson(gson.toJson(bid), BidPayload.class);

        assertAll(
                () -> assertEquals(auth.getUsername(), authCopy.getUsername()),
                () -> assertEquals(auth.getPassword(), authCopy.getPassword()),
                () -> assertEquals(auth.getEmail(), authCopy.getEmail()),
                () -> assertEquals(room.getItemId(), roomCopy.getItemId()),
                () -> assertEquals(room.getToken(), roomCopy.getToken()),
                () -> assertEquals(verify.getEmail(), verifyCopy.getEmail()),
                () -> assertEquals(verify.getOtp(), verifyCopy.getOtp()),
                () -> assertEquals(extended.getItemId(), extendedCopy.getItemId()),
                () -> assertEquals(extended.getNewEndTime(), extendedCopy.getNewEndTime()),
                () -> assertEquals(autoBid.getItemId(), autoBidCopy.getItemId()),
                () -> assertEquals(autoBid.getUserId(), autoBidCopy.getUserId()),
                () -> assertEquals(autoBid.getMaxBid(), autoBidCopy.getMaxBid()),
                () -> assertEquals(autoBid.getIncrement(), autoBidCopy.getIncrement()),
                () -> assertEquals(autoBid.getIsActive(), autoBidCopy.getIsActive()),
                () -> assertEquals(bid.getItemId(), bidCopy.getItemId()),
                () -> assertEquals(bid.getUserId(), bidCopy.getUserId()),
                () -> assertEquals(bid.getBidPrice(), bidCopy.getBidPrice()),
                () -> assertEquals(bid.getBidTime(), bidCopy.getBidTime())
        );
    }
}

