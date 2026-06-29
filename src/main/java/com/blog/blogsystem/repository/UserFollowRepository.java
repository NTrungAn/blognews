package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, UUID> {

    boolean existsByFollowerAndFollowing(User follower, User following);

    Optional<UserFollow> findByFollowerAndFollowing(User follower, User following);

    Page<UserFollow> findByFollowing(User following, Pageable pageable);

    /** Danh sách mà user đó đang theo dõi */
    Page<UserFollow> findByFollower(User follower, Pageable pageable);

    /** Đếm số người theo dõi (followers) của một user */
    long countByFollowing(User following);

    /** Đếm số người user đang theo dõi (following) */
    long countByFollower(User follower);
}
