package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.dto.response.PostResponse;
import com.blog.blogsystem.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private static final String AUTHENTICATED_ROLES =
            "hasAnyRole('ADMIN', 'EDITOR', 'USER', 'READER')";

    private final BookmarkService bookmarkService;

    @GetMapping("/me")
    @PreAuthorize(AUTHENTICATED_ROLES)
    public ResponseEntity<PageResponse<PostResponse>> getMyBookmarks(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        String username = getCurrentUsername();
        return ResponseEntity.ok(bookmarkService.getMyBookmarks(username, pageNo, pageSize));
    }

    @PostMapping("/{postId}")
    @PreAuthorize(AUTHENTICATED_ROLES)
    public ResponseEntity<String> toggleBookmark(@PathVariable UUID postId) {
        String username = getCurrentUsername();
        String result = bookmarkService.toggleBookmark(postId, username);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{postId}/status")
    @PreAuthorize(AUTHENTICATED_ROLES)
    public ResponseEntity<Boolean> checkBookmarkStatus(@PathVariable UUID postId) {
        String username = getCurrentUsername();
        boolean isBookmarked = bookmarkService.checkBookmarkStatus(postId, username);
        return ResponseEntity.ok(isBookmarked);
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
