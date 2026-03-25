package com.auction.server;

import com.auction.shared.User;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuthService {
    private List<User> userDatabase;
    private final String FILE_PATH = "users.txt";

    public AuthService() {
        this.userDatabase = new ArrayList<>();
        loadUsersFromFile(); // Load dữ liệu từ file khi khởi tạo
    }

    // Đọc dữ liệu từ file txt lên List
    private void loadUsersFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return; // Nếu file chưa có thì bỏ qua

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length == 4) {
                    userDatabase.add(new User(data[0], data[1], data[2], data[3]));
                }
            }
        } catch (IOException e) {
            System.out.println("Lỗi đọc file: " + e.getMessage());
        }
    }

    // Ghi một user mới vào cuối file txt
    private void saveUserToFile(User user) {
        try (FileWriter fw = new FileWriter(FILE_PATH, true); // true để ghi tiếp vào cuối file
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(user.getId() + "," + user.getUsername() + "," + user.getPassword() + "," + user.getRole());
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Lỗi ghi file: " + e.getMessage());
        }
    }

    // Logic Đăng ký (Cập nhật để lưu file)
    public boolean register(String username, String password, String role) {
        for (User u : userDatabase) {
            if (u.getUsername().equals(username)) return false; // Tài khoản đã tồn tại
        }

        User newUser = new User(UUID.randomUUID().toString(), username, password, role);
        userDatabase.add(newUser);
        saveUserToFile(newUser); // Lưu ngay vào file
        return true;
    }

    // Logic Đăng nhập (Giữ nguyên)
    public User login(String username, String password) {
        for (User u : userDatabase) {
            if (u.getUsername().equals(username) && u.getPassword().equals(password)) {
                return u;
            }
        }
        return null;
    }
}