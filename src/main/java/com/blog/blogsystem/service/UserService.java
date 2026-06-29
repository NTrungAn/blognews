package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.request.UserUpdateRequest;
import com.blog.blogsystem.dto.response.UserResponse;
import com.blog.blogsystem.dto.response.PublicProfileResponse;

import com.blog.blogsystem.dto.response.UserProfileResponse;

import com.blog.blogsystem.dto.response.PageResponse;
import java.util.UUID;

public interface UserService {
    UserResponse getCurrentUserProfile(String username);

    UserProfileResponse getMyProfileStats(String username);

    UserResponse updateUserProfile(String username, UserUpdateRequest request);

    PageResponse<UserResponse> getAllUsers(int pageNo, int pageSize, String sortBy, String sortDir);

    UserResponse updateUserRole(UUID userId, String newRoleName);

    void deleteUser(UUID userId);

    PublicProfileResponse getPublicProfile(String username, String currentUser);
}
