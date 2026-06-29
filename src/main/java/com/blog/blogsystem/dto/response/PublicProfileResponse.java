package com.blog.blogsystem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicProfileResponse {
    private UUID id;
    private String username;
    private String fullName;
    private String avatar;
    private String biography;
    
    private int followersCount;
    private int followingCount;
    private long totalPosts;
    private String coverImage;
    
    /** Jackson serialize boolean 'isXxx' thành key 'following', dùng @JsonProperty để giữ đúng tên */
    @JsonProperty("isFollowing")
    private boolean isFollowing;
}
