package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.request.CategoryRequest;
import com.blog.blogsystem.dto.response.CategoryResponse;
import com.blog.blogsystem.dto.response.PageResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    /** Tạo mới một chuyên mục. Tự động sinh slug nếu không được cung cấp. */
    CategoryResponse createCategory(CategoryRequest request);

    /** Lấy danh sách tất cả chuyên mục (không phân trang, dùng cho dropdown menu). */
    List<CategoryResponse> getAllCategories();

    /** Lấy danh sách chuyên mục có phân trang và sắp xếp. */
    PageResponse<CategoryResponse> getAllCategoriesPaged(int pageNo, int pageSize, String sortBy, String sortDir);

    /** Lấy chi tiết một chuyên mục theo ID. */
    CategoryResponse getCategoryById(UUID id);

    /** Lấy chi tiết một chuyên mục theo slug (dùng cho URL-friendly). */
    CategoryResponse getCategoryBySlug(String slug);

    /** Tìm kiếm chuyên mục theo từ khóa trong tên. */
    PageResponse<CategoryResponse> searchCategories(String keyword, int pageNo, int pageSize);

    /** Cập nhật thông tin chuyên mục. */
    CategoryResponse updateCategory(UUID id, CategoryRequest request);

    /** Xóa một chuyên mục theo ID. Sẽ báo lỗi nếu chuyên mục đang có bài viết. */
    void deleteCategory(UUID id);
}
