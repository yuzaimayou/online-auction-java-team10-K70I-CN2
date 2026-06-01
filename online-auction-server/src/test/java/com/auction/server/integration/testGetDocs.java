package com.auction.server.integration;

import com.auction.server.database.DatabaseInit;
import com.auction.server.database.DatabaseManager;

public class testGetDocs {
    public static void main(String[] agrs) {
        DatabaseManager.init();
        DatabaseInit.init();

        AiServiceClient aiServiceClient = AiServiceClient.getInstance();
        String dataResponse = aiServiceClient.getDocs();
        System.out.println(dataResponse);


    }
}
