package com.blog.blogsystem.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CommentResponse {

    private UUID id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Thông tin tác giả bình luận (rút gọn) */
    private AuthorDto author;

    /** ID bình luận cha (null nếu là bình luận gốc) */
    private UUID parentId;

    /** Danh sách reply con (đệ quy 1 cấp để tránh N+1 quá sâu) */
    private List<CommentResponse> replies;

    /** Đếm số lượng từng biểu tượng cảm xúc */
    private java.util.Map<String, Long> reactionsCount;

    @Data
    public static class AuthorDto {
        private UUID id;
        private String username;
        private String fullName;
    }
}
