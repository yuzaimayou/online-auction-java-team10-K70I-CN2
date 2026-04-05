package com.auction.server.service;

import com.auction.server.repository.ItemRepository;
import com.auction.shared.model.payloads.ProductPayload;
import com.auction.shared.util.GsonUtil;
import com.auction.shared.util.ImageUtil;
import com.google.gson.Gson;

import java.time.LocalDateTime;

public class ProductService {
    private final ItemRepository itemRepository;
    private Gson gson = GsonUtil.getInstance();

    public ProductService() {
        this.itemRepository = new ItemRepository();
    }

    public boolean addProduct(String payload) {
        ProductPayload productData = gson.fromJson(payload, ProductPayload.class);
        String productName = productData.getProductName();
        String category = productData.getCategory();
        LocalDateTime startTime = productData.getStartDateTime();
        LocalDateTime endTime = productData.getEndDateTime();
        String productDesc = productData.getProductDesc();
        String[] productImg = productData.getProductImg();
        Double initPrice = productData.getInitPrice();
        Double bidStep = productData.getBidStep();
        Double maxPrice = productData.getMaxPrice();
        Double minPrice = productData.getMinPrice();
        String userId = productData.getUserId();

        String imagePath = ImageUtil.convertBase64ToImg(productImg[0], productImg[1]);
//        Item newItem = new Item();
//        boolean result = itemRepository.createItem(newItem);
//        if (result) {
//            System.out.println("Created product successfully!");
//        }
        return true;
    }
}
