package com.auction.server.service;

import com.auction.server.database.DatabaseInit;
import com.auction.server.database.DatabaseManager;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.shared.model.enums.AuctionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ProductionBiddingFlowTest {
    private static final double EPSILON = 0.000001d;

    private final BidService bidService = new BidService();
    private final ItemRepository itemRepository = ItemRepository.getInstance();
    private String prefix;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        Files.createDirectories(Paths.get(System.getProperty("user.dir"), "dataBase"));
        try (Connection ignored = DatabaseManager.getConnection()) {
            // Already initialized by another test.
        } catch (Exception ignored) {
            DatabaseManager.init();
        }
        DatabaseInit.init();
    }

    @BeforeEach
    void setUp() {
        prefix = "junit-" + UUID.randomUUID();
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Connection conn = DatabaseManager.getConnection()) {
            deleteLike(conn, "DELETE FROM bids WHERE item_id LIKE ?", prefix + "%");
            deleteLike(conn, "DELETE FROM auto_bids WHERE item_id LIKE ?", prefix + "%");
            deleteLike(conn, "DELETE FROM items WHERE id LIKE ?", prefix + "%");
            deleteLike(conn, "DELETE FROM users WHERE id LIKE ?", prefix + "%");
        }
    }

    @Test
    void validBidSucceedsAndUpdatesCurrentBidder() throws Exception {
        String itemId = item("valid", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30), null);
        String bidder = user("bidder");

        assertTrue(bidService.placeBid(itemId, bidder, 15.0));

        assertEquals(15.0, currentPrice(itemId), EPSILON);
        assertEquals(bidder, currentBidder(itemId));
        assertEquals(1, bidCount(itemId));
    }

    @Test
    void bidLowerThanCurrentPriceIsRejected() throws Exception {
        String itemId = item("low", 20.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30), null);
        String bidder = user("bidder");

        assertFalse(bidService.placeBid(itemId, bidder, 15.0));

        assertEquals(20.0, currentPrice(itemId), EPSILON);
        assertNull(currentBidder(itemId));
        assertEquals(0, bidCount(itemId));
    }

    @Test
    void bidBelowRequiredBidStepIsRejected() throws Exception {
        String itemId = item("step", 20.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30), null);
        String bidder = user("bidder");

        assertFalse(bidService.placeBid(itemId, bidder, 24.0));

        assertEquals(20.0, currentPrice(itemId), EPSILON);
        assertEquals(0, bidCount(itemId));
    }

    @Test
    void bidAfterAuctionEndedIsRejected() throws Exception {
        String itemId = item("ended", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(30), LocalDateTime.now().minusMinutes(1), null);
        String bidder = user("bidder");

        assertFalse(bidService.placeBid(itemId, bidder, 15.0));

        assertEquals(10.0, currentPrice(itemId), EPSILON);
        assertEquals(0, bidCount(itemId));
    }

    @Test
    void bidBeforeAuctionStartsIsRejected() throws Exception {
        String itemId = item("upcoming", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusMinutes(30), null);
        String bidder = user("bidder");

        assertFalse(bidService.placeBid(itemId, bidder, 15.0));

        assertEquals(10.0, currentPrice(itemId), EPSILON);
        assertEquals(0, bidCount(itemId));
    }

    @Test
    void twoConcurrentBiddersCannotBothBecomeFinalWinner() throws Exception {
        String itemId = item("two-race", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30), null);
        String lowBidder = user("low-bidder");
        String highBidder = user("high-bidder");

        runConcurrent(List.of(
                () -> bidService.placeBid(itemId, lowBidder, 15.0),
                () -> bidService.placeBid(itemId, highBidder, 20.0)
        ));

        double finalPrice = currentPrice(itemId);
        assertEquals(maxBidPrice(itemId), finalPrice, EPSILON);
        assertEquals(1, bidCountAtPrice(itemId, finalPrice));
        assertEquals(bidderForPrice(itemId, finalPrice), currentBidder(itemId));

        // Winner wallet verification
        String leader = currentBidder(itemId);
        String loser = leader.equals(lowBidder) ? highBidder : lowBidder;
        double winnerBidPrice = finalPrice;

        assertEquals(10000.0 - winnerBidPrice, queryDouble("SELECT balance FROM users WHERE id = ?", leader), EPSILON);
        assertEquals(winnerBidPrice, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", leader), EPSILON);

        // Loser wallet verification
        assertEquals(10000.0, queryDouble("SELECT balance FROM users WHERE id = ?", loser), EPSILON);
        assertEquals(0.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", loser), EPSILON);
    }

    @Test
    void twentyConcurrentBiddersLeaveExactlyOneHighestBidder() throws Exception {
        String itemId = item("twenty-race", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30), null);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        List<String> bidders = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            String bidder = user("bidder-" + i);
            bidders.add(bidder);
            double amount = 15.0 + (i * 5.0);
            tasks.add(() -> bidService.placeBid(itemId, bidder, amount));
        }

        runConcurrent(tasks);

        double finalPrice = currentPrice(itemId);
        assertEquals(finalPrice, maxBidPrice(itemId), EPSILON);
        assertEquals(1, bidCountAtPrice(itemId, finalPrice));
        assertEquals(bidderForPrice(itemId, finalPrice), currentBidder(itemId));

        // Verify wallet balances for all 20 bidders:
        String winner = currentBidder(itemId);
        for (String bidder : bidders) {
            if (bidder.equals(winner)) {
                assertEquals(10000.0 - finalPrice, queryDouble("SELECT balance FROM users WHERE id = ?", bidder), EPSILON);
                assertEquals(finalPrice, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", bidder), EPSILON);
            } else {
                assertEquals(10000.0, queryDouble("SELECT balance FROM users WHERE id = ?", bidder), EPSILON);
                assertEquals(0.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", bidder), EPSILON);
            }
        }
    }

    @Test
    void antiSnipingExtendsEndTimeForValidBidInsideFinalMinute() throws Exception {
        LocalDateTime originalEnd = LocalDateTime.now().plusSeconds(30);
        String itemId = item("snipe", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), originalEnd, null);
        String bidder = user("bidder");

        assertTrue(bidService.placeBid(itemId, bidder, 15.0));

        LocalDateTime updatedEnd = endTime(itemId);
        assertTrue(updatedEnd.isAfter(originalEnd));
        long secondsFromNow = ChronoUnit.SECONDS.between(LocalDateTime.now(), updatedEnd);
        assertTrue(secondsFromNow >= 50 && secondsFromNow <= 70);
    }

    @Test
    void antiSnipingDoesNotExtendOutsideFinalMinute() throws Exception {
        LocalDateTime originalEnd = LocalDateTime.now().plusMinutes(5);
        String itemId = item("no-snipe", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), originalEnd, null);
        String bidder = user("bidder");

        assertTrue(bidService.placeBid(itemId, bidder, 15.0));

        assertEquals(originalEnd, endTime(itemId));
    }

    @Test
    void autoBidRegisterOnlyWhenUserAlreadyLeads() throws Exception {
        String leader = user("leader");
        String itemId = item("auto-register-only", 20.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30), leader);
        insertBid(itemId, leader, 20.0);

        assertTrue(bidService.registerAutoBidAndMaybeBid(itemId, leader, 100.0, 5.0));

        assertEquals(20.0, currentPrice(itemId), EPSILON);
        assertEquals(leader, currentBidder(itemId));
        assertEquals(1, bidCount(itemId));
        assertEquals(1, activeAutoBidCount(itemId));
    }

    @Test
    void twoAutoBidsCompeteAndHigherMaxWinsWithoutExceedingMax() throws Exception {
        String itemId = item("auto-compete", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30), null);
        String first = user("first");
        String second = user("second");

        assertTrue(bidService.registerAutoBidAndMaybeBid(itemId, first, 50.0, 5.0));
        Thread.sleep(5L);
        assertTrue(bidService.registerAutoBidAndMaybeBid(itemId, second, 60.0, 5.0));

        assertEquals(second, currentBidder(itemId));
        assertTrue(currentPrice(itemId) <= 60.0 + EPSILON);
        assertTrue(maxBidPrice(itemId) <= 60.0 + EPSILON);
        assertTrue(bidCount(itemId) > 2);
        assertEquals(currentPrice(itemId), maxBidPrice(itemId), EPSILON);
    }

    @Test
    void autoBidResolverUsesEarlierRegistrationForEqualNextBid() {
        AutoBidResolver resolver = new AutoBidResolver(EPSILON);
        LocalDateTime earlier = LocalDateTime.now();
        LocalDateTime later = earlier.plusSeconds(1);
        List<BidRepository.AutoBidConfig> configs = List.of(
                new BidRepository.AutoBidConfig("earlier", 50.0, 5.0, earlier),
                new BidRepository.AutoBidConfig("later", 50.0, 5.0, later)
        );

        AutoBidResolver.ResolvedAutoBid selected = resolver.selectNextBid(configs, "manual", 20.0);

        assertNotNull(selected);
        assertEquals("earlier", selected.userId());
        assertEquals(25.0, selected.bidPrice(), EPSILON);
    }

    @Test
    void itemRepositoryUpdateStatusTransitionsOnlyEligibleRows() throws Exception {
        String upcomingToOngoing = item("status-upcoming", 10.0, 5.0, AuctionStatus.UPCOMING,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(30), null);
        String ongoingToEnded = item("status-ended", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(30), LocalDateTime.now().minusMinutes(1), null);
        String remainsOngoing = item("status-ongoing", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(30), null);

        List<String> updatedIds = itemRepository.updateStatus();

        assertTrue(updatedIds.contains(upcomingToOngoing));
        assertTrue(updatedIds.contains(ongoingToEnded));
        assertFalse(updatedIds.contains(remainsOngoing));
        assertEquals(AuctionStatus.ONGOING.name(), storedStatus(upcomingToOngoing));
        assertEquals(AuctionStatus.ENDED.name(), storedStatus(ongoingToEnded));
        assertEquals(AuctionStatus.ONGOING.name(), storedStatus(remainsOngoing));
    }

    private List<Boolean> runConcurrent(List<Callable<Boolean>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Callable<Boolean> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    return task.call();
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private String user(String label) throws Exception {
        String userId = prefix + "-user-" + label;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                     INSERT INTO users(id, username, password, role, isVerify, email, balance, frozen_balance)
                     VALUES(?,?,?,?,?,?,?,?)
                     """)) {
            stmt.setString(1, userId);
            stmt.setString(2, userId);
            stmt.setString(3, "test");
            stmt.setString(4, "USER");
            stmt.setInt(5, 1);
            stmt.setString(6, userId + "@example.test");
            stmt.setDouble(7, 10_000.0);
            stmt.setDouble(8, 0.0);
            stmt.executeUpdate();
        }
        return userId;
    }

    private String item(String label,
                        double currentPrice,
                        double bidStep,
                        AuctionStatus status,
                        LocalDateTime startTime,
                        LocalDateTime endTime,
                        String currentBidderId) throws Exception {
        String seller = user("seller-" + label);
        String itemId = prefix + "-item-" + label;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                     INSERT INTO items(id, name, description, start_price, current_price, seller_id, start_time, end_time,
                                       category, bid_step, image_path, create_at, top_player_id, search_name, status, current_bidder_id)
                     VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                     """)) {
            stmt.setString(1, itemId);
            stmt.setString(2, "JUnit item " + label);
            stmt.setString(3, "JUnit production bidding flow fixture");
            stmt.setDouble(4, 10.0);
            stmt.setDouble(5, currentPrice);
            stmt.setString(6, seller);
            stmt.setString(7, startTime.toString());
            stmt.setString(8, endTime.toString());
            stmt.setString(9, "other");
            stmt.setDouble(10, bidStep);
            stmt.setString(11, "[]");
            stmt.setString(12, LocalDateTime.now().toString());
            stmt.setString(13, null);
            stmt.setString(14, "junit item " + label);
            stmt.setString(15, status.name());
            stmt.setString(16, currentBidderId);
            stmt.executeUpdate();
        }
        return itemId;
    }

    private void insertBid(String itemId, String userId, double amount) throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                     INSERT INTO bids(item_id, user_id, bid_price, bid_time)
                     VALUES(?,?,?,?)
                     """)) {
            stmt.setString(1, itemId);
            stmt.setString(2, userId);
            stmt.setDouble(3, amount);
            stmt.setString(4, LocalDateTime.now().toString());
            stmt.executeUpdate();
        }
    }

    private static void deleteLike(Connection conn, String sql, String value) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            stmt.executeUpdate();
        }
    }

    private double currentPrice(String itemId) throws Exception {
        return queryDouble("SELECT current_price FROM items WHERE id = ?", itemId);
    }

    private String currentBidder(String itemId) throws Exception {
        return queryString("SELECT current_bidder_id FROM items WHERE id = ?", itemId);
    }

    private LocalDateTime endTime(String itemId) throws Exception {
        return LocalDateTime.parse(queryString("SELECT end_time FROM items WHERE id = ?", itemId));
    }

    private String storedStatus(String itemId) throws Exception {
        return queryString("SELECT status FROM items WHERE id = ?", itemId);
    }

    private int bidCount(String itemId) throws Exception {
        return queryInt("SELECT COUNT(*) FROM bids WHERE item_id = ?", itemId);
    }

    private int activeAutoBidCount(String itemId) throws Exception {
        return queryInt("SELECT COUNT(*) FROM auto_bids WHERE item_id = ? AND is_active = 1", itemId);
    }

    private double maxBidPrice(String itemId) throws Exception {
        return queryDouble("SELECT COALESCE(MAX(bid_price), 0) FROM bids WHERE item_id = ?", itemId);
    }

    private int bidCountAtPrice(String itemId, double price) throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                     SELECT COUNT(*) FROM bids WHERE item_id = ? AND ABS(bid_price - ?) < 0.000001
                     """)) {
            stmt.setString(1, itemId);
            stmt.setDouble(2, price);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }

    private String bidderForPrice(String itemId, double price) throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                     SELECT user_id FROM bids
                     WHERE item_id = ? AND ABS(bid_price - ?) < 0.000001
                     ORDER BY id DESC
                     LIMIT 1
                     """)) {
            stmt.setString(1, itemId);
            stmt.setDouble(2, price);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getString(1);
            }
        }
    }

    private int queryInt(String sql, String id) throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }

    private double queryDouble(String sql, String id) throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getDouble(1);
            }
        }
    }

    private String queryString(String sql, String id) throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getString(1);
            }
        }
    }

    @Test
    void manualBidShouldFreezeMoneyForHighestBidder() throws Exception {
        String itemId = item("wallet-freeze", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30), null);
        String bidder = user("wallet-bidder");

        assertTrue(bidService.placeBid(itemId, bidder, 15.0));

        assertEquals(15.0, currentPrice(itemId), EPSILON);
        assertEquals(bidder, currentBidder(itemId));

        // Assert A available balance decreased (from 10,000 to 9,985)
        assertEquals(9985.0, queryDouble("SELECT balance FROM users WHERE id = ?", bidder), EPSILON);
        // Assert A frozen balance increased by 15.0
        assertEquals(15.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", bidder), EPSILON);
    }

    @Test
    void outbidShouldReleasePreviousLeaderAndFreezeNewLeader() throws Exception {
        String itemId = item("wallet-outbid", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30), null);
        String bidderA = user("bidder-a");
        String bidderB = user("bidder-b");

        assertTrue(bidService.placeBid(itemId, bidderA, 15.0));
        assertTrue(bidService.placeBid(itemId, bidderB, 20.0));

        // Assert A's frozen balance is released (back to 0) and balance returned (back to 10,000)
        assertEquals(10000.0, queryDouble("SELECT balance FROM users WHERE id = ?", bidderA), EPSILON);
        assertEquals(0.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", bidderA), EPSILON);

        // Assert B's frozen balance is locked (20) and balance decreased (9,980)
        assertEquals(9980.0, queryDouble("SELECT balance FROM users WHERE id = ?", bidderB), EPSILON);
        assertEquals(20.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", bidderB), EPSILON);
    }

    @Test
    void sameBidderBiddingAgainShouldLockNewAmount() throws Exception {
        String itemId = item("wallet-rebid", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30), null);
        String bidderA = user("bidder-a");
        String bidderB = user("bidder-b");

        assertTrue(bidService.placeBid(itemId, bidderA, 15.0));
        assertTrue(bidService.placeBid(itemId, bidderB, 20.0));
        assertTrue(bidService.placeBid(itemId, bidderA, 25.0));

        // bidderA outbid, then bid again.
        // bidderB should be released (frozen = 0, balance = 10,000)
        assertEquals(10000.0, queryDouble("SELECT balance FROM users WHERE id = ?", bidderB), EPSILON);
        assertEquals(0.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", bidderB), EPSILON);

        // bidderA's new frozen is 25, balance is 9975
        assertEquals(9975.0, queryDouble("SELECT balance FROM users WHERE id = ?", bidderA), EPSILON);
        assertEquals(25.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", bidderA), EPSILON);
    }

    @Test
    void insufficientBalanceShouldRejectBidAndNotMutateState() throws Exception {
        String itemId = item("wallet-insufficient", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30), null);
        String poorBidder = prefix + "-poor";
        
        // Create user with 5.0 balance
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                     INSERT INTO users(id, username, password, role, isVerify, email, balance, frozen_balance)
                     VALUES(?,?,?,?,?,?,?,?)
                     """)) {
            stmt.setString(1, poorBidder);
            stmt.setString(2, poorBidder);
            stmt.setString(3, "test");
            stmt.setString(4, "USER");
            stmt.setInt(5, 1);
            stmt.setString(6, poorBidder + "@example.test");
            stmt.setDouble(7, 5.0);
            stmt.setDouble(8, 0.0);
            stmt.executeUpdate();
        }

        // Try bidding 15.0 (which is greater than 5.0 available balance)
        assertFalse(bidService.placeBid(itemId, poorBidder, 15.0));

        // Assert no balance state mutated
        assertEquals(5.0, queryDouble("SELECT balance FROM users WHERE id = ?", poorBidder), EPSILON);
        assertEquals(0.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", poorBidder), EPSILON);

        // Assert item was not updated
        assertEquals(10.0, currentPrice(itemId), EPSILON);
        assertNull(currentBidder(itemId));
        assertEquals(0, bidCount(itemId));
    }

    @Test
    void autoBidShouldFreezeAndReleaseCorrectly() throws Exception {
        String itemId = item("wallet-autobid", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30), null);
        String first = user("first");
        String second = user("second");

        assertTrue(bidService.registerAutoBidAndMaybeBid(itemId, first, 50.0, 5.0));
        Thread.sleep(5L);
        assertTrue(bidService.registerAutoBidAndMaybeBid(itemId, second, 60.0, 5.0));

        // The competition should end with 'second' leading at 55.0 or 60.0
        String leader = currentBidder(itemId);
        double finalPrice = currentPrice(itemId);
        assertEquals(second, leader);

        // Assert second user's frozen balance matches final price
        assertEquals(finalPrice, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", second), EPSILON);
        assertEquals(10000.0 - finalPrice, queryDouble("SELECT balance FROM users WHERE id = ?", second), EPSILON);

        // Assert first user's frozen balance is released (returned to 0)
        assertEquals(0.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", first), EPSILON);
        assertEquals(10000.0, queryDouble("SELECT balance FROM users WHERE id = ?", first), EPSILON);
    }

    @Test
    void currentLeaderCannotBidAgainAndWalletDoesNotChange() throws Exception {
        String itemId = item("wallet-consecutive", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30), null);
        String bidder = user("consecutive-bidder");

        assertTrue(bidService.placeBid(itemId, bidder, 15.0));

        // Attempt consecutive bid by same bidder
        assertFalse(bidService.placeBid(itemId, bidder, 20.0));

        // State remains at 15.0
        assertEquals(15.0, currentPrice(itemId), EPSILON);
        assertEquals(bidder, currentBidder(itemId));
        assertEquals(9985.0, queryDouble("SELECT balance FROM users WHERE id = ?", bidder), EPSILON);
        assertEquals(15.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", bidder), EPSILON);
    }

    @Test
    void autoBidCandidateWithInsufficientFundsIsSkippedSafely() throws Exception {
        String itemId = item("wallet-auto-poor", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30), null);
        String rich = user("rich-autobidder");
        String poor = prefix + "-poor-autobidder";

        // Create poor user with 22.0 balance
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                     INSERT INTO users(id, username, password, role, isVerify, email, balance, frozen_balance)
                     VALUES(?,?,?,?,?,?,?,?)
                     """)) {
            stmt.setString(1, poor);
            stmt.setString(2, poor);
            stmt.setString(3, "test");
            stmt.setString(4, "USER");
            stmt.setInt(5, 1);
            stmt.setString(6, poor + "@example.test");
            stmt.setDouble(7, 22.0);
            stmt.setDouble(8, 0.0);
            stmt.executeUpdate();
        }

        // Register rich auto-bid (max 100, increment 5)
        assertTrue(bidService.registerAutoBidAndMaybeBid(itemId, rich, 100.0, 5.0));
        // Register poor auto-bid (max 100, increment 5)
        assertTrue(bidService.registerAutoBidAndMaybeBid(itemId, poor, 100.0, 5.0));

        // Price competition:
        // rich bids 15
        // poor bids 20
        // rich bids 25
        // poor wants to bid 30, but poor only has 22.0 balance!
        // So poor's auto-bid is skipped and deactivated.
        // rich should win/remain the leader.
        String leader = currentBidder(itemId);
        assertEquals(rich, leader);
        double finalPrice = currentPrice(itemId);
        assertEquals(25.0, finalPrice, EPSILON);

        // Verify rich wallet
        assertEquals(10000.0 - 25.0, queryDouble("SELECT balance FROM users WHERE id = ?", rich), EPSILON);
        assertEquals(25.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", rich), EPSILON);

        // Verify poor wallet was not charged/frozen any money (since its bid of 20 was outbid and unfrozen, and 30 was rejected)
        assertEquals(22.0, queryDouble("SELECT balance FROM users WHERE id = ?", poor), EPSILON);
        assertEquals(0.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", poor), EPSILON);

        // Verify poor's auto-bid config is inactive
        com.auction.shared.model.payloads.AutoBidPayload status = bidService.getAutoBidStatus(itemId, poor);
        assertFalse(status.getIsActive());
    }

    @Test
    void settlementCannotDoubleChargeAndShouldNotRaceWithBidProcessing() throws Exception {
        String itemId = item("wallet-settle", 10.0, 5.0, AuctionStatus.ONGOING,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30), null);
        String seller = prefix + "-user-seller-wallet-settle";
        String bidder = user("settle-bidder");

        assertTrue(bidService.placeBid(itemId, bidder, 15.0));

        // Explicitly end the auction in DB
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE items SET status = ?, end_time = ? WHERE id = ?")) {
            stmt.setString(1, AuctionStatus.ENDED.name());
            stmt.setString(2, LocalDateTime.now().minusMinutes(5).toString());
            stmt.setString(3, itemId);
            stmt.executeUpdate();
        }

        AuctionSettlementService settlementService = new AuctionSettlementService();

        // 1. First settlement run
        AuctionSettlementService.SettlementResult result1 = settlementService.settleAuction(itemId);
        assertTrue(result1.success);
        assertTrue(result1.hadBids);
        assertEquals(bidder, result1.winnerId);
        assertEquals(15.0, result1.winningPrice, EPSILON);

        // Verify balances after first settlement
        assertEquals(10000.0 - 15.0, queryDouble("SELECT balance FROM users WHERE id = ?", bidder), EPSILON);
        assertEquals(0.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", bidder), EPSILON);
        assertEquals(10015.0, queryDouble("SELECT balance FROM users WHERE id = ?", seller), EPSILON);

        // 2. Second settlement run (idempotency check)
        AuctionSettlementService.SettlementResult result2 = settlementService.settleAuction(itemId);
        assertTrue(result2.success);
        assertTrue(result2.hadBids);

        // Verify balances remain unchanged (no double charge/credit!)
        assertEquals(10000.0 - 15.0, queryDouble("SELECT balance FROM users WHERE id = ?", bidder), EPSILON);
        assertEquals(0.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", bidder), EPSILON);
        assertEquals(10015.0, queryDouble("SELECT balance FROM users WHERE id = ?", seller), EPSILON);

        // 3. Test concurrent bid and settlement (race condition safety)
        String anotherBidder = user("another-bidder");
        List<Boolean> raceResults = runConcurrent(List.of(
                () -> bidService.placeBid(itemId, anotherBidder, 20.0),
                () -> {
                    settlementService.settleAuction(itemId);
                    return true;
                }
        ));

        assertFalse(raceResults.get(0));
        assertTrue(raceResults.get(1));
        assertEquals(15.0, currentPrice(itemId), EPSILON);
        assertEquals(bidder, currentBidder(itemId));
        assertEquals(1, bidCount(itemId));
        assertEquals(10000.0 - 15.0, queryDouble("SELECT balance FROM users WHERE id = ?", bidder), EPSILON);
        assertEquals(0.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", bidder), EPSILON);
        assertEquals(10015.0, queryDouble("SELECT balance FROM users WHERE id = ?", seller), EPSILON);
        assertEquals(10000.0, queryDouble("SELECT balance FROM users WHERE id = ?", anotherBidder), EPSILON);
        assertEquals(0.0, queryDouble("SELECT frozen_balance FROM users WHERE id = ?", anotherBidder), EPSILON);
    }
}
