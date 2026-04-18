package com.auction.shared.model.payloads;

public class RoomPayload {
    private String productId, token;

    public RoomPayload(String productId, String token) {
        this.productId = productId;
        this.token = token;
    }

    public String getProductId() {
        return productId;
    }

    public String getToken() {
        return token;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
