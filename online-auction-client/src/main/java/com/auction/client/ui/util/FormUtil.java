package com.auction.client.ui.util;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

public class FormUtil {

    private FormUtil() {}

    public static void autoGrowTextArea(TextArea area, double minHeight) {
        area.setWrapText(true);
        area.textProperty().addListener((obs, oldVal, newVal) -> {
            Text helper = new Text(newVal);
            helper.setFont(area.getFont());
            helper.setWrappingWidth(area.getWidth() - 40);
            area.setPrefHeight(Math.max(minHeight,
                    helper.getLayoutBounds().getHeight() + 40));
        });
    }

    public static void populateHalfHourSlots(ComboBox<String> start,
                                             ComboBox<String> end) {
        for (int h = 0; h < 24; h++) {
            for (int m = 0; m < 60; m += 5) {
                String slot = String.format("%02d:%02d", h, m);
                start.getItems().add(slot);
                end.getItems().add(slot);
            }
        }
    }
}