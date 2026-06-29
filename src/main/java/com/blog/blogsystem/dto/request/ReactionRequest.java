package com.blog.blogsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReactionRequest {

    @NotBlank(message = "Biểu tượng cảm xúc không được để trống")
    private String emoji;

}
