package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.request.PostRequest;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.dto.response.PostResponse;

import java.util.UUID;

public interface PostService {
    PostResponse createPost(PostRequest request, String username);

    PageResponse<PostResponse> getAllPosts(String categorySlug, String tagSlug, String status, String keyword,
                                           int pageNo, int pageSize, String sortBy, String sortDir);

    PageResponse<PostResponse> getMyPosts(String username, int pageNo, int pageSize, String sortBy, String sortDir);

    PageResponse<PostResponse> getPostsByAuthor(String username, int pageNo, int pageSize);

    PostResponse getPostById(UUID id);

    PostResponse getPostBySlug(String slug);

    PostResponse updatePost(UUID id, PostRequest request, String username);

    void deletePost(UUID id, String username);

    PostResponse updatePostStatusByAdmin(UUID id, com.blog.blogsystem.entity.enums.PostStatus status, String adminUsername);

    String summarizePost(UUID id);

    String suggestPostContent(String title, String summary);
}
