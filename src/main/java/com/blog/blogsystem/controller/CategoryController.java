package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.request.CategoryRequest;
import com.blog.blogsystem.dto.response.CategoryResponse;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller cho tính năng Quản lý Chuyên mục (Category).
 *
 * <p>Phân quyền:
 * <ul>
 *   <li>GET (read): tất cả mọi người (public) – không cần đăng nhập.</li>
 *   <li>POST / PUT / DELETE (write): chỉ người dùng có quyền ADMIN hoặc EDITOR.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Category API", description = "Quản lý Chuyên mục bài viết")
public class CategoryController {

    private final CategoryService categoryService;

    // =========================================================================
    // =========================================================================
    // CREATE – Chỉ ADMIN/EDITOR
    // =========================================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @Operation(summary = "Tạo chuyên mục mới",
               description = "Slug tự động được sinh từ tên nếu không cung cấp.")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // =========================================================================
    // READ – Public
    // =========================================================================

    @GetMapping
    @Operation(summary = "Lấy tất cả chuyên mục (không phân trang)",
               description = "Dành cho dropdown menu / thanh điều hướng.")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/paged")
    @Operation(summary = "Lấy danh sách chuyên mục có phân trang")
    public ResponseEntity<PageResponse<CategoryResponse>> getAllCategoriesPaged(
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = "0") int pageNo,
            @Parameter(description = "Số bản ghi mỗi trang")
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "Trường sắp xếp (name, id, ...)")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Chiều sắp xếp: asc hoặc desc")
            @RequestParam(defaultValue = "asc") String sortDir) {
        PageResponse<CategoryResponse> response =
                categoryService.getAllCategoriesPaged(pageNo, pageSize, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết chuyên mục theo ID")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Lấy chi tiết chuyên mục theo Slug",
               description = "Dùng cho các trang public như /danh-muc/cong-nghe.")
    public ResponseEntity<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(categoryService.getCategoryBySlug(slug));
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm chuyên mục theo từ khóa trong tên")
    public ResponseEntity<PageResponse<CategoryResponse>> searchCategories(
            @Parameter(description = "Từ khóa tìm kiếm", required = true)
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<CategoryResponse> response =
                categoryService.searchCategories(keyword, pageNo, pageSize);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // UPDATE – Chỉ ADMIN/EDITOR
    // =========================================================================

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @Operation(summary = "Cập nhật thông tin chuyên mục")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // DELETE – Chỉ ADMIN/EDITOR
    // =========================================================================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @Operation(summary = "Xóa chuyên mục",
               description = "Sẽ từ chối xóa nếu chuyên mục còn bài viết liên kết.")
    public ResponseEntity<String> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok("Xóa chuyên mục thành công!");
    }
}
