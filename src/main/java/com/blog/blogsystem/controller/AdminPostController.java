package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.response.PostResponse;
import com.blog.blogsystem.entity.enums.PostStatus;
import com.blog.blogsystem.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostController {

    private final PostService postService;

    @PutMapping("/{id}/status")
    public ResponseEntity<PostResponse> updatePostStatus(
            @PathVariable UUID id,
            @RequestParam PostStatus status) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(postService.updatePostStatusByAdmin(id, status, adminUsername));
    }
}
