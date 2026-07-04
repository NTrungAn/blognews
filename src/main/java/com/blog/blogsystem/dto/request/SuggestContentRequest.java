package com.blog.blogsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuggestContentRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String summary;
}
