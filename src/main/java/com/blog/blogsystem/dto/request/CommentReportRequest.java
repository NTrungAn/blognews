package com.blog.blogsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentReportRequest {

    @NotBlank(message = "Lý do báo cáo không được để trống")
    @Size(max = 50, message = "Lý do không dài quá 50 ký tự")
    private String reason;

    @Size(max = 500, message = "Nội dung chi tiết không dài quá 500 ký tự")
    private String detail;
}
