package com.auction.client.validation;

import javafx.scene.control.Toggle;

import java.time.LocalDateTime;

public class AuctionFormValidator {

    private AuctionFormValidator(){}

    public static String validate(
            String itemName,
            String itemDesc,
            Toggle category,
            Double initPrice,
            Double bidStep,
            LocalDateTime start,
            LocalDateTime end,
            boolean hasImages
    ){

        if(itemName == null || itemName.isBlank()
                || itemDesc == null || itemDesc.isBlank()
                || category == null
                || initPrice == null
                || bidStep == null
                || !hasImages){
            return "Please fill in all required fields";
        }

        if(initPrice<=0){
            return "Price must be positive";
        }

        if(bidStep<=0){
            return "Bid step must be positive";
        }

        if(bidStep>initPrice){
            return "Bid step cannot exceed starting price";
        }

        LocalDateTime now=LocalDateTime.now();

        if(start.isBefore(now)){
            return "Start time cannot be in the past";
        }

        if(!end.isAfter(start)){
            return "End time must be after start time";
        }

        return null;
    }
}