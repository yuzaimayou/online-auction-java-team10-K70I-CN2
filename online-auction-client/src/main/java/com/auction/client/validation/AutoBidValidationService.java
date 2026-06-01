package com.auction.client.validation;

import com.auction.shared.model.item.Item;

public class AutoBidValidationService {

    public record ValidationResult(boolean ok, String errorMessage) {
        public static ValidationResult success() { return new ValidationResult(true, null); }
        public static ValidationResult fail(String msg) { return new ValidationResult(false, msg); }
    }

    /**
     * Kiểm tra logic nghiệp vụ trước khi kích hoạt Auto-Bid
     */
    public ValidationResult validate(Item item, double max, double step) {
        if (step <= 0) {
            return ValidationResult.fail("Increment amount must be greater than 0.");
        }
        if (step < item.getBidStep()) {
            return ValidationResult.fail(
                    String.format("Your increment step must be at least the minimum allowed ($ %.0f).", item.getBidStep()));
        }
        if (max <= item.getCurrentPrice()) {
            return ValidationResult.fail(
                    String.format("Max Bid ($ %.0f) must be higher than the Current Price ($ %.0f).", max, item.getCurrentPrice()));
        }
        if (step >= max) {
            return ValidationResult.fail(
                    String.format("Increment step ($ %.0f) cannot be equal or greater than your Max Bid ($ %.0f).", step, max));
        }
        double firstAutoBidPrice = item.getCurrentPrice() + step;
        if (firstAutoBidPrice > max) {
            return ValidationResult.fail(
                    String.format("The first auto-bid will be $ %.0f, which exceeds your Max Bid ($ %.0f). Please raise Max Bid or lower increment.",
                            firstAutoBidPrice, max));
        }
        return ValidationResult.success();
    }
}