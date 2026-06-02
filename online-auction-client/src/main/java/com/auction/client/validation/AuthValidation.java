package com.auction.client.validation;

/**
 * Lớp chịu trách nhiệm kiểm tra tính hợp lệ của dữ liệu xác thực đầu vào.
 */
public class AuthValidation {

    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    /**
     * Kiểm tra tính hợp lệ của dữ liệu đăng nhập.
     */
    public static String validateLogin(String username, String password) {
        if (isNullOrEmpty(username) || isNullOrEmpty(password)) {
            return "Username and password are required!";
        }
        return null;
    }

    /**
     * Kiểm tra tính hợp lệ của dữ liệu đăng ký tài khoản mới..
     */
    public static String validateRegister(String username, String email, String password, String confirmPassword) {
        if (isNullOrEmpty(username) || isNullOrEmpty(email) || isNullOrEmpty(password) || isNullOrEmpty(confirmPassword)) {
            return "Please fill in all fields.";
        }

        if (!email.trim().matches(EMAIL_REGEX)) {
            return "Invalid email address.";
        }

        if (!password.equals(confirmPassword)) {
            return "Passwords do not match.";
        }

        return null;
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}