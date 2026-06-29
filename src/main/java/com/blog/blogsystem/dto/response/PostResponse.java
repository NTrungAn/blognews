package com.blog.blogsystem.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
public class PostResponse {
    private UUID id;
    private String title;
    private String slug;
    private String summary;
    private String contentMarkdown;
    private String coverImage;
    private String status;
    private int viewCount;
    private int likesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Gộp thông tin rút gọn của Tác giả, Danh mục và Tag
    private String authorName; // Chỉ lấy tên tác giả
    private String authorUsername; // Username để link sang profile
    private CategoryDto category;
    private Set<TagDto> tags;

    @Data
    public static class CategoryDto {
        private UUID id;
        private String name;
        private String slug;
    }

    @Data
    public static class TagDto {
        private UUID id;
        private String name;
        private String slug;
    }
}
