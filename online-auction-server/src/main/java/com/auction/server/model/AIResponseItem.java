package com.auction.server.model;

public class AIResponseItem {
    private String id, name, description;
    private int image_index;
    private double distance, vector_score, field_weight, final_score;

    public AIResponseItem() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public double getFinal_score() {
        return final_score;
    }

    public String getDescription() {
        return description;
    }

}
