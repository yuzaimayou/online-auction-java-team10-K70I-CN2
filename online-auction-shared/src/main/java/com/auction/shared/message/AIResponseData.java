package com.auction.shared.message;

import com.auction.shared.model.item.ItemSummary;

import java.util.List;

public class AIResponseData {
    private String aiResponse;
    private List<ItemSummary> itemSummaries;

    public AIResponseData() {
    }

    public AIResponseData(String aiResponse, List<ItemSummary> itemSummaries) {
        this.aiResponse = aiResponse;
        this.itemSummaries = itemSummaries;
    }

    public String getAiResponse() {
        return aiResponse;

    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }

    public List<ItemSummary> getItemSummaries() {
        return itemSummaries;
    }

    public void setItemSummaries(List<ItemSummary> items) {
        this.itemSummaries = items;
    }
}
