package com.auction.client.service;

import com.auction.shared.model.account.User;
import com.auction.shared.model.item.Item;

public class BidValidationService {

    public record ValidationResult(
            boolean ok,
            String errorMessage
    ) {}

    public ValidationResult validate(
            Item item,
            User user,
            double bidAmount,
            boolean ongoing
    ) {

        if (!ongoing) {
            return new ValidationResult(
                    false,
                    "Auction is not active."
            );
        }

        if (user.getId().equals(
                item.getCurrentTopPLayerId())) {

            return new ValidationResult(
                    false,
                    "You are already highest bidder."
            );
        }

        double minRequired =
                item.getCurrentPrice()
                        + item.getBidStep();

        if (bidAmount < minRequired) {

            return new ValidationResult(
                    false,
                    String.format(
                            "Bid must be at least $ %.1f",
                            minRequired
                    )
            );
        }
        return new ValidationResult(
                true,
                null
        );
    }
}