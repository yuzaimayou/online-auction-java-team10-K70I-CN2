package com.auction.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;

public class StaticFileServer {
    private static final String imageDir = System.getProperty("user.dir") + File.separator + "dataBase" + File.separator + "images" + File.separator;

    public void startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(1401), 0);
            server.createContext("/images", new ImageHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("HTTP File Server đang chạy tại: http://localhost:1401/images/");
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    static class ImageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {

                String requestURI = exchange.getRequestURI().getPath();

                String fileName = requestURI.substring(requestURI.lastIndexOf("/") + 1);
                File imageFile = new File(imageDir + fileName);

                System.out.println("Client yêu cầu ảnh. Đang tìm tại: " + imageFile.getAbsolutePath());
                if (imageFile.exists() && !imageFile.isDirectory()) {
                    String mimeType;
                    if (fileName.endsWith(".png")) {
                        mimeType = "image/png";
                    } else if (fileName.endsWith(".gif")) {
                        mimeType = "image/gif";
                    } else if (fileName.endsWith(".jpg")) {
                        mimeType = "image/jpg";
                    } else if (fileName.endsWith(".heic")) {
                        mimeType = "image/heic";

                    } else if (fileName.endsWith(".jpeg")) {
                        mimeType = "image/jpeg";
                    } else {
                        System.out.println("filetype is invalid");
                        System.out.println(fileName);
                        return;
                    }
                    exchange.getResponseHeaders().set("Content-Type", mimeType);
                    // 2. Lấy dung lượng file chuẩn xác
                    long fileLength = imageFile.length();

                    if (fileLength == 0) {
                        // Đề phòng file ảnh rỗng bị lỗi mạng
                        exchange.sendResponseHeaders(200, -1);
                    } else {
                        // Hứa với trình duyệt sẽ gửi đúng từng này byte
                        exchange.sendResponseHeaders(200, fileLength);

                        // 3. Dùng Files.copy (Chuẩn công nghiệp): Bơm thẳng dữ liệu từ ổ cứng ra mạng
                        // Cách này an toàn và chống tràn RAM cực tốt so với readAllBytes()
                        try (OutputStream os = exchange.getResponseBody()) {
                            Files.copy(imageFile.toPath(), os);
                        }
                    }
                    System.out.println("-> Đã gửi ảnh thành công!");
                } else {
                    String response = "404 (Not found)\n";
                    exchange.sendResponseHeaders(404, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.length());
                    os.close();
                    System.out.println("-> Đã gửi ảnh that bai!");
                }
            } catch (Exception e) {
                System.err.println("Lỗi nghiêm trọng trong lúc xử lý ảnh:");
                e.printStackTrace();
            } finally {
                exchange.close();
            }
        }
    }
}
