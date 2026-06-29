package com.blog.blogsystem.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostLikeResponse {
    private boolean liked;
    private int likesCount;
}
