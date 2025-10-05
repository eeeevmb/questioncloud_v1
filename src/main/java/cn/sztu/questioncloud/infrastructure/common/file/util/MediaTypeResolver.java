package cn.sztu.questioncloud.infrastructure.common.file.util;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * 媒体类型解析器
 */
@Component
public class MediaTypeResolver {

    /**
     * 根据文件名解析 MediaType
     */
    public MediaType resolve(String filename) {
        if (filename == null || filename.isEmpty()) {
            return MediaType.IMAGE_JPEG;
        }

        String lower = filename.toLowerCase();

        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        } else if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        } else if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        } else if (lower.endsWith(".svg")) {
            return MediaType.parseMediaType("image/svg+xml");
        }

        return MediaType.IMAGE_JPEG;  // 默认
    }
}