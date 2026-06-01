package com.auction.shared.model.account;

import com.auction.shared.model.item.Item;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountTestSupport {

    static User user(String id, String username, String password) {
        return new User(id, username, password);
    }

    static Admin admin(String id, String username, String password) {
        return new Admin(id, username, password);
    }

    static Item activeItemFor(String currentUserId, double highestPrice) {
        Item item = mock(Item.class);
        when(item.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(10));
        when(item.getEndTime()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(item.isOwner(currentUserId)).thenReturn(false);
        when(item.getHighestCurrentPrice()).thenReturn(highestPrice);
        return item;
    }
}

