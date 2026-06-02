package com.auction.client.service;

import com.auction.client.util.AppConfig;
import com.auction.shared.message.ResponseMessage;
import com.auction.shared.model.account.Admin;
import com.auction.shared.model.account.User;
import com.auction.shared.model.payloads.AuthPayload;
import com.auction.shared.model.payloads.VerifyPayload;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Xử lý các nghiệp vụ liên quan đến xác thực người dùng.
 * Singleton Pattern
 */
public class AuthService {

    /** Singleton pattern */
    private static AuthService instance;
    private final HttpClient httpClient;
    private final Gson gson;

    private AuthService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.gson = new Gson();
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }
    /* @return CompletableFuture chứa {@link ResponseMessage} phản hồi từ server */
    /**
     * Gửi yêu cầu đăng nhập hệ thống
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     */
    public CompletableFuture<ResponseMessage> login(String username, String password) {
        return postJsonAsync("/api/login", new AuthPayload(username, password));
    }

    /**
     * Gửi yêu cầu đăng ký tài khoản mới
     * @param username Tên đăng nhập mong muốn
     * @param password Mật khẩu mong muốn
     * @param email Địa chỉ email để nhận thông báo và mã xác thực
     */
    public CompletableFuture<ResponseMessage> register(String username, String password, String email) {
        return postJsonAsync("/api/register", new AuthPayload(username, password, email));
    }

    /**
     * Gửi yêu cầu xác thực tài khoản qua mã OTP bất đồng bộ.
     * @param email Địa chỉ email của tài khoản cần xác thực
     * @param otp Mã xác thực dùng một lần (OTP) gửi tới email
     */
    public CompletableFuture<ResponseMessage> verify(String email, String otp) {
        return postJsonAsync("/api/verify-account", new VerifyPayload(email, otp));
    }

    /**
     * Yêu cầu server gửi lại mã OTP đến email chỉ định.
     * @param email Địa chỉ email cần nhận mã OTP
     */
    public CompletableFuture<ResponseMessage> sendOtp(String email) {
        String url = String.format("%s/api/send-otp?email=%s", AppConfig.getHttpUrl(), email);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return executeRequest(request);
    }

    /**
     * Chuyển đổi dữ liệu thô từ server thành đối tượng User hoặc Admin tương ứng.
     * Phân tích dựa trên trường "role" có trong cấu trúc dữ liệu JSON.
     *
     * @param data Dữ liệu thô (Object) cần parse, thường được bóc tách từ ResponseMessage
     * @return Đối tượng {@link Admin} nếu role là Admin, ngược lại trả về đối tượng {@link User}
     */
    public User parseUser(Object data) {
        JsonElement jsonElement = gson.toJsonTree(data);
        JsonObject userData    = jsonElement.getAsJsonObject();

        String role = "";
        if (userData.has("role") && !userData.get("role").isJsonNull()) {
            role = userData.get("role").getAsString();
        }

        if ("Admin".equalsIgnoreCase(role)) {
            return gson.fromJson(jsonElement, Admin.class);
        }
        return gson.fromJson(jsonElement, User.class);
    }

    /**
     * Hàm trợ giúp: Chuyển đổi payload thành JSON và tạo yêu cầu HTTP POST.
     * @param endpoint Đường dẫn API
     * @param payload  Đối tượng chứa dữ liệu cần gửi
     * @return CompletableFuture chứa kết quả phản hồi
     */
    private CompletableFuture<ResponseMessage> postJsonAsync(String endpoint, Object payload) {
        String jsonPayload = gson.toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.getHttpUrl() + endpoint))
                .header("Content-Type", "application/json; utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return executeRequest(request);
    }

    /**
     * Hàm trợ giúp: Thực thi yêu cầu HTTP bất đồng bộ và tự động parse JSON sang Object.
     * @param request Yêu cầu HTTP đã được thiết lập
     * @return CompletableFuture chứa đối tượng {@link ResponseMessage}
     */
    private CompletableFuture<ResponseMessage> executeRequest(HttpRequest request) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), ResponseMessage.class));
    }
}