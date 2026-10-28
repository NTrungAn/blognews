package com.blog.blogsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Thông tin cấu hình Cloudinary, được bind từ các key "cloudinary.*" trong application.properties.
 *
 * Giá trị thật KHÔNG được commit lên Git – chỉ khai báo trong application-local.properties
 * hoặc truyền qua biến môi trường (CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET).
 *
 * @param cloudName Tên cloud trên Dashboard Cloudinary
 * @param apiKey    API Key
 * @param apiSecret API Secret
 * @param folder    Thư mục chứa ảnh trên Cloudinary (ví dụ: blognews/images)
 */
@ConfigurationProperties(prefix = "cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        String folder
) {
}
