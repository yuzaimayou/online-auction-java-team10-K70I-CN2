package com.auction.client.validation;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class AuctionFormValidator {

    public enum Error {
        MISSING_FIELDS("Please fill in all required fields."),
        INVALID_INIT_PRICE("Price must be a positive number."),
        INVALID_BID_STEP("Bid step must be a positive number."),
        BID_STEP_EXCEEDS_PRICE("Bid step cannot be greater than starting price."),
        START_TIME_IN_PAST("Start time must be after current time."),
        END_TIME_BEFORE_START("End time must be after start time.");

        public final String message;

        Error(String message){
            this.message=message;
        }
    }

    public static final class Result {

        private final Error error;
        private Result(Error error){
            this.error=error;
        }
        public static Result ok(){
            return new Result(null);
        }
        public static Result fail(Error e){
            return new Result(e);
        }
        public boolean isValid(){
            return error==null;
        }
        public Error getError(){
            return error;
        }
    }

    public static Result validateCreate(
            String itemName,
            String itemDesc,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            String startTime,
            String endTime,
            String initPriceStr,
            String bidStepStr,
            List<File> imageFiles
    ){
        return validate(
                itemName,
                itemDesc,
                category,
                startDate,
                endDate,
                startTime,
                endTime,
                initPriceStr,
                bidStepStr,
                imageFiles,
                true
        );
    }

    public static Result validateUpdate(
            String itemName,
            String itemDesc,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            String startTime,
            String endTime,
            String initPriceStr,
            String bidStepStr,
            List<File> imageFiles
    ){
        return validate(
                itemName,
                itemDesc,
                category,
                startDate,
                endDate,
                startTime,
                endTime,
                initPriceStr,
                bidStepStr,
                imageFiles,
                false
        );
    }

    private static Result validate(
            String itemName,
            String itemDesc,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            String startTime,
            String endTime,
            String initPriceStr,
            String bidStepStr,
            List<File> imageFiles,
            boolean validateCurrentTime
    ){

        if(isBlank(itemName)
                ||isBlank(itemDesc)
                ||isBlank(category)
                ||startDate==null
                ||endDate==null
                ||isBlank(startTime)
                ||isBlank(endTime)
                ||imageFiles==null
                ||imageFiles.isEmpty()){

            return Result.fail(Error.MISSING_FIELDS);
        }

        Double initPrice=parsePositive(initPriceStr);

        if(initPrice==null)
            return Result.fail(Error.INVALID_INIT_PRICE);

        Double bidStep=parsePositive(bidStepStr);

        if(bidStep==null)
            return Result.fail(Error.INVALID_BID_STEP);

        if(bidStep>initPrice)
            return Result.fail(Error.BID_STEP_EXCEEDS_PRICE);

        LocalDateTime start= LocalDateTime.of(startDate, LocalTime.parse(startTime));

        LocalDateTime end= LocalDateTime.of(endDate, LocalTime.parse(endTime));

        if(validateCurrentTime && !start.isAfter(LocalDateTime.now())) {
            return Result.fail(Error.START_TIME_IN_PAST
            );
        }

        if(!end.isAfter(start)) {
            return Result.fail(Error.END_TIME_BEFORE_START);
        }
        return Result.ok();
    }

    public static Double parsePositive(String s){
        if(s==null||s.isBlank())
            return null;
        try{
            double value= Double.parseDouble(s);
            return value>0 ? value : null;
        }catch(Exception e){
            return null;
        }
    }

    private static boolean isBlank(String s){
        return s==null||s.isBlank();
    }
}