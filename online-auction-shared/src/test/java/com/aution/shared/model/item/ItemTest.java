package com.aution.shared.model.item;

import com.auction.shared.model.item.Item;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemTest {

    @Test
    void shouldRejectPastStartTime() {
        LocalDateTime past = LocalDateTime.now().minusHours(1);
        LocalDateTime future = LocalDateTime.now().plusHours(2);

        assertThrows(IllegalArgumentException.class, () ->
                new Item("Test", "Desc", 1000, past, future,
                        "seller", "Cat", 50, List.of("img.png")));
    }

    @Test
    void shouldRejectBidStepGreaterThanPrice() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        assertThrows(IllegalArgumentException.class, () ->
                new Item("Test", "Desc", 1000, start, end,
                        "seller", "Cat", 2000, List.of("img.png")));
    }
}