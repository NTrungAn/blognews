package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    /**
     * Lấy tất cả bình luận gốc (không có parent) của một bài viết,
     * phân trang, sắp xếp theo thời gian tạo.
     */
    Page<Comment> findByPostIdAndParentIsNull(UUID postId, Pageable pageable);

    /**
     * Lấy tất cả reply của một bình luận cha.
     */
    List<Comment> findByParentIdOrderByCreatedAtAsc(UUID parentId);

    /**
     * Đếm tổng số bình luận (gốc + reply) của một bài viết.
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId")
    long countByPostId(@Param("postId") UUID postId);

    /**
     * Lấy tất cả bình luận do một user thực hiện (dùng trong trang profile).
     */
    Page<Comment> findByAuthorId(UUID authorId, Pageable pageable);

    /**
     * Đếm tổng số bình luận của user (dùng cho thống kê profile).
     */
    long countByAuthorUsername(String username);
}
