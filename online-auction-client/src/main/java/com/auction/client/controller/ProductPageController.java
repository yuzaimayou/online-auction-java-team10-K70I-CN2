package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.client.util.ClientImageUtil;
import com.auction.client.util.UserSession;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.account.User;
import com.auction.shared.model.product.Item;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;


public class ProductPageController implements NetworkService.MessageListener {
    private Item item;
    @FXML
    private Label productNameLabel;
    @FXML
    private Label productDesLabel;
    @FXML
    private ImageView productImage;
    @FXML
    private Label sellerLabel;
    @FXML
    private Label currentPriceLabel;
    @FXML
    private Label startPriceLabel;
    @FXML
    private Label bidStepLabel;
    @FXML
    private Label startTimeLabel;
    @FXML
    private Label endTimeLabel;
    @FXML
    private TextField bidAmountField;

    private final User user = UserSession.getInstance().getLoggedInUser();

    private NetworkService network = NetworkService.getInstance();
    private Gson gson = new Gson();

    public void initData(Item item) {
        this.item = item;
        displayDataProduct(item);
        connectToRealTimeBidding();
    }

    private void connectToRealTimeBidding() {
        network.setListener(this);

        String userId = UserSession.getInstance().getLoggedInUser().getId();
        String itemId = item.getId();

        boolean connected = network.connectToAuctionRoom(userId, itemId);
        if (connected) {
            System.out.println("Connected to auction room for item: " + itemId);
        } else {
            System.err.println("Failed to connect to auction room for item: " + itemId);
        }
    }

    public void initialize() {

    }

    @Override
    public void onMessageReceived(ResponseMessage response) {
        System.out.println(response);
        javafx.application.Platform.runLater(() -> {
            if ("success".equals(response.getStatus()) && "NEW_BID".equals(response.getMessage())) {
                String jsonPayload = response.getData();
                Item updatedItem = gson.fromJson(jsonPayload, Item.class);
                if (updatedItem != null) {
                    displayDataProduct(updatedItem);
                } else {
                    System.err.println("Failed to parse updated item from response: " + jsonPayload);
                }
            } else {

                System.out.println("Received message: " + response.getMessage());
            }
        });

    }

    public void bidHandle() {
        String inputAmount = bidAmountField.getText().trim();

        if (inputAmount.isEmpty()) {
            System.out.println("Please enter a bid amount.");
            return;
        }
        try {
            double bidAmount = Double.parseDouble(inputAmount);
            String roomId = item.getId();

            network.sendBid(roomId, user.getId(), bidAmount, "");
            System.out.println("Bid sent: " + bidAmount);
        } catch (NumberFormatException e) {
            System.out.println("Invalid bid amount. Please enter a numeric value.");
        }

    }

    private void displayDataProduct(Item item) {
        ClientImageUtil.displayImage(item.getImagePath(), "images", productImage);
        productDesLabel.setText(item.getDescription());
        productNameLabel.setText(item.getName());
        sellerLabel.setText(item.getSellerId());
        currentPriceLabel.setText(String.format("Current bid: %.0f", item.getCurrentPrice()));
        startPriceLabel.setText(String.valueOf(item.getStartingPrice()));
        bidStepLabel.setText(String.valueOf(item.getBidStep()));
        startTimeLabel.setText(String.valueOf(item.getStartTime()));
        endTimeLabel.setText(String.valueOf(item.getEndTime()));

    }


}
