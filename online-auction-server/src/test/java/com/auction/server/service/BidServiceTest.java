package com.auction.server.service;


import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.shared.model.item.Item;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BidServiceTest {

    @Test
    void should_reject_bid_when_same_user_bids_twice() throws Exception {
        BidRepository bidRepo = mock(BidRepository.class);
        ItemRepository itemRepo = mock(ItemRepository.class);
        AutoBidResolver resolver = new AutoBidResolver(0.000001);

        BidService service = new BidService(bidRepo, itemRepo, resolver);

        Item item = mock(Item.class);
        when(item.getSellerId()).thenReturn("seller1");
        when(item.getEndTime()).thenReturn(LocalDateTime.now().plusHours(1));
        when(item.getHighestCurrentPrice()).thenReturn(1000.0);
        when(item.getBidStep()).thenReturn(50.0);

        when(itemRepo.findById("item1")).thenReturn(item);

        Connection conn = mock(Connection.class);
        when(bidRepo.findLastBidder(conn, "item1")).thenReturn("user1");

        // Nếu bạn refactor thêm: cho phép truyền conn vào placeBid để mock
        // hoặc mock DatabaseManager.getConnection bằng wrapper.

        // Ví dụ giả định placeBid đã nhận conn (sau refactor):
        // boolean result = service.placeBid(conn, "item1", "user1", 1100, null);
        // assertFalse(result);
    }
}