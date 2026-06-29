package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.dto.response.PostResponse;

import java.util.UUID;

public interface BookmarkService {
    String toggleBookmark(UUID postId, String username);
    boolean checkBookmarkStatus(UUID postId, String username);
    PageResponse<PostResponse> getMyBookmarks(String username, int pageNo, int pageSize);
}
