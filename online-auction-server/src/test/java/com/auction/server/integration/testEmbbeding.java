package com.auction.server.integration;

import com.auction.server.database.DatabaseInit;
import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.ItemRepository;
import com.auction.shared.model.item.Item;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class testEmbbeding {
    public static void main(String[] agrs) {
        DatabaseManager.init();
        DatabaseInit.init();
        AiServiceClient aiServiceClient = AiServiceClient.getInstance();
        Item item = ItemRepository.getInstance().findById("f92a7118-b9d8-4f29-8ed2-d52d50e33a85");
        List<Path> imagePaths = item.getImagesPath().stream()
                .map(name -> Paths.get("dataBase", "images", name))
                .collect(Collectors.toList());
        String respons = aiServiceClient.embeddingProduct(item.getId(), item.getName(), item.getDescription(), imagePaths);
        System.out.println(respons);
    }
}
