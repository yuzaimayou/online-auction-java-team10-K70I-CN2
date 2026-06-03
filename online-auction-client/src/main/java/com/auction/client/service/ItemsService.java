package com.auction.client.service;

import com.auction.client.controller.auction.AuctionFormController; // Import cấu trúc dữ liệu gom nhóm
import com.auction.client.network.ItemApiClient;
import com.auction.client.validation.AuctionFormValidator;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.ItemSummary;
import com.auction.shared.model.item.MyBidSummary;
import com.auction.shared.model.payloads.ItemPayload;
import com.auction.shared.util.ImageUtil;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ItemsService {

    private static volatile ItemsService instance;
    public static ItemsService getInstance() {
        if (instance == null) {
            synchronized (ItemsService.class) {
                if (instance == null) instance = new ItemsService();
            }
        }
        return instance;
    }

    private final ItemApiClient apiDb = ItemApiClient.getInstance();
    private List<ItemSummary> cachedItems = new ArrayList<>();

    private ItemsService() {}

    public CompletableFuture<Map<AuctionStatus, List<ItemSummary>>> getFilteredAndGroupedItems(String search, String category) {
        if (!cachedItems.isEmpty()) {
            return CompletableFuture.completedFuture(groupItems(filterItems(cachedItems, search, category))
            );
        }
        return getItems("", "ALL")
                .thenApply(items -> {
                    cachedItems.clear();
                    if (items != null) {
                        cachedItems.addAll(items);
                    }
                    return groupItems( filterItems(cachedItems, search, category)
                    );
                });
    }
    private List<ItemSummary> filterItems(List<ItemSummary> items, String search, String category) {
        String keyword = search == null ? "" : search.toLowerCase().trim();
        return items.stream().filter(item -> {
            boolean matchSearch = keyword.isBlank() || item.getName()
                                    .toLowerCase()
                                    .contains(keyword);
                    boolean matchCategory = category.equalsIgnoreCase("ALL")
                                    || item.getCategory().equalsIgnoreCase(category);
                    return matchSearch && matchCategory;
                }).toList();
    }
    private Map<AuctionStatus, List<ItemSummary>> groupItems(List<ItemSummary> items) {
        Map<AuctionStatus, List<ItemSummary>> groupedMap = new EnumMap<>(AuctionStatus.class);

        groupedMap.put(AuctionStatus.ONGOING, new ArrayList<>());
        groupedMap.put(AuctionStatus.UPCOMING, new ArrayList<>());
        groupedMap.put(AuctionStatus.ENDED, new ArrayList<>());

        for (ItemSummary item : items) {
            if (item.getStatus() == AuctionStatus.BANNED) {
                continue;
            }
            AuctionStatus status = AuctionStatus.compute(item.getStartTime(), item.getEndTime());
            groupedMap.get(status).add(item);
        }
        return groupedMap;
    }
    public CompletableFuture<Item> getItemById(String itemId, String userId) {
        return apiDb.getItemById(itemId, userId);
    }

    public CompletableFuture<List<ItemSummary>> getAllFromSeller(String sellerId) {
        return apiDb.getAllFromSeller(sellerId);
    }

    public CompletableFuture<List<ItemSummary>> getItemsForAdmin() {
        return apiDb.getItemsForAdmin();
    }

    public CompletableFuture<ResponseMessage> banItem(String itemId) {
        return apiDb.banItem(itemId);
    }

    public CompletableFuture<ResponseMessage> deleteItem(String itemId) {
        return apiDb.deleteItem(itemId);
    }

    public CompletableFuture<List<ItemSummary>> getItems(String search, String category) {
        return apiDb.getItems(search, category);
    }

    public CompletableFuture<List<MyBidSummary>> getMyBids() {
        String userId = UserSession.getInstance().getLoggedInUser().getId();
        return apiDb.getMyBids(userId);
    }

    /**
     * Phương thức nạp chồng mới giúp trừu tượng hóa tối đa tầng Controller.
     */
    public CompletableFuture<ResponseMessage> createItem(AuctionFormController.FormData data, List<File> selectedFiles) {
        return createItem(
                data.itemName, data.itemDesc, data.category,
                data.startDate, data.endDate,
                data.startTime, data.endTime,
                data.initPriceStr, data.bidStepStr,
                selectedFiles
        );
    }


    public CompletableFuture<ResponseMessage> updateItem(
            AuctionFormController.FormData data,
            List<String> existingImagePaths,
            List<File> newFiles,
            String itemId) {
        return updateItem(
                data.itemName, data.itemDesc, data.category,
                data.startDate, data.endDate,
                data.startTime, data.endTime,
                data.initPriceStr, data.bidStepStr,
                existingImagePaths, newFiles, itemId
        );
    }
    public CompletableFuture<ResponseMessage> createItem(
            String itemName, String itemDesc, String category,
            LocalDate startDate, LocalDate endDate,
            String startTime, String endTime,
            String initPriceStr, String bidStepStr,
            List<File> selectedFiles) {
        try {
            Double initPrice = AuctionFormValidator.parsePositive(initPriceStr);
            Double bidStep = AuctionFormValidator.parsePositive(bidStepStr);
            LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.parse(startTime));
            LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.parse(endTime));

            List<String[]> imagesConverted = new ArrayList<>();
            for (File file : selectedFiles) {
                String[] base64 = ImageUtil.convertImgToBase64(file);
                if (base64 != null) imagesConverted.add(base64);
            }

            String userId = UserSession.getInstance().getLoggedInUser().getId();

            ItemPayload payload = new ItemPayload(
                    itemName, category, itemDesc, imagesConverted,
                    startDateTime, endDateTime, initPrice, bidStep, userId);

            return apiDb.createItem(payload);

        } catch (IOException e) {
            CompletableFuture<ResponseMessage> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    public CompletableFuture<ResponseMessage> updateItem(
            String itemName, String itemDesc, String category,
            LocalDate startDate, LocalDate endDate,
            String startTime, String endTime,
            String initPriceStr, String bidStepStr,
            List<String> existingImagePaths, List<File> newFiles, String itemId) {
        try {
            Double initPrice = AuctionFormValidator.parsePositive(initPriceStr);
            Double bidStep = AuctionFormValidator.parsePositive(bidStepStr);
            LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.parse(startTime));
            LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.parse(endTime));

            List<String[]> images = new ArrayList<>();
            for (String oldPath : existingImagePaths) {
                images.add(new String[]{oldPath, null});
            }
            for (File file : newFiles) {
                String[] base64 = ImageUtil.convertImgToBase64(file);
                if (base64 != null) images.add(base64);
            }

            ItemPayload payload = new ItemPayload(
                    itemName, category, itemDesc, images,
                    startDateTime, endDateTime, initPrice, bidStep,
                    UserSession.getInstance().getCurrentUserId()
            );

            return apiDb.updateItem(itemId, payload);

        } catch (Exception e) {
            CompletableFuture<ResponseMessage> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    public void clearCache() {
        cachedItems.clear();
    }
}