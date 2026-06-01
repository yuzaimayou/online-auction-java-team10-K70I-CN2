package com.auction.server.integration;

import com.auction.server.database.DatabaseInit;
import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.ItemRepository;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.ItemSummary;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class testEmbbeding {
    public static void main(String[] agrs) {
        DatabaseManager.init();
        DatabaseInit.init();
        List<ItemSummary> items = ItemRepository.getInstance().findAllItems("s", 0);
        AiServiceClient aiServiceClient = AiServiceClient.getInstance();
        for (ItemSummary itemSummary : items) {
            String itemId = itemSummary.getId();
            Item item = ItemRepository.getInstance().findById(itemId);
            List<Path> imagePaths = item.getImagesPath().stream()
                    .map(name -> Paths.get("dataBase", "images", name))
                    .collect(Collectors.toList());
            String respons = aiServiceClient.embeddingProduct(item.getId(), item.getName(), item.getDescription(), imagePaths);
            System.out.println(respons);
        }

    }
}
