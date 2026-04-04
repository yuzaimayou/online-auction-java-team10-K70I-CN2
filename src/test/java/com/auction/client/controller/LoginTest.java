package com.auction.client.controller;

import com.auction.client.service.NetworkService;
import com.auction.shared.message.RequestMessage;
import com.auction.shared.model.enums.ActionType;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

public class LoginTest {
    private LoginController controller;
    private NetworkService mockNetwork;

    @BeforeAll
    public static void initJFX() {
        Platform.startup(() -> {
        });
    }

    @BeforeEach
    public void setUp() throws Exception {
        // Thêm throws Exception vào signature của hàm vì Reflection có thể ném ra NoSuchFieldException

        // 1. Khởi tạo Controller và tạo đối tượng mạng giả
        controller = new LoginController();
        mockNetwork = Mockito.mock(NetworkService.class);

        // 2. Dùng Reflection để can thiệp và gán mạng giả vào 'network'
        Field networkField = LoginController.class.getDeclaredField("network");
        networkField.setAccessible(true);
        networkField.set(controller, mockNetwork);

        // 3. Xử lý các biến giao diện @FXML (nếu không gán, chúng sẽ bị null)
        // Bơm TextField username giả
        Field txtUsernameField = LoginController.class.getDeclaredField("txtUsername");
        txtUsernameField.setAccessible(true);
        TextField mockUserField = new TextField();
        mockUserField.setText("admin");
        txtUsernameField.set(controller, mockUserField);

        // Bơm PasswordField giả
        Field txtPasswordField = LoginController.class.getDeclaredField("txtPassword");
        txtPasswordField.setAccessible(true);
        PasswordField mockPassField = new PasswordField();
        mockPassField.setText("admin"); // Giả lập người dùng đã gõ pass
        txtPasswordField.set(controller, mockPassField);

        // Bơm Label thông báo giả
        Field lblMessageField = LoginController.class.getDeclaredField("lblMessage");
        lblMessageField.setAccessible(true);
        lblMessageField.set(controller, new Label());

    }

    @Test
    public void testSendLoginRequest() {
        controller.handleLogin(null);
        // Tạo một cái "lưới" chuyên đi bắt các object chuẩn bị chui vào lớp RequestMessage
        ArgumentCaptor<RequestMessage> captor = ArgumentCaptor.forClass(RequestMessage.class);
        // Xác nhận là mạng giả đã được gọi, đồng thời quăng lưới ra bắt lấy tham số truyền vào
        verify(mockNetwork).sendRequest(captor.capture());
        // Lấy "chiến lợi phẩm" vừa bắt được ra ngoài
        RequestMessage sentMessage = captor.getValue();
        assertEquals(ActionType.LOGIN, sentMessage.getAction(), "Hành động gửi đi phải là LOGIN");
        String expectedJson = "{\"username\":\"admin\",\"password\":\"admin\"}";
        System.out.println("Dữ liệu thực tế gửi đi: " + sentMessage.getPayload());
        assertEquals(expectedJson, sentMessage.getPayload(), "Chuỗi JSON đóng gói sai thông tin");

    }
}
