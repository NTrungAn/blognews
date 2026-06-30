package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.roleName = :roleName")
    long countByRolesRoleName(@Param("roleName") RoleType roleName);

    /** Tìm kiếm user theo keyword (username/email/fullName) và lọc theo role */
    @Query("""
        SELECT DISTINCT u FROM User u JOIN u.roles r
        WHERE (:keyword IS NULL OR :keyword = '' OR
               LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(u.email)    LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:role IS NULL OR r.roleName = :role)
        """)
    Page<User> searchUsers(@Param("keyword") String keyword,
                           @Param("role") RoleType role,
                           Pageable pageable);
}