package com.auction.shared.model.payloads;

import java.time.LocalDateTime;
import java.util.List;

public class ItemPayload {
    private String productName, category, productDesc;
    private LocalDateTime startDateTime, endDateTime;
    private Double initPrice, bidStep;
    private String userId;
    private List<String[]> imagesConverted;

    public ItemPayload() {
    }

    public ItemPayload(String productName, String category, String productDesc, List<String[]> images, LocalDateTime startTime, LocalDateTime endTime, Double initPrice, Double bidStep, String userId) {
        this.productName = productName;
        this.category = category;
        this.startDateTime = startTime;
        this.endDateTime = endTime;
        this.productDesc = productDesc;
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

    public String getProductName() {
        return productName;
    }

    public String getProductDesc() {
        return productDesc;
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
