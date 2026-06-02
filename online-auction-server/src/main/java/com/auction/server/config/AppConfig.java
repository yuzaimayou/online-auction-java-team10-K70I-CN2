package com.auction.server.config;

import java.io.File;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class AppConfig {
    private static final String AI_SERVER_URL = "http://127.0.0.1:8000";

    public static final String DB_PATH = getDbPath();
    public static final String IMAGE_DIR = getImageDir();

    public static String getRootDir() {
        return System.getProperty("auth.dir") != null
                ? System.getProperty("auth.dir")
                : System.getProperty("user.dir");
    }

    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());

    public static String getDbPath() {
        return Paths.get(getRootDir(), "dataBase", "auction.db").toString();
    }

    public static String getImageDir() {
        return Paths.get(getRootDir(), "dataBase", "images").toString();
    }

    public static void initFolders() {
        File imgFolder = new File(getImageDir());
        if (!imgFolder.exists()) {
            imgFolder.mkdirs();
            LOGGER.info("Created image storage folder: " + imgFolder.getAbsolutePath());
        }
    }

    public static String getAiServerUrl() {
        return AI_SERVER_URL;
    }
}
