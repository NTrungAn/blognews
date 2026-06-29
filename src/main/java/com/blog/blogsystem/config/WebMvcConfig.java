package com.blog.blogsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Cấu hình Spring MVC để expose thư mục uploads/ ra ngoài HTTP.
 *
 * Ảnh được lưu tại: <uploadDir>/images/<uuid>.jpg
 * Truy cập qua URL: http://localhost:8080/uploads/images/<uuid>.jpg
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Chuyển URL pattern /uploads/** sang thư mục vật lý trên đĩa
        String absoluteUploadPath = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absoluteUploadPath);
    }
}
