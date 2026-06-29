package com.blog.blogsystem.config;

import com.blog.blogsystem.entity.Role;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.RoleType;
import com.blog.blogsystem.repository.RoleRepository;
import com.blog.blogsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Khởi tạo các quyền cơ bản nếu chưa có
        createRoleIfNotFound(RoleType.ADMIN, "Quản trị viên toàn hệ thống");
        createRoleIfNotFound(RoleType.EDITOR, "Biên tập viên duyệt bài");
        createRoleIfNotFound(RoleType.USER, "Người dùng phổ thông");
        createRoleIfNotFound(RoleType.READER, "Người đọc");

        // 2. Tạo sẵn một tài khoản Admin mặc định để tiện test
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@blog.com");
            admin.setPasswordHash(passwordEncoder.encode("123456"));
            admin.setFullName("System Administrator");

            Role adminRole = roleRepository.findByRoleName(RoleType.ADMIN).get();
            admin.setRoles(Collections.singleton(adminRole));

            userRepository.save(admin);
            System.out.println("Đã tạo tài khoản Admin mặc định: admin / 123456");
        }
    }

    private void createRoleIfNotFound(RoleType roleType, String description) {
        if (roleRepository.findByRoleName(roleType).isEmpty()) {
            Role role = new Role();
            role.setRoleName(roleType);
            role.setDescription(description);
            roleRepository.save(role);
            System.out.println("Đã thêm quyền: " + roleType.name());
        }
    }
}
