package com.auction.client.service;

import com.auction.client.network.ItemApiClient;
import com.auction.client.validation.AuctionFormValidator;
import com.auction.shared.message.ResponseMessage;
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
import java.util.List;
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

    // Phụ thuộc duy nhất vào client mạng vừa tách
    private final ItemApiClient apiDb = ItemApiClient.getInstance();

    private ItemsService() {}

    // ─── ĐIỀU HƯỚNG THẲNG XUỐNG TẦNG MẠNG (KHÔNG CHỨA LOGIC HTTP) ───
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

    // ─── TẬP TRUNG XỬ LÝ LOGIC NGHIỆP VỤ (BUSINESS LOGIC) TẠI ĐÂY ───
    public CompletableFuture<ResponseMessage> createItem(
            String itemName, String itemDesc, String category,
            LocalDate startDate, LocalDate endDate,
            String startTime, String endTime,
            String initPriceStr, String bidStepStr,
            List<File> selectedFiles) {
        try {
            // 1. Nghiệp vụ Validate & Đổi kiểu dữ liệu UI
            Double initPrice = AuctionFormValidator.parsePositive(initPriceStr);
            Double bidStep = AuctionFormValidator.parsePositive(bidStepStr);
            LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.parse(startTime));
            LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.parse(endTime));

            // 2. Nghiệp vụ chuyển đổi tài nguyên vật lý sang Base64 mã hóa
            List<String[]> imagesConverted = new ArrayList<>();
            for (File file : selectedFiles) {
                String[] base64 = ImageUtil.convertImgToBase64(file);
                if (base64 != null) imagesConverted.add(base64);
            }

            // 3. Nghiệp vụ lấy context phiên đăng nhập hiện tại
            String userId = UserSession.getInstance().getLoggedInUser().getId();

            // Đóng gói payload sạch sẽ và đẩy qua cho Tợ mạng bắn API
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
}