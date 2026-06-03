package com.auction.server.service.user;

import com.auction.server.http.handler.GetUsersHandler;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import com.auction.shared.model.account.User;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CongratulationsService {
    private static final CongratulationsService instance = new CongratulationsService();
    private final EmailService emailService = EmailService.getInstance();
    private final UserRepository userRepository = new UserRepository();
    private final ItemRepository itemRepository = ItemRepository.getInstance();
    private static final Logger LOGGER = Logger.getLogger(GetUsersHandler.class.getName());

    public static CongratulationsService getInstance() {
        return instance;
    }

    public void sendCongratulationsEmail(String userId, String itemId) throws Exception {
        User user = userRepository.findById(userId);
        String email = user.getEmail();

        emailService.sendHtmlEmail(
                email,
                "Congratulations on Winning the Auction!",
                buildCongratulationsHtml(itemId)
        );
    }

    public void sendSellerAuctionSuccessEmail(
            String sellerId,
            String winnerId,
            String itemId,
            double finalPrice
    ) {
        try {
            User seller = userRepository.findById(sellerId);
            User winner = userRepository.findById(winnerId);

            if (seller == null || seller.getEmail() == null) {
                return;
            }

            String email = seller.getEmail();

            emailService.sendHtmlEmail(
                    email,
                    "Your Auction Has Ended Successfully!",
                    buildSellerAuctionSuccessHtml(itemId, winner, finalPrice)
            );

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send seller auction success email", e);
        }
    }

    private String buildCongratulationsHtml(String itemId) {
        String itemName = itemRepository.findItemSummaryById(itemId).getName();
        return String.format("""
                <div style='font-family: Arial, sans-serif; background-color: #f3f4f6; padding: 40px 20px;'>
                    <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 40px; border-radius: 12px;'>
                        <h2 style='color: #1e3a8a; text-align: center;'>ONLINE AUCTION</h2>
                        <p>Congratulations!</p>
                        <p>You have won the auction for the item: <strong>%s</strong>.</p>
                        <p>Please log in to your account to view the details and complete the purchase.</p>
                    </div>
                </div>
                """, itemName);
    }

    private String buildSellerAuctionSuccessHtml(
            String itemId,
            User winner,
            double finalPrice
    ) {
        String itemName = itemRepository.findItemSummaryById(itemId).getName();

        String winnerName = winner != null ? winner.getUsername() : "Unknown winner";

        return String.format("""
                <div style="font-family: Arial, sans-serif; background-color: #f4f6f8; padding: 30px;">
                    <div style="max-width: 600px; margin: 0 auto; background: #ffffff; padding: 24px; border-radius: 10px;">
                        <h2 style="color: #1e3a8a; text-align: center;">
                            Your Auction Has Ended Successfully!
                        </h2>
                
                        <p>Hello,</p>
                
                        <p>Your auction for the item 
                            <strong>%s</strong> has ended successfully.
                        </p>
                
                        <p>
                            Winner: <strong>%s</strong>
                        </p>
                
                        <p>
                            Final price: <strong>%,.0f</strong>
                        </p>
                
                        <p>
                            The payment has been processed and the winning amount has been added to your wallet.
                        </p>
                
                        <p style="margin-top: 24px;">
                            Please log in to your account to view the auction details.
                        </p>
                    </div>
                </div>
                """, itemName, winnerName, finalPrice);
    }
}
