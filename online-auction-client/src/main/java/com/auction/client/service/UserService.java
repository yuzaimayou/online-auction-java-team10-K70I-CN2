package com.auction.client.service;

import com.auction.client.controller.admin.UserRowViewModel;
import com.auction.client.network.HttpClientProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UserService {
    private static UserService instance;
    private final Gson gson = new Gson();
    private static final String BASE_URL = "http://localhost:8080/api";

    private UserService() {}

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    public CompletableFuture<List<UserRowViewModel>> getAllUsers() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users"))
                .GET()
                .build();

        return HttpClientProvider.get()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Server trả về mã lỗi: " + response.statusCode());
                    }
                    Type listType = new TypeToken<List<JsonObject>>() {}.getType();
                    List<JsonObject> raw = gson.fromJson(response.body(), listType);
                    List<UserRowViewModel> users = new ArrayList<>();
                    for (JsonObject obj : raw) {
                        users.add(new UserRowViewModel(
                                obj.has("id") ? obj.get("id").getAsString() : "",
                                obj.has("username") ? obj.get("username").getAsString() : "",
                                obj.has("email") ? obj.get("email").getAsString() : "",
                                obj.has("status") ? obj.get("status").getAsString() : "Active",
                                obj.has("role") ? obj.get("role").getAsString() : ""
                        ));
                    }
                    return users;
                });
    }

    public CompletableFuture<Boolean> banUser(String targetUserId, String adminId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("targetUserId", targetUserId);
        if (adminId != null) {
            payload.addProperty("adminId", adminId);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users/ban"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        return HttpClientProvider.get()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200);
    }
}