package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.CommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, UUID> {
    Optional<CommentReaction> findByCommentIdAndUserId(UUID commentId, UUID userId);
}
