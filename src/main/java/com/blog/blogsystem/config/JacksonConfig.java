package com.blog.blogsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * Cấu hình Jackson ObjectMapper cho Spring Boot 4.
 *
 * Spring Boot 4 sử dụng Jackson 3.x (package: tools.jackson)
 * thay vì Jackson 2.x (package: com.fasterxml.jackson).
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // Jackson 3.x ObjectMapper với cấu hình mặc định
        // Tự động hỗ trợ Java 8 Date/Time và format ISO 8601
        return new ObjectMapper();
    }
}
