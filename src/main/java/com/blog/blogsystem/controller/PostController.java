package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.request.PostRequest;
import com.blog.blogsystem.dto.request.SuggestContentRequest;
import jakarta.validation.Valid;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.dto.response.PostResponse;
import com.blog.blogsystem.service.FileStorageService;
import com.blog.blogsystem.service.PostService;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    // ─────────────── Public endpoints ───────────────

    @GetMapping
    public ResponseEntity<PageResponse<PostResponse>> getAllPosts(
            @RequestParam(value = "categorySlug", required = false) String categorySlug,
            @RequestParam(value = "tagSlug", required = false) String tagSlug,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNo",   defaultValue = "0",         required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10",        required = false) int pageSize,
            @RequestParam(value = "sortBy",   defaultValue = "createdAt", required = false) String sortBy,
            @RequestParam(value = "sortDir",  defaultValue = "desc",      required = false) String sortDir
    ) {
        return ResponseEntity.ok(postService.getAllPosts(categorySlug, tagSlug, status, keyword, pageNo, pageSize, sortBy, sortDir));
    }

    @GetMapping("/my-posts")
    public ResponseEntity<PageResponse<PostResponse>> getMyPosts(
            @RequestParam(value = "pageNo",   defaultValue = "0",         required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10",        required = false) int pageSize,
            @RequestParam(value = "sortBy",   defaultValue = "createdAt", required = false) String sortBy,
            @RequestParam(value = "sortDir",  defaultValue = "desc",      required = false) String sortDir
    ) {
        String currentUsername = getCurrentUsername();
        return ResponseEntity.ok(postService.getMyPosts(currentUsername, pageNo, pageSize, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable UUID id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    @GetMapping("/{id}/summarize")
    public ResponseEntity<com.blog.blogsystem.dto.response.ApiResponse<String>> summarizePost(@PathVariable UUID id) {
        return ResponseEntity.ok(com.blog.blogsystem.dto.response.ApiResponse.<String>builder()
                .code(200)
                .message("Tóm tắt bài viết thành công")
                .data(postService.summarizePost(id))
                .build());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<PostResponse> getPostBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(postService.getPostBySlug(slug));
    }

    // ─────────────── Protected endpoints (cần JWT) ───────────────

    /**
     * Tạo bài viết mới kèm upload ảnh bìa.
     *
     * Content-Type: multipart/form-data
     * Fields:
     *   - data       (required): JSON string của PostRequest
     *   - coverImage (optional): File ảnh bìa (JPEG/PNG/WebP/GIF, tối đa 5MB)
     *
     * ⚠️ author được lấy từ SecurityContextHolder – KHÔNG từ request body.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPost(
            @RequestPart("data") String data,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage
    ) throws IOException {
        PostRequest request = objectMapper.readValue(data, PostRequest.class);
        validatePostRequest(request);

        if (coverImage != null && !coverImage.isEmpty()) {
            String imageUrl = fileStorageService.storeImage(coverImage);
            request.setCoverImage(imageUrl);
        }

        String currentUsername = getCurrentUsername();
        return new ResponseEntity<>(postService.createPost(request, currentUsername), HttpStatus.CREATED);
    }

    /**
     * Gợi ý nội dung bài viết bằng AI.
     */
    @PostMapping("/suggest-content")
    public ResponseEntity<com.blog.blogsystem.dto.response.ApiResponse<String>> suggestPostContent(
            @Valid @RequestBody SuggestContentRequest request
    ) {
        String suggestion = postService.suggestPostContent(request.getTitle(), request.getSummary());
        return ResponseEntity.ok(com.blog.blogsystem.dto.response.ApiResponse.<String>builder()
                .code(200)
                .message("Gợi ý nội dung thành công")
                .data(suggestion)
                .build());
    }

    /**
     * Cập nhật bài viết, có thể đổi ảnh bìa.
     *
     * Content-Type: multipart/form-data
     * Fields:
     *   - data       (required): JSON string của PostRequest
     *   - coverImage (optional): File ảnh bìa mới. Nếu không gửi → giữ nguyên ảnh cũ.
     *
     * ⚠️ Chỉ tác giả mới được sửa – xác định từ JWT Token.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable UUID id,
            @RequestPart("data") String data,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage
    ) throws IOException {
        PostRequest request = objectMapper.readValue(data, PostRequest.class);
        validatePostRequest(request);

        if (coverImage != null && !coverImage.isEmpty()) {
            String imageUrl = fileStorageService.storeImage(coverImage);
            request.setCoverImage(imageUrl);
        }

        String currentUsername = getCurrentUsername();
        return ResponseEntity.ok(postService.updatePost(id, request, currentUsername));
    }

    /**
     * Xóa bài viết.
     * ⚠️ Chỉ tác giả mới được xóa – xác định từ JWT Token.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePost(@PathVariable UUID id) {
        postService.deletePost(id, getCurrentUsername());
        return ResponseEntity.ok("Xóa bài viết thành công!");
    }

    // ─────────────── Private helpers ───────────────

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private void validatePostRequest(PostRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Tiêu đề không được để trống");
        }
        if (request.getContentMarkdown() == null || request.getContentMarkdown().isBlank()) {
            throw new IllegalArgumentException("Nội dung Markdown không được để trống");
        }
        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("Danh mục không được để trống");
        }
    }
}
