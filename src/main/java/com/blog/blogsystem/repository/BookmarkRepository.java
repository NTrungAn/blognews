package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.Bookmark;
import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {
    boolean existsByUserAndPost(User user, Post post);
    Optional<Bookmark> findByUserAndPost(User user, Post post);
    Page<Bookmark> findByUser(User user, Pageable pageable);
}
