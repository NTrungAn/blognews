package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /** Tìm chuyên mục theo slug (dùng cho tra cứu URL-friendly). */
    Optional<Category> findBySlug(String slug);

    /** Kiểm tra slug đã tồn tại chưa (dùng khi tạo mới). */
    boolean existsBySlug(String slug);

    /** Kiểm tra slug đã tồn tại chưa, loại trừ chính nó (dùng khi cập nhật). */
    boolean existsBySlugAndIdNot(String slug, UUID id);

    /** Kiểm tra tên chuyên mục đã tồn tại chưa (dùng khi tạo mới). */
    boolean existsByName(String name);

    /** Tìm kiếm chuyên mục theo tên (không phân biệt hoa thường, hỗ trợ phân trang). */
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Đếm số bài viết của một chuyên mục theo trạng thái.
     */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.category.id = :categoryId AND p.status = 'PUBLISHED'")
    long countPublishedPostsByCategoryId(@Param("categoryId") UUID categoryId);
}
