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

    /** Phản hồi của người dùng hiện tại (nếu có) */
    private String userReaction;

    /** URL ảnh đính kèm (nếu có) */
    private String imageUrl;

    /** Số lần bị báo cáo vi phạm */
    private int reportCount;

    /** Danh sách lý do báo cáo chi tiết (phục vụ Admin) */
    private List<CommentReportResponse> reports;

    /** Thông tin rút gọn của bài viết chứa bình luận */
    private PostInfoDto post;

    @Data
    public static class PostInfoDto {
        private UUID id;
        private String title;
        private String slug;
    }

    @Data
    public static class AuthorDto {
        private UUID id;
        private String username;
        private String fullName;
    }
}
