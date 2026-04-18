package com.auction.client.util;

public class AppConfig {
    //server ip: 206.189.155.57
    //local server: 127.0.0.1
    public static final String ServerIp = "127.0.0.1";

    public static final int ServerPort = 8000;
    public static final int ImagePort = 1401;

    private AppConfig() {
    }

    public static String getImageUrl() {
        return String.format("http://%s:%d/images/", ServerIp, ImagePort);
    }
}
