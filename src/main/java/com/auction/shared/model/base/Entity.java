package com.auction.shared.model.base;

public abstract class Entity {
    protected String id;

    public Entity(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: ID cannot be null or empty!");
        }
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Error: ID cannot be null or empty!");
        }
        this.id = id;
    }
}
