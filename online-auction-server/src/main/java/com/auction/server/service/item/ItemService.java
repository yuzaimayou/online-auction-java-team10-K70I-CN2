package com.auction.server.service.item;


import com.auction.server.database.DatabaseManager;
import com.auction.server.integration.AiServiceClient;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import com.auction.server.repository.WalletRepository;
import com.auction.server.repository.WalletTransactionRepository;
import com.auction.server.service.auction.AuctionLockManager;
import com.auction.server.util.StringUtil;
import com.auction.shared.model.enums.AuctionStatus;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.ItemSummary;
import com.auction.shared.model.payloads.ItemPayload;
import com.auction.shared.util.ImageUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ItemService {
    private static final Logger LOGGER = Logger.getLogger(ItemService.class.getName());
    private static final ItemService instance = new ItemService();

    private final ItemRepository itemRepository = ItemRepository.getInstance();
    private final AiServiceClient aiServiceClient = AiServiceClient.getInstance();

    public static ItemService getInstance() {
        return instance;
    }

    public List<ItemSummary> getAllItemsBySeller(String sellerId) {
        if (isBlank(sellerId)) {
            return null;
        }
        return itemRepository.findAllBySellerId(sellerId);
    }

    /**
     * Lấy danh sách item theo query string.
     * <p>
     * [UPDATE] Hỗ trợ tham số caller=ADMIN:
     * - Nếu caller=ADMIN → gọi findAllItemsForAdmin() để trả về toàn bộ item kể cả BANNED
     * (admin cần nhìn thấy item đã ban để quản lý).
     * - Nếu không có caller → gọi findAllItems() như cũ, đã loại BANNED ra khỏi kết quả.
     */
    public List<ItemSummary> getItems(String query) {
        if (isBlank(query)) {
            return itemRepository.findAllItems("end_time ASC", 0, null);
        }

        int page = parsePage(query);

        String sortOrder = "end_time ASC";
        if (query.contains("sort=")) {
            String sortParam = extractParam(query, "sort");
            if ("newest".equalsIgnoreCase(sortParam)) {
                sortOrder = "start_time DESC";
            } else if ("price_low".equalsIgnoreCase(sortParam)) {
                sortOrder = "current_price ASC";
            } else if ("price_high".equalsIgnoreCase(sortParam)) {
                sortOrder = "current_price DESC";
            }
        }

        // Parse category filter (null = no filter = ALL)
        String category = null;
        if (query.contains("category=")) {
            String raw = extractParam(query, "category");
            if (raw != null && !raw.isBlank()) {
                try {
                    category = java.net.URLDecoder.decode(raw, java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception e) {
                    category = raw;
                }
            }
        }

        // [NEW] Admin caller: trả về tất cả item kể cả BANNED
        if (query.contains("caller=ADMIN")) {
            return itemRepository.findAllItemsForAdmin(sortOrder, page);
        }

        // Lấy theo keyword + category (public)
        if (query.contains("search=")) {
            try {
                String input = StringUtil.removeAccents(extractParam(query, "search"));

                return getItemsByKeyword(input, category, page);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to search items", e);
            }
        }

        // Lấy theo sellerId
        if (query.contains("sellerId")) {
            String sellerId = extractParam(query, "sellerId");
            return itemRepository.findAllBySellerId(sellerId);
        }

        // Mặc định: trang chủ public, loại bỏ BANNED, filter theo category nếu có
        List<ItemSummary> items = itemRepository.findAllItems(sortOrder, page, category);
        LOGGER.info("Fetched " + items.size() + " items" + (category != null ? " in category: " + category : ""));
        return items;
    }


    public Item setItem(ItemPayload itemData) {
        String itemName = itemData.getItemName();
        String category = itemData.getCategory();
        LocalDateTime startTime = itemData.getStartDateTime();
        LocalDateTime endTime = itemData.getEndDateTime();
        String itemDesc = itemData.getItemDesc();
        List<String[]> imagesConverted = itemData.getImagesConverted();
        Double initPrice = itemData.getInitPrice();
        Double bidStep = itemData.getBidStep();

        String userId = itemData.getUserId();
        List<String> imagesPath = new ArrayList<>();
        LOGGER.fine("Setting item with data:");
        LOGGER.fine(userId);
        LOGGER.fine(itemName);
        LOGGER.fine("----");
        for (String[] image : imagesConverted) {
            if (image[1] == null) {
                imagesPath.add(image[0]);
            } else {
                String path = ImageUtil.convertBase64ToImg(image[0], image[1]);
                imagesPath.add(path);
            }

        }

        return new Item(
                itemName,
                itemDesc,
                initPrice,
                startTime,
                endTime,
                userId,
                category,
                bidStep,
                imagesPath
        );
    }

    public boolean addItem(ItemPayload itemData) {
        if (isSellerSuspended(itemData.getUserId())) {
            LOGGER.info("Item creation rejected: seller is suspended.");
            return false;
        }

        Item item = setItem(itemData);
        boolean created = itemRepository.createItem(item);

        if (created) {
            LOGGER.info(item.getSellerId() + " created item: " + item.getName() + " with ID: " + item.getId());
            CompletableFuture.runAsync(() -> {
                try {
                    List<Path> imagePaths = item.getImagesPath().stream()
                            .map(name -> Paths.get("dataBase", "images", name))
                            .collect(Collectors.toList());
                    aiServiceClient.embeddingProduct(item.getId(), item.getName(), item.getDescription(), imagePaths);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to index item with AI service: " + item.getId(), e);
                }
            });
        }
        return created;
    }

    public Item getItem(String id) {
        return itemRepository.findById(id);
    }

    public boolean updateItem(ItemPayload itemData, String itemId) {
        if (isSellerSuspended(itemData.getUserId())) {
            LOGGER.info("Item update rejected: seller is suspended.");
            return false;
        }
        Item item = setItem(itemData);
        LOGGER.fine("sellerid: " + item.getSellerId());
        LOGGER.fine("image" + item.getImagesPath());
        boolean updated = itemRepository.updateItem(item, itemId);

        if (updated) {
            CompletableFuture.runAsync(() -> {
                try {
                    List<Path> imagePaths = item.getImagesPath().stream()
                            .map(name -> Paths.get("dataBase", "images", name))
                            .collect(Collectors.toList());
                    aiServiceClient.embeddingProduct(item.getId(), item.getName(), item.getDescription(), imagePaths);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to index item with AI service: " + item.getId(), e);
                }
            });
        }
        return updated;
    }

    public boolean deleteItem(String itemId) {
        List<String> fileNames = itemRepository.getImgName(itemId);
        for (String fileName : fileNames) {
            Path filePath = Paths.get("dataBase", "images", fileName);
            try {
                Files.delete(filePath);
                LOGGER.info("Deleted image file: " + filePath);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to delete image file: " + filePath, e);
                return false;
            }
        }
        return itemRepository.deleteItem(itemId);
    }

    private List<ItemSummary> getItemsByKeyword(String input, String category, int page) throws SQLException {
        if (isBlank(input)) {
            return null;
        }
        List<String> keywords = Arrays.stream(input.trim().toLowerCase().split("\\s+"))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toList());
        int offset = page * 10;
        return itemRepository.searchItems(keywords, category, offset);
    }


    private String extractParam(String query, String keyParam) {
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && keyParam.equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int parsePage(String query) {
        if (query.contains("page=")) {
            try {
                return Integer.parseInt(extractParam(query, "page"));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private boolean isSellerSuspended(String userId) {
        com.auction.shared.model.account.User user = new UserRepository().findById(userId);
        return user != null && "Suspended".equalsIgnoreCase(user.getStatus());
    }

    public double getUserLastBid(String itemId, String userId) {
        return itemRepository.getUserLastBid(itemId, userId);
    }

    public boolean banItem(String itemId) {
        if (isBlank(itemId)) {
            return false;
        }

        synchronized (AuctionLockManager.getItemLock(itemId)) {
            Connection conn = null;
            try {
                conn = DatabaseManager.getConnection();
                conn.setAutoCommit(false);

                Item item = itemRepository.findById(conn, itemId);
                if (item == null || item.getStatus() == AuctionStatus.BANNED) {
                    conn.rollback();
                    return false;
                }

                // Update status to BANNED
                if (!itemRepository.updateStatus(conn, itemId, AuctionStatus.BANNED)) {
                    conn.rollback();
                    return false;
                }

                String currentBidderId = item.getCurrentBidderId();
                double currentPrice = item.getHighestCurrentPrice();

                if (!isBlank(currentBidderId)) {
                    WalletRepository walletRepo = new WalletRepository();
                    // Unfreeze amount
                    if (!walletRepo.unfreezeAmount(conn, currentBidderId, currentPrice)) {
                        conn.rollback();
                        return false;
                    }

                    // Log unfreeze
                    WalletTransactionRepository txLogRepo = new WalletTransactionRepository();
                    double[] balances = walletRepo.getBalances(conn, currentBidderId);
                    if (balances != null && balances.length >= 2) {
                        txLogRepo.logUnfreeze(conn, currentBidderId, currentPrice, balances[1] + currentPrice, balances[1], itemId);
                    }

                    // Do NOT clear current_bidder_id and current_price per requirements.
                }

                // Deactivate all auto bids
                com.auction.server.repository.BidRepository bidRepo = new com.auction.server.repository.BidRepository();
                bidRepo.deactivateAllAutoBids(conn, itemId);

                conn.commit();
                return true;
            } catch (Exception e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (Exception ignored) {
                    }
                }
                LOGGER.log(Level.SEVERE, "Failed to ban item " + itemId, e);
                return false;
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }
}
