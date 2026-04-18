package com.auction.shared.model.payloads;

import java.time.LocalDateTime;

public class ProductPayload {
    private String productName, category, productDesc;
    private LocalDateTime startDateTime, endDateTime;
    private Double initPrice, bidStep;
    private String userId;
    private String[] productImg;

    public ProductPayload() {
    }

    public ProductPayload(String productName, String category, String productDesc, String[] productImg, LocalDateTime startTime, LocalDateTime endTime, Double initPrice, Double bidStep, String userId) {
        this.productName = productName;
        this.category = category;
        this.startDateTime = startTime;
        this.endDateTime = endTime;
        this.productDesc = productDesc;
        this.productImg = productImg;
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

    public String[] getProductImg() {
        return productImg;
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
