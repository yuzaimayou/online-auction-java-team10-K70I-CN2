package com.auction.server.service;

import com.auction.server.repository.ItemRepository;

import java.sql.Date;

public class ProductService {
    private final ItemRepository itemRepository;

    public ProductService() {
        this.itemRepository = new ItemRepository();
    }

    public boolean addProduct(String productName, String category, Date startTime, Date endTime) {
//        Item newItem = new Item();
//        boolean result = itemRepository.createItem(newItem);
//        if (result) {
//            System.out.println("Created product successfully!");
//        }
        return false;
    }
}
