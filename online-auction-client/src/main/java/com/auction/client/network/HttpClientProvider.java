package com.auction.client.network;

import java.net.http.HttpClient;

public class HttpClientProvider {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private HttpClientProvider() {}
    public static HttpClient get() {
        return CLIENT;
    }
}