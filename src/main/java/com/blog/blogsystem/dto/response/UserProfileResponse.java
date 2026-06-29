package com.blog.blogsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String avatar;
    private String role;
    private String biography;
    
    // Các trường thống kê mở rộng
    private long totalPosts;
    private long totalComments;
    private long followersCount;
    private long followingCount;
    private String coverImage;
}
