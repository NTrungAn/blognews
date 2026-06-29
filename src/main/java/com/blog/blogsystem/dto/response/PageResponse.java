package com.blog.blogsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResponse<T> {
    private List<T> content;      // Danh sách dữ liệu (ví dụ: danh sách bài viết)
    private int pageNo;           // Trang hiện tại
    private int pageSize;         // Số phần tử trên một trang
    private long totalElements;   // Tổng số phần tử trong database
    private int totalPages;       // Tổng số trang
    private boolean last;         // Kiểm tra xem có phải trang cuối không
}