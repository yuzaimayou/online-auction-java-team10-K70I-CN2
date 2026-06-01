package com.auction.server.service.auction;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AuctionLockManager {
    private static final ConcurrentMap<String, Object> ITEM_LOCKS = new ConcurrentHashMap<>();

    private AuctionLockManager() {}

    public static Object getItemLock(String itemId) {
        return ITEM_LOCKS.computeIfAbsent(itemId, ignored -> new Object());
    }
}
