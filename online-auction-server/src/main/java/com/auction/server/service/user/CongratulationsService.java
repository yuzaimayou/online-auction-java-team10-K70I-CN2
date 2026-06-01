package com.auction.server.service.user;

import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import com.auction.shared.model.account.User;

public class CongratulationsService {
    private static final CongratulationsService instance = new CongratulationsService();
    private final EmailService emailService = EmailService.getInstance();
    private final UserRepository userRepository = new UserRepository();
    private final ItemRepository itemRepository = ItemRepository.getInstance();

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
}
