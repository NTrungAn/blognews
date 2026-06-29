package com.blog.blogsystem.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    private String fullName;

    private String avatar;

    private String coverImage;

    private String currentPassword;

    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String newPassword;

    @Size(max = 500, message = "Tiểu sử không được vượt quá 500 ký tự")
    private String biography;
}
