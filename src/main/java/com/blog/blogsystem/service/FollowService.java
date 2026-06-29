package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.response.FollowerResponse;
import com.blog.blogsystem.dto.response.PageResponse;

public interface FollowService {
    void followUser(String targetUsername, String currentUsername);
    
    void unfollowUser(String targetUsername, String currentUsername);
    
    PageResponse<FollowerResponse> getFollowers(String username, int pageNo, int pageSize);

    PageResponse<FollowerResponse> getFollowing(String username, int pageNo, int pageSize);
}
