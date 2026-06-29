package com.blog.blogsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private UUID id;
    private String type;
    private String content;
    private String targetUrl;
    private boolean isRead;
    private LocalDateTime createdAt;
    private UserResponse actor; // Optional actor details
}
