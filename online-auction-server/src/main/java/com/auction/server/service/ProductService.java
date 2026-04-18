package com.auction.server.service;

import com.auction.server.repository.ItemRepository;
import com.auction.shared.model.payloads.ProductPayload;
import com.auction.shared.model.product.Item;
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

    public boolean addProduct(ProductPayload productData) {
        String productName = productData.getProductName();
        String category = productData.getCategory();
        LocalDateTime startTime = productData.getStartDateTime();
        LocalDateTime endTime = productData.getEndDateTime();
        String productDesc = productData.getProductDesc();
        String[] productImg = productData.getProductImg();
        Double initPrice = productData.getInitPrice();
        Double bidStep = productData.getBidStep();


        String userId = productData.getUserId();

        String imagePath = ImageUtil.convertBase64ToImg(productImg[0], productImg[1]);
        Item newItem = new Item(
                productName,
                productDesc,
                initPrice,
                startTime,
                endTime,
                userId,
                category,
                bidStep,
                imagePath
        );

        return itemRepository.createItem(newItem);
    }
}
