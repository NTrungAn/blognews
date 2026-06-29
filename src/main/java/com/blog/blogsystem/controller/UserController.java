package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.request.UserUpdateRequest;
import com.blog.blogsystem.dto.response.FollowerResponse;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.dto.response.PostResponse;
import com.blog.blogsystem.dto.response.UserProfileResponse;
import com.blog.blogsystem.dto.response.PublicProfileResponse;
import com.blog.blogsystem.dto.response.UserResponse;
import com.blog.blogsystem.service.FollowService;
import com.blog.blogsystem.service.PostService;
import com.blog.blogsystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PostService postService;
    private final FollowService followService;

    // ─────────────── Public Endpoints ───────────────

    @GetMapping("/{username}")
    public ResponseEntity<PublicProfileResponse> getPublicProfile(@PathVariable String username) {
        String currentUsername = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            currentUsername = authentication.getName();
        }
        return ResponseEntity.ok(userService.getPublicProfile(username, currentUsername));
    }

    @GetMapping("/{username}/posts")
    public ResponseEntity<PageResponse<PostResponse>> getAuthorPosts(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(postService.getPostsByAuthor(username, pageNo, pageSize));
    }

    // ─────────────── Protected Endpoints (Current User) ───────────────

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        String username = getCurrentUsername();
        return ResponseEntity.ok(userService.getCurrentUserProfile(username));
    }

    @GetMapping("/me/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'USER', 'READER')")
    public ResponseEntity<UserProfileResponse> getMyProfileStats() {
        String username = getCurrentUsername();
        return ResponseEntity.ok(userService.getMyProfileStats(username));
    }

    @GetMapping("/me/followers")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'USER', 'READER')")
    public ResponseEntity<PageResponse<FollowerResponse>> getMyFollowers(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        String username = getCurrentUsername();
        return ResponseEntity.ok(followService.getFollowers(username, pageNo, pageSize));
    }

    @GetMapping("/me/following")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'USER', 'READER')")
    public ResponseEntity<PageResponse<FollowerResponse>> getMyFollowing(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        String username = getCurrentUsername();
        return ResponseEntity.ok(followService.getFollowing(username, pageNo, pageSize));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(@Valid @RequestBody UserUpdateRequest request) {
        String username = getCurrentUsername();
        return ResponseEntity.ok(userService.updateUserProfile(username, request));
    }

    // ─────────────── Admin Endpoints ───────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.blog.blogsystem.dto.response.PageResponse<UserResponse>> getAllUsers(
            @RequestParam(value = "pageNo",   defaultValue = "0",         required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10",        required = false) int pageSize,
            @RequestParam(value = "sortBy",   defaultValue = "createdAt", required = false) String sortBy,
            @RequestParam(value = "sortDir",  defaultValue = "desc",      required = false) String sortDir
    ) {
        return ResponseEntity.ok(userService.getAllUsers(pageNo, pageSize, sortBy, sortDir));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        String newRole = body.get("role");
        if (newRole == null || newRole.isBlank()) {
            throw new IllegalArgumentException("Quyền (role) không được để trống");
        }
        return ResponseEntity.ok(userService.updateUserRole(id, newRole.toUpperCase()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("Xóa người dùng thành công");
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
