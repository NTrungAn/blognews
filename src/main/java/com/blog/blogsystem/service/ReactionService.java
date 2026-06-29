package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.response.PostLikeResponse;

import java.util.UUID;

public interface ReactionService {
    PostLikeResponse toggleLikePost(UUID postId, String currentUsername);

    boolean isPostLiked(UUID postId, String currentUsername);
}
