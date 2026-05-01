package com.auction.client.controller.setting;

import com.auction.client.service.NetworkService;
import com.auction.client.util.NavigationUtil;
import com.auction.shared.util.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class ProductEditController {
    @FXML
    private Label lblMessage;
    @FXML
    private TextField txtProductName;
    @FXML
    private ToggleGroup categoryGroup;
    @FXML
    private DatePicker startDateP;
    @FXML
    private DatePicker endDateP;
    @FXML
    private ImageView imageViewProduct;
    @FXML
    private ComboBox<String> cbStartTime;
    @FXML
    private ComboBox<String> cbEndTime;
    @FXML
    private TextArea txtProductDesc;
    @FXML
    private TextField txtInitPrice;
    @FXML
    private TextField txtBidStep;
    @FXML
    private TextField txtMaxPrice;
    @FXML
    private TextField txtMinPrice;

    @FXML
    private Button btnChooseImage;
    private String currentProductId; // ID của sản phẩm đang sửa
    private final List<String> listImagesBase64 = new ArrayList<>(); // Danh sách chuỗi ảnh Base64
    private final NetworkService network = NetworkService.getInstance();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    @FXML
    public void handleClose() {
        NavigationUtil.handleSwitchToSetting(lblMessage, "myAuctions");
    }

}
