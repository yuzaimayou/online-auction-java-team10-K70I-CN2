package com.auction.server.service;

import com.auction.server.repository.ItemRepository;
import com.auction.shared.model.payloads.ItemPayload;
import com.auction.shared.model.item.Item;
import com.auction.shared.util.GsonUtil;
import com.auction.shared.util.ImageUtil;
import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemService {
    private final ItemRepository itemRepository;
    private Gson gson = GsonUtil.getInstance();

    public ItemService() {

        this.itemRepository = new ItemRepository();
    }

    public List<Item> getAllProductsBySeller(String sellerId) {
        if (sellerId == null || sellerId.trim().isEmpty()) {
            return null; // Hoặc trả về list rỗng tùy logic của bạn
        }
        // Gọi hàm từ ItemRepository
        return itemRepository.findAllBySellerId(sellerId);
    }


    public Item setItem(ItemPayload productData) {
        String productName = productData.getProductName();
        String category = productData.getCategory();
        LocalDateTime startTime = productData.getStartDateTime();
        LocalDateTime endTime = productData.getEndDateTime();
        String productDesc = productData.getProductDesc();
        List<String[]> imagesConverted = productData.getImagesConverted();
        Double initPrice = productData.getInitPrice();
        Double bidStep = productData.getBidStep();

        String userId = productData.getUserId();
        List<String> imagesPath = new ArrayList<>();
        for (String[] image : imagesConverted) {
            String path = ImageUtil.convertBase64ToImg(image[0], image[1]);
            imagesPath.add(path);
        }

        LocalDateTime now = LocalDateTime.now();
        if (startTime.isBefore(now)) {
            throw new IllegalArgumentException("Start time cannot be in the past!");
        }
        if (endTime.isBefore(now)) {
            throw new IllegalArgumentException("End time cannot be in the past!");
        }
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End time must be after start time!");
        }

        return new Item(
                productName,
                productDesc,
                initPrice,
                startTime,
                endTime,
                userId,
                category,
                bidStep,
                imagesPath
        );
    }

    public boolean addProduct(ItemPayload productData) {
        Item item = setItem(productData);
        return itemRepository.createItem(item);
    }

    public boolean updateProduct(ItemPayload productData, String itemId) {
        Item item = setItem(productData);
        return itemRepository.updateItem(item, itemId);
    }

    public boolean deleteProduct(String itemId) {
        return itemRepository.deleteItem(itemId);
    }
}
