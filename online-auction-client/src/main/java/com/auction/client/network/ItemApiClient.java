package com.auction.client.network;

import com.auction.client.util.AppConfig;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.ItemSummary;
import com.auction.shared.model.item.MyBidSummary;
import com.auction.shared.model.payloads.ItemPayload;
import com.auction.shared.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ItemApiClient {

    private static final ItemApiClient instance = new ItemApiClient();
    public static ItemApiClient getInstance() { return instance; }

    private final HttpClient httpClient;
    private final Gson gson;

    private ItemApiClient() {
        // Tận dụng lại Provider cũ của em hoặc fallback tạo mới
        this.httpClient = HttpClient.newHttpClient();
        this.gson = GsonUtil.getInstance();
    }

    // ─── HÀM GENERIC THẦN THÁNH: GIẢM TRÙNG LẶP & XỬ LÝ TRỰC TIẾP JSON ───
    private <T> CompletableFuture<T> executeGet(String url, Type targetType) {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    // GIẢI QUYẾT TRIỆT ĐỂ DOUBLE PARSING: Parse thẳng từ JsonObject
                    JsonObject jsonObject = gson.fromJson(body, JsonObject.class);
                    if (!"success".equals(jsonObject.get("status").getAsString())) {
                        throw new RuntimeException(jsonObject.has("message") ? jsonObject.get("message").getAsString() : "API Error");
                    }
                    // Trích xuất thẳng vùng 'data' sang Kiểu dữ liệu mong muốn (Không dùng toJsonTree)
                    return gson.fromJson(jsonObject.get("data"), targetType);
                });
    }

    private CompletableFuture<ResponseMessage> executeMutate(HttpRequest request) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), ResponseMessage.class));
    }

    // ─── CÁC API READ (ĐÃ ĐƯỢC RÚT GỌN CỰC SẠCH) ───
    public CompletableFuture<Item> getItemById(String itemId, String userId) {
        String url = String.format("%s/api/items/%s?userId=%s", AppConfig.getHttpUrl(), itemId, userId);
        return executeGet(url, Item.class);
    }

    public CompletableFuture<List<ItemSummary>> getAllFromSeller(String sellerId) {
        String url = String.format("%s/api/items?sellerId=%s", AppConfig.getHttpUrl(), sellerId);
        return executeGet(url, new TypeToken<List<ItemSummary>>(){}.getType());
    }

    public CompletableFuture<List<ItemSummary>> getItemsForAdmin() {
        String url = String.format("%s/api/items?caller=ADMIN", AppConfig.getHttpUrl());
        return executeGet(url, new TypeToken<List<ItemSummary>>(){}.getType());
    }

    public CompletableFuture<List<MyBidSummary>> getMyBids(String userId) {
        String url = AppConfig.getHttpUrl() + "/api/mybids?userId=" + userId;
        return executeGet(url, new TypeToken<List<MyBidSummary>>(){}.getType());
    }

    public CompletableFuture<List<ItemSummary>> getItems(String search, String category) {
        StringBuilder url = new StringBuilder(String.format("%s/api/items", AppConfig.getHttpUrl()));
        boolean hasParam = false;
        try {
            if (search != null && !search.isBlank()) {
                url.append("?search=").append(URLEncoder.encode(search.trim(), StandardCharsets.UTF_8));
                hasParam = true;
            }
            if (category != null && !category.equalsIgnoreCase("ALL")) {
                url.append(hasParam ? "&" : "?").append("category=").append(URLEncoder.encode(category.trim(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeGet(url.toString(), new TypeToken<List<ItemSummary>>(){}.getType());
    }

    // ─── CÁC API MUTATE (WRITE / DELETE) ───
    public CompletableFuture<ResponseMessage> banItem(String itemId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/items/ban/%s", AppConfig.getHttpUrl(), itemId)))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        return executeMutate(request);
    }

    public CompletableFuture<ResponseMessage> createItem(ItemPayload payload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.getHttpUrl() + "/api/items"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();
        return executeMutate(request);
    }

    public CompletableFuture<ResponseMessage> updateItem(String itemId, ItemPayload payload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/items/%s", AppConfig.getHttpUrl(), itemId)))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();
        return executeMutate(request);
    }

    public CompletableFuture<ResponseMessage> deleteItem(String itemId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/items/%s", AppConfig.getHttpUrl(), itemId)))
                .DELETE()
                .build();
        return executeMutate(request);
    }
}