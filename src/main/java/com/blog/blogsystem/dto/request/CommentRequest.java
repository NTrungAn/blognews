package com.blog.blogsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CommentRequest {

    @NotBlank(message = "Nội dung bình luận không được để trống")
    @Size(max = 5000, message = "Nội dung bình luận không được vượt quá 5000 ký tự")
    private String content;

    // ID bài viết cần bình luận
    // (Không có authorId – lấy từ SecurityContextHolder)
    private UUID parentId; // null = bình luận gốc, có giá trị = reply
}
