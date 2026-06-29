package com.blog.blogsystem.dto.response;

import lombok.Data;
import java.util.UUID;

@Data
public class TagResponse {
    private UUID id;
    private String name;
    private String slug;
    private Long postCount;
}
