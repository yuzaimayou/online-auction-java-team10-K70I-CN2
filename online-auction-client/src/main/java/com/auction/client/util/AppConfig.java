package com.auction.client.util;

public class AppConfig {
    //server ip: 206.189.155.57
    //local server: 127.0.0.1
    public static final String ServerIp = "127.0.0.1";

    public static final int SocketPort = 9090;
    public static final int HttpPort = 8080;
    private static final String appName = "Online Auction System";

    private AppConfig() {
    }

    public static String getHttpUrl() {
        return String.format("http://%s:%d", ServerIp, HttpPort);
    }

    public static String getAppName() {
        return appName;
    }
}
