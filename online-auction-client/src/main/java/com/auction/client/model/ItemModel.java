package com.auction.client.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ItemModel {
    private final StringProperty itemName;
    private final StringProperty seller;
    private final StringProperty date;
    private final StringProperty imageUrl;

    public ItemModel(String itemName, String seller, String date, String imageUrl) {
        this.itemName = new SimpleStringProperty(itemName);
        this.seller = new SimpleStringProperty(seller);
        this.date = new SimpleStringProperty(date);
        this.imageUrl = new SimpleStringProperty(imageUrl);
    }

    // Item Name
    public String getItemName() { return itemName.get(); }
    public StringProperty itemNameProperty() { return itemName; }
    public void setItemName(String value) { itemName.set(value); }

    // Seller
    public String getSeller() { return seller.get(); }
    public StringProperty sellerProperty() { return seller; }
    public void setSeller(String value) { seller.set(value); }

    // Date (Created At)
    public String getDate() { return date.get(); }
    public StringProperty dateProperty() { return date; }
    public void setDate(String value) { date.set(value); }

    // Image
    public String getImageUrl() { return imageUrl.get(); }
    public StringProperty imageUrlProperty() { return imageUrl; }
    public void setImageUrl(String value) { imageUrl.set(value); }
}