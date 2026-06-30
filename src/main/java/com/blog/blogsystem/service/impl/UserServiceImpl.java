package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.request.UserUpdateRequest;
import com.blog.blogsystem.dto.response.UserResponse;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.mapper.UserMapper;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.dto.response.PublicProfileResponse;
import com.blog.blogsystem.dto.response.UserProfileResponse;
import com.blog.blogsystem.entity.Role;
import com.blog.blogsystem.entity.enums.RoleType;
import com.blog.blogsystem.repository.PostRepository;
import com.blog.blogsystem.repository.CommentRepository;
import com.blog.blogsystem.repository.RoleRepository;
import com.blog.blogsystem.repository.UserRepository;
import com.blog.blogsystem.repository.UserFollowRepository;
import com.blog.blogsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserFollowRepository userFollowRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfileStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        long totalPosts = postRepository.countByAuthorUsername(username);
        long totalComments = commentRepository.countByAuthorUsername(username);
        long followersCount = userFollowRepository.countByFollowing(user);
        long followingCount = userFollowRepository.countByFollower(user);
        UserResponse userResponse = userMapper.toResponse(user);

        return UserProfileResponse.builder()
                .id(userResponse.getId())
                .username(userResponse.getUsername())
                .email(userResponse.getEmail())
                .fullName(userResponse.getFullName())
                .avatar(userResponse.getAvatar())
                .role(userResponse.getRole())
                .biography(userResponse.getBiography())
                .totalPosts(totalPosts)
                .totalComments(totalComments)
                .followersCount(followersCount)
                .followingCount(followingCount)
                .coverImage(user.getCoverImage())
                .build();
    }

    @Override
    @Transactional
    public UserResponse updateUserProfile(String username, UserUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Cập nhật thông tin cơ bản
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getCoverImage() != null) {
            user.setCoverImage(request.getCoverImage());
        }
        if (request.getBiography() != null) {
            user.setBiography(request.getBiography());
        }

        // Đổi mật khẩu nếu có gửi currentPassword và newPassword
        if (request.getCurrentPassword() != null && !request.getCurrentPassword().isBlank()
                && request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new RuntimeException("Mật khẩu hiện tại không chính xác");
            }
            
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(int pageNo, int pageSize, String sortBy, String sortDir, String keyword, String role) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        // Parse role filter
        RoleType roleType = null;
        if (role != null && !role.isBlank() && !role.equalsIgnoreCase("ALL")) {
            try {
                roleType = RoleType.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException ignored) { /* invalid role = no filter */ }
        }

        // Normalize keyword
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Page<User> users;
        if (kw == null && roleType == null) {
            users = userRepository.findAll(pageable);
        } else {
            users = userRepository.searchUsers(kw, roleType, pageable);
        }

        List<UserResponse> content = users.getContent().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .content(content)
                .pageNo(users.getNumber())
                .pageSize(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .last(users.isLast())
                .build();
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(UUID userId, String newRoleName) {
        RoleType roleType;
        try {
            roleType = RoleType.valueOf(newRoleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Quyền không hợp lệ: " + newRoleName);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        Role role = roleRepository.findByRoleName(roleType)
                .orElseThrow(() -> new RuntimeException("Quyền không hợp lệ: " + newRoleName));

        // Kiểm tra nếu đang hạ quyền của Admin
        boolean wasAdmin = user.getRoles().stream().anyMatch(r -> r.getRoleName() == RoleType.ADMIN);
        if (wasAdmin && roleType != RoleType.ADMIN) {
            long adminCount = userRepository.countByRolesRoleName(RoleType.ADMIN);
            if (adminCount <= 1) {
                throw new RuntimeException("Không thể hạ quyền Admin cuối cùng của hệ thống.");
            }
        }

        user.getRoles().clear();
        user.getRoles().add(role);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));
        
        // Kiểm tra nếu đang xoá Admin
        boolean wasAdmin = user.getRoles().stream().anyMatch(r -> r.getRoleName() == RoleType.ADMIN);
        if (wasAdmin) {
            long adminCount = userRepository.countByRolesRoleName(RoleType.ADMIN);
            if (adminCount <= 1) {
                throw new RuntimeException("Không thể xoá Admin cuối cùng của hệ thống.");
            }
        }

        // Lưu ý: Nếu user có bài viết, cần xử lý cascade hoặc set null authorId.
        // Ở đây giả định xoá user (thực tế nên dùng cờ 'isActive' để soft delete).
        userRepository.deleteById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(String username, String currentUser) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        long totalPosts = postRepository.countByAuthorUsername(username);

        // Đếm thực tế từ bảng quan hệ – tánh trường hợp cache bị lệch
        long followersCount = userFollowRepository.countByFollowing(user);
        long followingCount = userFollowRepository.countByFollower(user);

        boolean isFollowing = false;
        if (currentUser != null && !currentUser.equals("anonymousUser")) {
            User follower = userRepository.findByUsername(currentUser).orElse(null);
            if (follower != null) {
                isFollowing = userFollowRepository.existsByFollowerAndFollowing(follower, user);
            }
        }

        return PublicProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
                .coverImage(user.getCoverImage())
                .biography(user.getBiography())
                .followersCount((int) followersCount)
                .followingCount((int) followingCount)
                .totalPosts(totalPosts)
                .isFollowing(isFollowing)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicProfileResponse> getPopularAuthors() {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        List<User> users = userRepository.findAll(pageable).getContent();

        return users.stream().map(user -> {
            long totalPosts = postRepository.countByAuthorUsername(user.getUsername());
            long followersCount = userFollowRepository.countByFollowing(user);
            long followingCount = userFollowRepository.countByFollower(user);
            return PublicProfileResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .avatar(user.getAvatar())
                    .coverImage(user.getCoverImage())
                    .biography(user.getBiography())
                    .followersCount((int) followersCount)
                    .followingCount((int) followingCount)
                    .totalPosts(totalPosts)
                    .isFollowing(false)
                    .build();
        }).collect(Collectors.toList());
    }
}
