package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.request.CommentRequest;
import com.blog.blogsystem.dto.response.CommentResponse;
import com.blog.blogsystem.dto.response.PageResponse;

import java.util.UUID;

public interface CommentService {

    /**
     * Tạo bình luận mới cho một bài viết.
     * authorUsername được trích xuất từ JWT Token – KHÔNG nhận từ request body.
     *
     * @param postId         ID bài viết
     * @param request        Nội dung bình luận + parentId (nếu là reply)
     * @param authorUsername Username của người đang đăng nhập (từ
     *                       SecurityContextHolder)
     */
    CommentResponse createComment(UUID postId, CommentRequest request, String authorUsername);

    /**
     * Cập nhật nội dung bình luận.
     * Chỉ chủ bình luận mới được phép sửa.
     */
    CommentResponse updateComment(UUID commentId, CommentRequest request, String currentUsername);

    /**
     * Xóa bình luận.
     * Chủ bình luận hoặc Admin được xóa.
     */
    void deleteComment(UUID commentId, String currentUsername);

    /**
     * Lấy danh sách bình luận gốc (top-level) của một bài viết, phân trang.
     * Mỗi bình luận gốc đã bao gồm danh sách replies.
     */
    PageResponse<CommentResponse> getCommentsByPost(UUID postId, int pageNo, int pageSize);

    /**
     * Thêm hoặc cập nhật biểu tượng cảm xúc cho bình luận.
     */
    void addReaction(UUID commentId, String currentUsername, String emoji);

    /**
     * Xóa biểu tượng cảm xúc khỏi bình luận.
     */
    void removeReaction(UUID commentId, String currentUsername);

    /**
     * Lấy toàn bộ danh sách bình luận (phục vụ Admin), có hỗ trợ tìm kiếm và phân trang.
     */
    PageResponse<CommentResponse> getAllCommentsForAdmin(String keyword, int pageNo, int pageSize);

    /**
     * Báo cáo một bình luận vi phạm với lý do cụ thể.
     */
    void reportComment(UUID commentId, com.blog.blogsystem.dto.request.CommentReportRequest request, String reporterUsername);

    /**
     * Bỏ qua các báo cáo vi phạm của một bình luận
     */
    void dismissCommentReport(UUID commentId);

    /**
     * Lấy danh sách bình luận bị báo cáo (report_count > 0)
     */
    PageResponse<CommentResponse> getReportedComments(String keyword, int pageNo, int pageSize);
}
