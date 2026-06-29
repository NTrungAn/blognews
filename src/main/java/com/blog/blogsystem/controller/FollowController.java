package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.response.ApiResponse;
import com.blog.blogsystem.dto.response.FollowerResponse;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{username}/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> followUser(@PathVariable String username) {
        String currentUsername = getCurrentUsername();
        followService.followUser(username, currentUsername);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Theo dõi thành công!")
                .build());
    }

    @DeleteMapping("/{username}/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(@PathVariable String username) {
        String currentUsername = getCurrentUsername();
        followService.unfollowUser(username, currentUsername);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Đã hủy theo dõi!")
                .build());
    }

    @GetMapping("/{username}/followers")
    public ResponseEntity<PageResponse<FollowerResponse>> getFollowers(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(followService.getFollowers(username, pageNo, pageSize));
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}

// Force recompile
