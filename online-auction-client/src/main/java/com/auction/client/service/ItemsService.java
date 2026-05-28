package com.auction.client.service;

import com.auction.client.util.AppConfig;
import com.auction.client.util.UserSession;
import com.auction.client.validation.AuctionFormValidator;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.ItemSummary;
import com.auction.shared.model.payloads.ItemPayload;
import com.auction.shared.util.GsonUtil;
import com.auction.shared.util.ImageUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ItemsService {

    // Singleton
    private static volatile ItemsService instance;

    public static ItemsService getInstance() {
        if (instance == null) {
            synchronized (ItemsService.class) {
                if (instance == null) {
                    instance = new ItemsService();
                }
            }
        }
        return instance;
    }

    //  Core dependencies
    private final HttpClient httpClient;
    private final Gson gson;

    private ItemsService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = GsonUtil.getInstance();
    }

    // ITEM - READ (USER)
    public CompletableFuture<Item> getItemById(String itemId, String userId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/items/%s?userId=%s",
                        AppConfig.getHttpUrl(), itemId, userId)))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    ResponseMessage response = gson.fromJson(body, ResponseMessage.class);
                    if (!"success".equals(response.getStatus()) || response.getData() == null) {
                        throw new RuntimeException(response.getMessage());
                    }
                    JsonElement jsonElement = gson.toJsonTree(response.getData());
                    return gson.fromJson(jsonElement, Item.class);
                });
    }

    // ITEM - SELLER
    public CompletableFuture<List<ItemSummary>> getAllFromSeller(String sellerId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/items?sellerId=%s",
                        AppConfig.getHttpUrl(), sellerId)))
                .GET()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    ResponseMessage response = gson.fromJson(body, ResponseMessage.class);
                    if (!"success".equals(response.getStatus())) {
                        throw new RuntimeException(response.getMessage());
                    }

                    Type listType = new TypeToken<List<ItemSummary>>() {}.getType();
                    JsonElement jsonElement = gson.toJsonTree(response.getData());
                    return gson.fromJson(jsonElement, listType);
                });
    }
    // ITEM - ADMIN
    public CompletableFuture<List<ItemSummary>> getItemsForAdmin() {
        String url = String.format("%s/api/items?caller=ADMIN", AppConfig.getHttpUrl());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    ResponseMessage response = gson.fromJson(body, ResponseMessage.class);
                    if (!"success".equals(response.getStatus())) {
                        throw new RuntimeException(response.getMessage());
                    }

                    Type listType = new TypeToken<List<ItemSummary>>() {}.getType();
                    JsonElement jsonElement = gson.toJsonTree(response.getData());
                    return gson.fromJson(jsonElement, listType);
                });
    }

    public CompletableFuture<ResponseMessage> banItem(String itemId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/items/ban/%s",
                        AppConfig.getHttpUrl(), itemId)))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), ResponseMessage.class));
    }
    // ITEM - CRUD
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

                if (base64 != null) {
                    imagesConverted.add(base64);
                }
            }

            String userId = UserSession.getInstance()
                            .getLoggedInUser()
                            .getId();

            ItemPayload payload = new ItemPayload(
                            itemName, category, itemDesc, imagesConverted,
                            startDateTime, endDateTime,
                            initPrice, bidStep, userId);

            String jsonPayload = gson.toJson(payload);
            HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(String.format("%s/api/items",
                                    AppConfig.getHttpUrl())))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                            .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response ->
                                    gson.fromJson(response.body(), ResponseMessage.class));
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
            List<File> allImages = new ArrayList<>(newFiles);
            AuctionFormValidator.Result result = AuctionFormValidator.validateUpdate(
                            itemName, itemDesc, category,
                            startDate, endDate,
                            startTime, endTime,
                            initPriceStr, bidStepStr, allImages);

            if(!result.isValid()){
                CompletableFuture<ResponseMessage> failed = new CompletableFuture<>();
                failed.complete(new ResponseMessage("error", result.getError().message, null)
                );
                return failed;
            }

            Double initPrice = AuctionFormValidator.parsePositive(initPriceStr);
            Double bidStep = AuctionFormValidator.parsePositive(bidStepStr);
            LocalDateTime startDateTime= LocalDateTime.of(startDate, LocalTime.parse(startTime));

            LocalDateTime endDateTime= LocalDateTime.of(endDate, LocalTime.parse(endTime));

            List<String[]> images = new ArrayList<>();

            for(String oldPath:existingImagePaths){
                images.add(new String[]{oldPath, null}
                );
            }
            for(File file:newFiles){
                String[] base64= ImageUtil.convertImgToBase64(file);

                if(base64!=null){
                    images.add(base64);
                }
            }
            ItemPayload payload = new ItemPayload(
                            itemName, category, itemDesc, images,
                            startDateTime, endDateTime, initPrice, bidStep,
                            UserSession.getInstance().getCurrentUserId()
            );
            HttpRequest request = HttpRequest.newBuilder()
                            .uri(
                                    URI.create(String.format("%s/api/items/%s",
                                            AppConfig.getHttpUrl(), itemId)))
                            .header("Content-Type", "application/json")
                            .PUT(
                                    HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                            .build();
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> gson.fromJson(
                                            response.body(),
                                            ResponseMessage.class));
        }
        catch(Exception e){
            CompletableFuture<ResponseMessage> failed= new CompletableFuture<>();
            failed.completeExceptionally(e);

            return failed;
        }
    }

    public CompletableFuture<ResponseMessage> deleteItem(String itemId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/items/%s",
                        AppConfig.getHttpUrl(), itemId)))
                .DELETE()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), ResponseMessage.class));
    }

    // ITEM - SEARCH / FILTER
    public CompletableFuture<List<ItemSummary>> getItems(String search, String category) {
        StringBuilder url = new StringBuilder(
                String.format("%s/api/items", AppConfig.getHttpUrl())
        );
        boolean hasParam = false;
        try {
            if (search != null && !search.isBlank()) {
                url.append("?search=")
                        .append(URLEncoder.encode(search.trim(), StandardCharsets.UTF_8));
                hasParam = true;
            }
            if (category != null && !category.equalsIgnoreCase("ALL")) {
                url.append(hasParam ? "&" : "?")
                        .append("category=")
                        .append(URLEncoder.encode(category.trim(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    ResponseMessage response = gson.fromJson(body, ResponseMessage.class);
                    if (!"success".equals(response.getStatus())) {
                        throw new RuntimeException(response.getMessage());
                    }
                    Type listType = new TypeToken<List<ItemSummary>>() {}.getType();
                    JsonElement jsonElement = gson.toJsonTree(response.getData());
                    return gson.fromJson(jsonElement, listType);
                });
    }
}