package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.request.CommentRequest;
import com.blog.blogsystem.dto.response.CommentResponse;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller cho tính năng Bình luận (Comments).
 *
 * Các endpoint được đặt lồng dưới /api/posts/{postId}/comments
 * để thể hiện rõ ràng bình luận thuộc về một bài viết cụ thể.
 *
 * ⚠️ BẢO MẬT QUAN TRỌNG:
 * KHÔNG bao giờ lấy authorId / userId từ request body.
 * Tất cả các thao tác tạo / sửa / xóa đều trích xuất
 * username từ SecurityContextHolder (JWT Token).
 */
@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // ────────────────────────────────────────────────
    // Public: Đọc bình luận không cần đăng nhập
    // ────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<PageResponse<CommentResponse>> getCommentsByPost(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId, pageNo, pageSize));
    }

    // ────────────────────────────────────────────────
    // Protected: Cần JWT Token hợp lệ
    // ────────────────────────────────────────────────

    /**
     * Tạo bình luận mới hoặc reply cho một bài viết.
     * - Nếu request.parentId == null → bình luận gốc
     * - Nếu request.parentId != null → reply lồng nhau
     */
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CommentRequest request) {
        // ✅ Trích xuất username từ JWT Token thông qua SecurityContextHolder
        // KHÔNG nhận authorId từ request body
        String currentUsername = getCurrentUsername();

        CommentResponse response = commentService.createComment(postId, request, currentUsername);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Cập nhật nội dung bình luận.
     * Chỉ chủ bình luận mới được phép sửa.
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequest request) {
        // ✅ Trích xuất từ JWT – Service sẽ kiểm tra quyền sở hữu
        String currentUsername = getCurrentUsername();

        CommentResponse response = commentService.updateComment(commentId, request, currentUsername);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa bình luận.
     * Chỉ chủ bình luận hoặc Admin được xóa.
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<String> deleteComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId) {
        // ✅ Trích xuất từ JWT – Service sẽ kiểm tra quyền sở hữu
        String currentUsername = getCurrentUsername();

        commentService.deleteComment(commentId, currentUsername);
        return ResponseEntity.ok("Xóa bình luận thành công!");
    }

    /**
     * Thêm hoặc cập nhật biểu tượng cảm xúc cho bình luận.
     */
    @PostMapping("/{commentId}/reactions")
    public ResponseEntity<Void> addReaction(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @Valid @RequestBody com.blog.blogsystem.dto.request.ReactionRequest request) {
        String currentUsername = getCurrentUsername();
        commentService.addReaction(commentId, currentUsername, request.getEmoji());
        return ResponseEntity.ok().build();
    }

    /**
     * Xóa biểu tượng cảm xúc khỏi bình luận.
     */
    @DeleteMapping("/{commentId}/reactions")
    public ResponseEntity<Void> removeReaction(
            @PathVariable UUID postId,
            @PathVariable UUID commentId) {
        String currentUsername = getCurrentUsername();
        commentService.removeReaction(commentId, currentUsername);
        return ResponseEntity.ok().build();
    }

    // ────────────────────────────────────────────────
    // Private helper
    // ────────────────────────────────────────────────

    /**
     * Lấy username của người dùng đang đăng nhập từ SecurityContextHolder.
     * Đây là cách duy nhất đúng để xác định danh tính người gọi API.
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
