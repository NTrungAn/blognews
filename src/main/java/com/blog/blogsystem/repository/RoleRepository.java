package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.Role;
import com.blog.blogsystem.entity.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByRoleName(RoleType roleName);
}
