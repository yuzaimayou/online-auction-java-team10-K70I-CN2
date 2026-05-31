package com.auction.shared.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidHistoryItemDTOTest {

    @Test
    void shouldExposeConstructorValues() {
        LocalDateTime bidTime = LocalDateTime.of(2026, 5, 31, 10, 15, 30);
        BidHistoryItemDTO dto = new BidHistoryItemDTO("item-1", "alice", 1234.5, bidTime);

        assertAll(
                () -> assertEquals("item-1", dto.itemId),
                () -> assertEquals("alice", dto.userName),
                () -> assertEquals(1234.5, dto.bidPrice),
                () -> assertEquals(bidTime, dto.bidTime)
        );
    }
}

