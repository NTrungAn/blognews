package com.blog.blogsystem.dto.projection;

import java.util.UUID;

public interface TagCountProjection {
    UUID getId();
    String getName();
    String getSlug();
    Long getPostCount();
}
