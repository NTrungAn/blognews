package com.blog.blogsystem.config;

import com.cloudinary.Cloudinary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;

/**
 * Khởi tạo Cloudinary client.
 *
 * Chỉ được kích hoạt khi app.storage.type=cloudinary,
 * nên khi dev với chế độ local sẽ không cần khai báo khóa Cloudinary.
 */
@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "cloudinary")
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        String cloudName = cleanValue(requireValue(properties.cloudName(), "CLOUDINARY_CLOUD_NAME"));
        String apiKey = cleanValue(requireValue(properties.apiKey(), "CLOUDINARY_API_KEY"));
        String apiSecret = cleanValue(requireValue(properties.apiSecret(), "CLOUDINARY_API_SECRET"));

        return new Cloudinary(Map.of(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    /**
     * Fail-fast khi khởi động nếu thiếu khóa, thay vì lỗi mơ hồ lúc upload ảnh.
     */
    private String requireValue(String value, String envName) {
        return Optional.ofNullable(value)
                .filter(v -> !v.isBlank())
                .orElseThrow(() -> new IllegalStateException(
                        "Thiếu cấu hình Cloudinary: " + envName
                                + ". Hãy khai báo trong application-local.properties hoặc biến môi trường."));
    }

    /**
     * Loại bỏ khoảng trắng thừa và dấu nháy kép/đơn bao quanh do copy-paste.
     */
    private String cleanValue(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            if (trimmed.length() >= 2) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed;
    }
}
