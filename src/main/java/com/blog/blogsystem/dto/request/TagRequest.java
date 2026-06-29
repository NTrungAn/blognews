package com.blog.blogsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagRequest {

    @NotBlank(message = "Tên tag không được để trống")
    @Size(max = 100, message = "Tên tag không được vượt quá 100 ký tự")
    private String name;

    @NotBlank(message = "Slug không được để trống")
    @Size(max = 100, message = "Slug không được vượt quá 100 ký tự")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
             message = "Slug chỉ được chứa chữ thường, số và dấu gạch ngang")
    private String slug;
}
