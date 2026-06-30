package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.CommentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, UUID> {
    List<CommentReport> findByCommentIdOrderByCreatedAtDesc(UUID commentId);
    void deleteByCommentId(UUID commentId);
}
