package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.PostLike;
import com.blog.blogsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    boolean existsByPostAndUser(Post post, User user);

    Optional<PostLike> findByPostAndUser(Post post, User user);

    long countByPost_Id(UUID postId);
}
