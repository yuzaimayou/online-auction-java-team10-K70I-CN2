package com.auction.shared.model.payloads;

import java.time.LocalDateTime;
import java.util.List;

public class ItemPayload {
    private String itemName, category, itemDesc;
    private LocalDateTime startDateTime, endDateTime;
    private Double initPrice, bidStep;
    private String userId;
    private List<String[]> imagesConverted;

    public ItemPayload() {
    }

    public ItemPayload(String itemName, String category, String itemDesc, List<String[]> images, LocalDateTime startTime, LocalDateTime endTime, Double initPrice, Double bidStep, String userId) {
        this.itemName = itemName;
        this.category = category;
        this.startDateTime = startTime;
        this.endDateTime = endTime;
        this.itemDesc = itemDesc;
        this.imagesConverted = images;
        this.initPrice = initPrice;
        this.bidStep = bidStep;

        this.userId = userId;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemDesc() {
        return itemDesc;
    }

    public List<String[]> getImagesConverted() {
        return imagesConverted;
    }

    public String getCategory() {
        return category;
    }

    public Double getInitPrice() {
        return initPrice;
    }

    public Double getBidStep() {
        return bidStep;
    }

    public String getUserId() {
        return userId;
    }

}