package com.blog.blogsystem.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CategoryResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;

    /** Số lượng bài viết thuộc chuyên mục này. */
    private long postCount;

    /** Thời điểm tạo chuyên mục. */
    private LocalDateTime createdAt;

    /** Thời điểm cập nhật gần nhất. */
    private LocalDateTime updatedAt;
}

