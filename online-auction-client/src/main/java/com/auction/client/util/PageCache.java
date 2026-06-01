package com.auction.client.util;

import com.auction.client.controller.common.HomePageController;
import com.auction.client.controller.auction.ItemPageController;
import javafx.scene.Parent;

public class PageCache {

    private static Parent homePageRoot;
    private static HomePageController homePageController;
    private static ItemPageController currentItemController;

    private PageCache() {}

    public static Parent getHomeRoot() { return homePageRoot; }
    public static HomePageController getHomeController() { return homePageController; }

    public static void setHome(Parent root, HomePageController ctrl) {
        homePageRoot       = root;
        homePageController = ctrl;
    }

    public static void setCurrentItem(ItemPageController ctrl) {
        currentItemController = ctrl;
    }

    public static void disposeCurrentItem() {
        if (currentItemController != null) {
            currentItemController.dispose();
            currentItemController = null;
        }
    }

    public static void clear() {
        disposeCurrentItem();
        homePageRoot       = null;
        homePageController = null;
    }
}