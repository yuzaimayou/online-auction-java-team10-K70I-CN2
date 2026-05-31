package com.auction.client.util;

import com.auction.client.ui.image.ImageUtil;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientImageUtil {

    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    private ClientImageUtil() {}

    /**
     * Load ảnh từ server (có cache), gán vào ImageView.
     * Rendering cover dùng ImageUtil.makeResponsiveCover() riêng nếu cần.
     */
    public static void displayImage(String imageName, String source, ImageView imageView) {
        String imageUrl = buildUrl(source, imageName);
        Image fxImage = imageCache.computeIfAbsent(imageUrl, key -> {
            try {
                return new Image(key, true); // async load
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
        imageView.setSmooth(true);
        imageView.setImage(fxImage);
    }

    /**
     * Load ảnh + apply responsive cover layout cùng lúc.
     */
    public static void displayImageCover(String imageName, String source,
                                         ImageView imageView, Region container,
                                         double arcRadius) {
        displayImage(imageName, source, imageView);
        ImageUtil.makeResponsiveCover(imageView, container, arcRadius);
    }

    /**
     * Load ảnh có hint kích thước (requestedWidth/Height) để JavaFX
     * decode đúng resolution, tránh load ảnh quá lớn vào bộ nhớ.
     * Cache key vẫn là URL, nên cùng URL sẽ dùng lại ảnh đã load.
     */
    public static void displayImage(String imageName, String source,
                                    ImageView imageView,
                                    double requestedWidth, double requestedHeight) {
        String imageUrl = buildUrl(source, imageName);
        // Cache với key bao gồm kích thước để tránh dùng chung ảnh decode khác size
        String cacheKey = imageUrl + "@" + (int) requestedWidth + "x" + (int) requestedHeight;
        Image fxImage = imageCache.computeIfAbsent(cacheKey, key -> {
            try {
                return new Image(imageUrl, requestedWidth, requestedHeight, true, true, true);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
        imageView.setSmooth(true);
        imageView.setImage(fxImage);
    }

    /**
     * Áp dụng object-fit: cover cho ImageView — crop phần thừa,
     * giữ tỉ lệ, không stretch. Cần gọi lại mỗi khi Image thay đổi.
     *
     * @param imageView  view cần crop
     * @param image      ảnh đã load (không null)
     * @param viewWidth  chiều rộng hiển thị mục tiêu (px)
     * @param viewHeight chiều cao hiển thị mục tiêu (px)
     */
    public static void applyObjectFitCoverToImageView(ImageView imageView, Image image,
                                                      double viewWidth, double viewHeight) {
        if (image == null || image.getWidth() == 0 || image.getHeight() == 0) return;

        double imgW = image.getWidth();
        double imgH = image.getHeight();

        double scaleX = viewWidth  / imgW;
        double scaleY = viewHeight / imgH;
        double scale  = Math.max(scaleX, scaleY); // cover: lấy scale lớn hơn

        double visibleW = viewWidth  / scale;
        double visibleH = viewHeight / scale;
        double offsetX  = (imgW - visibleW) / 2.0;
        double offsetY  = (imgH - visibleH) / 2.0;

        imageView.setViewport(new Rectangle2D(offsetX, offsetY, visibleW, visibleH));
        imageView.setFitWidth(viewWidth);
        imageView.setFitHeight(viewHeight);
        imageView.setPreserveRatio(false);
    }

    /**
     * Thiết lập responsive cover: ImageView co giãn theo container,
     * crop kiểu cover, bo góc bằng clip.
     * Delegate sang ImageUtil để tái sử dụng logic clip/arc.
     */
    public static void makeResponsiveCover(ImageView imageView, Region container, double arcRadius) {
        ImageUtil.makeResponsiveCover(imageView, container, arcRadius);
    }

    public static void clearCache() {
        imageCache.clear();
    }

    // ── private ──────────────────────────────────────────────────────────────

    private static String buildUrl(String source, String imageName) {
        return String.format("%s/%s/%s", AppConfig.getStaticUrl(), source, imageName);
    }
}