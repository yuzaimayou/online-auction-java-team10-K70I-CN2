package com.auction.server.config;

import java.io.File;
import java.nio.file.Paths;

public class AppConfig {
    private static final String ROOT_DIR = System.getProperty("user.dir");
    private static final String AI_SERVER_URL = "http://127.0.0.1:8000";

    public static final String DB_PATH = Paths.get(ROOT_DIR, "dataBase", "auction.db").toString();
    public static final String IMAGE_DIR = Paths.get(ROOT_DIR, "dataBase", "images").toString();

    public static void initFolders() {
        File imgFolder = new File(IMAGE_DIR);
        if (!imgFolder.exists()) {
            imgFolder.mkdirs();
            System.out.println("✅ Đã tạo thư mục lưu trữ: " + imgFolder.getAbsolutePath());
        }
    }

    public static String getAiServerUrl() {
        return AI_SERVER_URL;
    }

}
