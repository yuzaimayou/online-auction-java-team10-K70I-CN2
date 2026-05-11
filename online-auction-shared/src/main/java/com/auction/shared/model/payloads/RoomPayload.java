package com.auction.shared.model.payloads;

public class RoomPayload {
    private String itemId, token;

    public RoomPayload() {
    }

    public RoomPayload(String itemId, String token) {
        this.itemId = itemId;
        this.token = token;
    }

    public String getItemId() {
        return itemId;
    }

    public String getToken() {
        return token;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
