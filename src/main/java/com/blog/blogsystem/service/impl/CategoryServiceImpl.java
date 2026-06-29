package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.request.CategoryRequest;
import com.blog.blogsystem.dto.response.CategoryResponse;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.entity.Category;
import com.blog.blogsystem.mapper.CategoryMapper;
import com.blog.blogsystem.repository.CategoryRepository;
import com.blog.blogsystem.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    // =========================================================================
    // CREATE
    // =========================================================================

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        // 1. Xác định slug cuối cùng (dùng slug do user nhập hoặc tự tạo từ name)
        String finalSlug = resolveSlug(request);

        // 2. Kiểm tra slug trùng lặp
        if (categoryRepository.existsBySlug(finalSlug)) {
            throw new IllegalArgumentException(
                    "Slug '" + finalSlug + "' đã tồn tại. Vui lòng chọn tên hoặc slug khác.");
        }

        // 3. Kiểm tra tên trùng lặp
        if (categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    "Tên chuyên mục '" + request.getName() + "' đã tồn tại.");
        }

        // 4. Ánh xạ từ DTO sang Entity, gán slug đã xử lý
        Category category = categoryMapper.toEntity(request);
        category.setSlug(finalSlug);

        // 5. Lưu xuống DB
        Category saved = categoryRepository.save(category);

        // 6. Trả về response (postCount = 0 khi vừa tạo)
        CategoryResponse response = categoryMapper.toResponse(saved);
        response.setPostCount(0L);
        return response;
    }

    // =========================================================================
    // READ
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(this::toResponseWithPostCount)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getAllCategoriesPaged(
            int pageNo, int pageSize, String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Category> page = categoryRepository.findAll(pageable);

        List<CategoryResponse> content = page.getContent().stream()
                .map(this::toResponseWithPostCount)
                .collect(Collectors.toList());

        return PageResponse.<CategoryResponse>builder()
                .content(content)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = findCategoryOrThrow(id);
        return toResponseWithPostCount(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy chuyên mục với slug: " + slug));
        return toResponseWithPostCount(category);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> searchCategories(String keyword, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("name").ascending());
        Page<Category> page = categoryRepository.findByNameContainingIgnoreCase(keyword, pageable);

        List<CategoryResponse> content = page.getContent().stream()
                .map(this::toResponseWithPostCount)
                .collect(Collectors.toList());

        return PageResponse.<CategoryResponse>builder()
                .content(content)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        // 1. Tìm chuyên mục cần cập nhật
        Category category = findCategoryOrThrow(id);

        // 2. Xác định slug mới (nếu có thay đổi)
        String finalSlug = resolveSlug(request);

        // 3. Kiểm tra slug mới có bị trùng với bản ghi KHÁC không
        if (categoryRepository.existsBySlugAndIdNot(finalSlug, id)) {
            throw new IllegalArgumentException(
                    "Slug '" + finalSlug + "' đã được sử dụng bởi chuyên mục khác.");
        }

        // 4. Cập nhật entity từ request (MapStruct ignore null)
        categoryMapper.updateEntityFromRequest(request, category);
        category.setSlug(finalSlug);

        // 5. Lưu và trả về
        Category updated = categoryRepository.save(category);
        return toResponseWithPostCount(updated);
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = findCategoryOrThrow(id);

        // Kiểm tra còn bài viết không trước khi xóa
        long postCount = categoryRepository.countPublishedPostsByCategoryId(id);
        if (postCount > 0) {
            throw new IllegalStateException(
                    "Không thể xóa chuyên mục '" + category.getName() + "' vì còn "
                            + postCount + " bài viết đang thuộc chuyên mục này. "
                            + "Vui lòng chuyển bài viết sang chuyên mục khác trước.");
        }

        categoryRepository.delete(category);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Tìm chuyên mục hoặc ném ResourceNotFoundException.
     */
    private Category findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy chuyên mục với ID: " + id));
    }

    /**
     * Map entity sang response và tự động điền postCount.
     */
    private CategoryResponse toResponseWithPostCount(Category category) {
        CategoryResponse response = categoryMapper.toResponse(category);
        response.setPostCount(categoryRepository.countPublishedPostsByCategoryId(category.getId()));
        return response;
    }

    /**
     * Xác định slug cuối cùng:
     * - Nếu request có slug → dùng slug đó (đã được validate bởi @Pattern).
     * - Nếu không → tự động sinh từ name (chuyển về ASCII, chữ thường, thay khoảng trắng bằng dấu '-').
     */
    private String resolveSlug(CategoryRequest request) {
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            return request.getSlug().toLowerCase().trim();
        }
        return generateSlug(request.getName());
    }

    /**
     * Tự động sinh slug từ tên tiếng Việt hoặc tiếng Anh.
     * Ví dụ: "Công Nghệ Thông Tin" → "cong-nghe-thong-tin"
     */
    public static String generateSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        // Chuẩn hóa Unicode (NFD) rồi bỏ dấu
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern diacriticsPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String withoutDiacritics = diacriticsPattern.matcher(normalized).replaceAll("");

        return withoutDiacritics
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")   // bỏ ký tự đặc biệt
                .replaceAll("\\s+", "-")              // thay khoảng trắng bằng '-'
                .replaceAll("-{2,}", "-")             // gộp nhiều '-' liên tiếp
                .replaceAll("^-|-$", "");             // bỏ '-' ở đầu và cuối
    }
}
