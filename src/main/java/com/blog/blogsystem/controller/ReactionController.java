package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.response.PostLikeResponse;
import com.blog.blogsystem.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping("/{postId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostLikeResponse> toggleLike(@PathVariable UUID postId) {
        String currentUsername = getCurrentUsername();
        return ResponseEntity.ok(reactionService.toggleLikePost(postId, currentUsername));
    }

    @GetMapping("/{postId}/like/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> checkLikeStatus(@PathVariable UUID postId) {
        String currentUsername = getCurrentUsername();
        return ResponseEntity.ok(reactionService.isPostLiked(postId, currentUsername));
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
