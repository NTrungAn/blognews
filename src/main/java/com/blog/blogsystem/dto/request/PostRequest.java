package com.blog.blogsystem.dto.request;

import com.blog.blogsystem.entity.enums.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Set;
import java.util.UUID;

@Data
public class PostRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String summary;

    @NotBlank(message = "Nội dung Markdown không được để trống")
    private String contentMarkdown;

    private String coverImage;

    @NotNull(message = "Danh mục không được để trống")
    private UUID categoryId;

    // Chỉ cần gửi danh sách ID của các Tag
    private Set<UUID> tagIds;

    private PostStatus status;
}