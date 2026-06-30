package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.response.CommentResponse;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller dành riêng cho các tác vụ quản trị bình luận của Admin.
 */
@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommentController {

    private final CommentService commentService;

    /**
     * Lấy danh sách toàn bộ bình luận trong hệ thống (phân trang, hỗ trợ tìm kiếm).
     */
    @GetMapping
    public ResponseEntity<PageResponse<CommentResponse>> getAllComments(
            @RequestParam(value = "pageNo", defaultValue = "0", required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10", required = false) int pageSize,
            @RequestParam(value = "keyword", defaultValue = "", required = false) String keyword
    ) {
        return ResponseEntity.ok(commentService.getAllCommentsForAdmin(keyword, pageNo, pageSize));
    }

    /**
     * Lấy danh sách bình luận bị báo cáo vi phạm (reportCount > 0).
     */
    @GetMapping("/reported")
    public ResponseEntity<PageResponse<CommentResponse>> getReportedComments(
            @RequestParam(value = "pageNo", defaultValue = "0", required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10", required = false) int pageSize,
            @RequestParam(value = "keyword", defaultValue = "", required = false) String keyword
    ) {
        return ResponseEntity.ok(commentService.getReportedComments(keyword, pageNo, pageSize));
    }

    /**
     * Bỏ qua báo cáo vi phạm đối với một bình luận.
     */
    @PutMapping("/{commentId}/dismiss")
    public ResponseEntity<String> dismissCommentReport(@PathVariable UUID commentId) {
        commentService.dismissCommentReport(commentId);
        return ResponseEntity.ok("Bỏ qua báo cáo bình luận thành công!");
    }

    /**
     * Xóa bình luận bất kỳ trong hệ thống.
     * Vì là Admin nên không cần check quyền sở hữu trong Service,
     * tuy nhiên, Service.deleteComment yêu cầu truyền username,
     * nếu là ADMIN thì sẽ được cho phép xóa trong logic của deleteComment.
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable UUID commentId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        commentService.deleteComment(commentId, currentUsername);
        return ResponseEntity.ok("Xóa bình luận thành công!");
    }
}
