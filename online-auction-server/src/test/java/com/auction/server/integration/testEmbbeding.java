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
        Item item = ItemRepository.getInstance().findById("d7df116c-edf2-424f-982e-ab5700ec2cc3");
        List<Path> imagePaths = item.getImagesPath().stream()
                .map(name -> Paths.get("dataBase", "images", name))
                .collect(Collectors.toList());
        String respons = aiServiceClient.embeddingProduct(item.getId(), item.getName(), item.getDescription(), imagePaths);
        System.out.println(respons);
    }
}
