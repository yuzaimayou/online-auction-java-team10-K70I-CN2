package com.auction.server.service;

import com.auction.server.integration.AiServiceClient;
import com.auction.server.repository.ItemRepository;
import com.auction.server.util.StringUtil;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.ItemSummary;
import com.auction.shared.model.payloads.ItemPayload;
import com.auction.shared.util.GsonUtil;
import com.auction.shared.util.ImageUtil;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ItemService {
    private static ItemService instance;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private final ItemRepository itemRepository = ItemRepository.getInstance();
    private final AiServiceClient aiServiceClient = AiServiceClient.getInstance();
    private Gson gson = GsonUtil.getInstance();

    public static ItemService getInstance() {
        if (instance == null) {
            instance = new ItemService();
        }
        return instance;
    }

    public List<ItemSummary> getAllItemsBySeller(String sellerId) {
        if (sellerId == null || sellerId.trim().isEmpty()) {
            return null; // Hoặc trả về list rỗng tùy logic của bạn
        }
        // Gọi hàm từ ItemRepository
        return itemRepository.findAllBySellerId(sellerId);
    }

    //Lay nhieu item
    public List<ItemSummary> getItems(String query) {

        int page = 0;
        String category = null;
        String status = null;

        String sortOrder = "create_at DESC";

        if (query != null && !query.isBlank()) {

            if (query.contains("page=")) {
                try {
                    page = Integer.parseInt(extractParam(query, "page"));
                } catch (Exception ignored) {
                }
            }

            if (query.contains("category=")) {
                category = extractParam(query, "category");
            }

            if (query.contains("status=")) {
                status = extractParam(query, "status");
            }

            if (query.contains("sort=")) {

                String sort = extractParam(query, "sort");

                switch (sort) {

                    case "price_low" ->
                            sortOrder = "current_price ASC";

                    case "price_high" ->
                            sortOrder = "current_price DESC";

                    case "newest" ->
                            sortOrder = "create_at DESC";

                    default ->
                            sortOrder = "end_time ASC";
                }
            }

            if (query.contains("search=")) {

                try {

                    String input = StringUtil.removeAccents(
                            extractParam(query, "search")
                    );

                    return getItemsByKeyword(input, page);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (query.contains("sellerId=")) {

                String sellerId = extractParam(query, "sellerId");

                return itemRepository.findAllBySellerId(sellerId);
            }
        }

        return itemRepository.findAllItems(
                sortOrder,
                page,
                category,
                status
        );
    }

    public void startAuctionStatusUpdater() {

        scheduler.scheduleAtFixedRate(() -> {

            try {

                itemRepository.updateStatus();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }, 0, 1, TimeUnit.SECONDS);
    }

    public Item setItem(ItemPayload itemData) {
        String itemName = itemData.getItemName();
        String category = itemData.getCategory();
        LocalDateTime startTime = itemData.getStartDateTime();
        LocalDateTime endTime = itemData.getEndDateTime();
        String itemDesc = itemData.getItemDesc();
        List<String[]> imagesConverted = itemData.getImagesConverted();
        Double initPrice = itemData.getInitPrice();
        Double bidStep = itemData.getBidStep();

        String userId = itemData.getUserId();
        List<String> imagesPath = new ArrayList<>();
        for (String[] image : imagesConverted) {
            String path = ImageUtil.convertBase64ToImg(image[0], image[1]);
            imagesPath.add(path);
        }

        return new Item(
                itemName,
                itemDesc,
                initPrice,
                startTime,
                endTime,
                userId,
                category,
                bidStep,
                imagesPath
        );
    }

    public boolean addItem(ItemPayload itemData) {
        Item item = setItem(itemData);
        boolean created = itemRepository.createItem(item);

        if (created) {
            System.out.println(item.getSellerId() + " created item: " + item.getName() + " with ID: " + item.getId());
            CompletableFuture.runAsync(() -> {
                try {
                    List<Path> imagePaths = item.getImagesPath().stream()
                            .map(name -> Paths.get("dataBase", "images", name))
                            .collect(Collectors.toList());
                    aiServiceClient.embeddingProduct(item.getId(), item.getName(), item.getDescription(), imagePaths);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        return created;
    }

    public Item getItem(String id) {
        Item item = itemRepository.findById(id);
        return item;
    }

    public boolean updateItem(ItemPayload itemData, String itemId) {
        Item item = setItem(itemData);
        return itemRepository.updateItem(item, itemId);
    }

    public boolean deleteItem(String itemId) {
        List<String> fileNames = itemRepository.getImgName(itemId);
        for (String fileName : fileNames) {
            Path filePath = Paths.get("dataBase", "images", fileName);
            try {
                Files.delete(filePath);
                System.out.println("Deleted image file: " + filePath);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return itemRepository.deleteItem(itemId);
    }

    private List<ItemSummary> getItemsByKeyword(String input, int page) throws SQLException {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        List<String> keywords = Arrays.stream(input.trim().toLowerCase().split("\\s+"))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toList());
        int offset = page * 10;
        return itemRepository.searchItems(keywords, offset);
    }


    private String extractParam(String query, String keyParam) {
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && keyParam.equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }

    public double getUserLastBid(String itemId, String userId) {
        return itemRepository.getUserLastBid(itemId, userId);
    }
}
