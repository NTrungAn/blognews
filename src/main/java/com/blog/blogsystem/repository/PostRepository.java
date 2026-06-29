package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    Optional<Post> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Lấy danh sách bài viết theo trạng thái
    Page<Post> findByStatus(PostStatus status, Pageable pageable);

    // Lấy danh sách bài viết của một tác giả cụ thể
    Page<Post> findByAuthorIdAndStatus(UUID authorId, PostStatus status, Pageable pageable);

    // Lấy bài viết theo danh mục (JPQL)
    @Query("SELECT p FROM Post p WHERE p.category.slug = :categorySlug AND p.status = :status")
    Page<Post> findByCategorySlugAndStatus(@Param("categorySlug") String categorySlug,
                                           @Param("status") PostStatus status,
                                           Pageable pageable);

    // Lấy bài viết theo tag
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t.id = :tagId AND p.status = :status")
    Page<Post> findByTagsIdAndStatus(@Param("tagId") UUID tagId,
                                     @Param("status") PostStatus status,
                                     Pageable pageable);

    // ─────────────────────────────────────────────────────────
    // CÁC HÀM MỚI BỔ SUNG ĐỂ KHỚP VỚI FRONTEND
    // ─────────────────────────────────────────────────────────

    // 1. Lấy bài viết của tác giả theo Username
    Page<Post> findByAuthorUsername(String username, Pageable pageable);

    // Lấy bài viết của tác giả theo Username và Trạng thái
    Page<Post> findByAuthorUsernameAndStatus(String username, PostStatus status, Pageable pageable);

    // 2. Đếm số lượng bài viết PUBLISHED theo Category Slug
    Long countByCategorySlugAndStatus(String categorySlug, PostStatus status);

    // 3. Lọc bài viết động (CategorySlug, TagSlug, Status, Keyword)
    @Query("SELECT DISTINCT p FROM Post p " +
           "LEFT JOIN p.tags t " +
           "WHERE (:categorySlug IS NULL OR p.category.slug = :categorySlug) " +
           "AND (:tagSlug IS NULL OR t.slug = :tagSlug) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.summary) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Post> findPostsWithFilters(@Param("categorySlug") String categorySlug,
                                    @Param("tagSlug") String tagSlug,
                                    @Param("status") PostStatus status,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);

    // 4. Đếm tổng số bài viết của user
    long countByAuthorUsername(String username);
}
