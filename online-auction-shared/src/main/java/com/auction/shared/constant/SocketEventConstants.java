package com.auction.shared.constant;

public class SocketEventConstants {
    public static final String EVENT_NEW_BID = "NEW_BID";
    public static final String EVENT_AUCTION_EXTENDED = "AUCTION_EXTENDED";
    public static final String EVENT_AUTO_BID_STATUS = "AUTO_BID_STATUS";
    public static final String EVENT_AUTO_BID_CANCELLED = "AUTO_BID_CANCELLED";
    public static final String EVENT_UPDATE_TIME = "UPDATE_TIME";
    // [NEW] Broadcast khi admin ban một sản phẩm.
    // Payload format: { "status": "success", "message": "ITEM_BANNED", "data": { "itemId": "..." } }
    public static final String EVENT_ITEM_BANNED = "ITEM_BANNED";

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_SUCCESS_LOWER = "success";
    public static final String STATUS_FAIL = "fail";

    public static final String STATUS_JOIN_ROOM_SUCCESS = "join_room_success";
    public static final String STATUS_JOIN_ROOM_FAIL = "join_room_fail";
}