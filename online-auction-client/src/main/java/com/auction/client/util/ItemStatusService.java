package com.auction.client.util;

import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.ItemSummary;

import javafx.scene.control.Label;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class ItemStatusService {

    public String formatTimeLeft(LocalDateTime from, LocalDateTime to) {
        if (to == null || from == null) return "N/A";
        if (from.isAfter(to)) return "00d 00h 00m 00s";

        long days = ChronoUnit.DAYS.between(from, to);
        long hours = ChronoUnit.HOURS.between(from, to)   % 24;
        long minutes = ChronoUnit.MINUTES.between(from, to) % 60;
        long seconds = ChronoUnit.SECONDS.between(from, to) % 60;

        return String.format("%02dd %02dh %02dm %02ds", days, hours, minutes, seconds);
    }
    // COMMON
    public String formatPrice(double price) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi","VN"));
        return "$ " + formatter.format(price) ;
    }

    public AuctionStatus resolveStatus(Item item){
        if(item==null){
            return AuctionStatus.ENDED;
        }
        if(item.getStatus() == AuctionStatus.BANNED){
            return AuctionStatus.BANNED;
        }
        return AuctionStatus.compute(item.getStartTime(), item.getEndTime());
    }

    public AuctionStatus resolveStatus(ItemSummary item){
        if(item==null){
            return AuctionStatus.ENDED;
        }
        if(item.getStatus() == AuctionStatus.BANNED){
            return AuctionStatus.BANNED;
        }
        return AuctionStatus.compute(
                item.getStartTime(),
                item.getEndTime()
        );
    }

    public boolean isOngoing(Item item){
        return resolveStatus(item) == AuctionStatus.ONGOING;
    }
    public void updateCardUi(
            ItemSummary item,
            Label statusLabel,
            Label priceTitleLabel,
            Label priceLabel,
            Label endTimeLabel,
            Label timeTitleLabel
    ){
        AuctionStatus status=resolveStatus(item);
        LocalDateTime now = LocalDateTime.now();
        statusLabel.setText(status.getDisplayName());
        switch(status){
            case UPCOMING -> {
                statusLabel.setStyle("-fx-background-color: #fff3c4; -fx-text-fill: #eea504;");
                priceTitleLabel.setText("START PRICE");
                priceLabel.setText(formatPrice(item.getCurrentPrice()));
                endTimeLabel.setText(formatTimeLeft(now, item.getStartTime()));
                timeTitleLabel.setText("Starts: "+ DateTimeUtil.format(item.getStartTime()));
            }
            case ONGOING -> {
                statusLabel.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: #10b981;");
                priceTitleLabel.setText("CURRENT BID");
                priceLabel.setText(formatPrice(item.getCurrentPrice()));
                endTimeLabel.setText(formatTimeLeft(now, item.getEndTime()));
                timeTitleLabel.setText("Ends: "+ DateTimeUtil.format(item.getEndTime()));
            }
            case ENDED -> {
                statusLabel.setStyle("-fx-background-color: #9e9e9e; -fx-text-fill: white;");
                priceTitleLabel.setText("FINAL PRICE");
                priceLabel.setText(formatPrice(item.getCurrentPrice()));
                endTimeLabel.setText("Auction Ended");
                timeTitleLabel.setText("Ended: "+ DateTimeUtil.format(item.getEndTime()));
            }

        }
    }
}