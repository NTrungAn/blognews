package com.blog.blogsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Tên chuyên mục không được để trống")
    @Size(max = 255, message = "Tên chuyên mục không được vượt quá 255 ký tự")
    private String name;

    /**
     * Slug được dùng trong URL (vd: "cong-nghe", "the-thao").
     * Chỉ cho phép chữ thường, số, và dấu gạch ngang.
     * Nếu không cung cấp, Service sẽ tự động tạo từ name.
     */
    @Pattern(
            regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "Slug chỉ được chứa chữ thường, số và dấu gạch ngang, không có khoảng trắng"
    )
    @Size(max = 255, message = "Slug không được vượt quá 255 ký tự")
    private String slug;

    @Size(max = 5000, message = "Mô tả không được vượt quá 5000 ký tự")
    private String description;
}
