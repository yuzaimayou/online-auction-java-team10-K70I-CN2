package com.auction.client.controller.user;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class VerifyController {
    @FXML
    private TextField txtCode1;
    @FXML
    private TextField txtCode2;
    @FXML
    private TextField txtCode3;
    @FXML
    private TextField txtCode4;
    @FXML
    private TextField txtCode5;
    @FXML
    private TextField txtCode6;


    @FXML
    public void initialize() {
        addAutoJump(txtCode1, txtCode2, null);
        addAutoJump(txtCode2, txtCode3, txtCode1);
        addAutoJump(txtCode3, txtCode4, txtCode2);
        addAutoJump(txtCode4, txtCode5, txtCode3);
        addAutoJump(txtCode5, txtCode6, txtCode4);
        addAutoJump(txtCode6, null, txtCode5);
    }

    private void addAutoJump(TextField current, TextField next, TextField previous) {
        current.textProperty().addListener((obs, oldVal, newVal) -> {
            // 1. Bổ sung: Chặn không cho nhập chữ cái, chỉ cho phép nhập số
            if (!newVal.matches("\\d*")) {
                current.setText(newVal.replaceAll("[^\\d]", ""));
                return;
            }

            // 2. GIẢI QUYẾT YÊU CẦU CỦA BẠN: Nếu chuỗi dài hơn 1 ký tự, ép nó về 1 ký tự đầu tiên
            if (newVal.length() > 1) {
                current.setText(newVal.substring(0, 1));
                return; // Lệnh setText ở trên sẽ tự động gọi lại Listener, nên ta return luôn tại đây
            }
            // Nếu đã nhập 1 ký tự và có ô tiếp theo
            if (newVal.length() == 1 && next != null) {
                next.requestFocus();
            }
        });

        current.setOnKeyPressed(event -> {
            // Nếu bấm xóa khi ô đang trống và có ô phía trước
            if (event.getCode() == KeyCode.BACK_SPACE && current.getText().isEmpty() && previous != null) {
                previous.requestFocus();
            }
        });
    }

    @FXML
    public void handleVerify(ActionEvent event) {

    }

    @FXML
    public void handleResend(ActionEvent event) {

    }
}
