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
public class FollowerResponse {
    private UUID id;
    private String username;
    private String fullName;
    private String avatar;
}
