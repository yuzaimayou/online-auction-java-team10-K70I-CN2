package com.auction.client.controller;


import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SearchStoreController {
    private static final StringProperty searchQuery = new SimpleStringProperty("");

    public static StringProperty searchQueryProperty() {
        return searchQuery;
    }

    public static String getSearchQuery() {
        return searchQuery.get();
    }

    public static void setSearchQuery(String query) {
        searchQuery.set(query);
    }

    public static void reset() {
        searchQuery.set("");
    }
}