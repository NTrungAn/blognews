package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.request.UserUpdateRequest;
import com.blog.blogsystem.dto.response.PublicProfileResponse;
import com.blog.blogsystem.dto.response.UserProfileResponse;
import com.blog.blogsystem.dto.response.UserResponse;
import com.blog.blogsystem.entity.Role;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.RoleType;
import com.blog.blogsystem.mapper.UserMapper;
import com.blog.blogsystem.repository.CommentRepository;
import com.blog.blogsystem.repository.PostRepository;
import com.blog.blogsystem.repository.RoleRepository;
import com.blog.blogsystem.repository.UserFollowRepository;
import com.blog.blogsystem.repository.UserRepository;
import com.blog.blogsystem.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserFollowRepository userFollowRepository;

    @InjectMocks
    private UserServiceImpl userService;

    // Cập nhật profile thành công khi mật khẩu hiện tại đúng và có yêu cầu đổi mật khẩu.
    @Test
    public void testUpdateUserProfile_WithPasswordChange_Success() {
        User user = createUser("profile_user", RoleType.USER);
        UserUpdateRequest request = UserUpdateRequest.builder()
                .fullName("Updated Name")
                .biography("Updated biography")
                .currentPassword("old-password")
                .newPassword("new-password")
                .build();
        UserResponse mappedResponse = UserResponse.builder()
                .username("profile_user")
                .fullName("Updated Name")
                .biography("Updated biography")
                .role("USER")
                .build();

        when(userRepository.findByUsername("profile_user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(mappedResponse);

        UserResponse result = userService.updateUserProfile("profile_user", request);

        assertEquals("Updated Name", result.getFullName());
        assertEquals("Updated biography", result.getBiography());
        assertEquals("encoded-new-password", user.getPasswordHash());
        verify(passwordEncoder, times(1)).encode("new-password");
    }

    // Đổi mật khẩu phải thất bại khi currentPassword không khớp với mật khẩu hiện tại.
    @Test
    public void testUpdateUserProfile_WrongCurrentPassword_ThrowsRuntimeException() {
        User user = createUser("profile_user", RoleType.USER);
        UserUpdateRequest request = UserUpdateRequest.builder()
                .currentPassword("wrong-password")
                .newPassword("new-password")
                .build();

        when(userRepository.findByUsername("profile_user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUserProfile("profile_user", request));

        assertEquals("Mật khẩu hiện tại không chính xác", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // Hạ quyền admin cuối cùng của hệ thống phải bị chặn để tránh mất toàn bộ quyền quản trị.
    @Test
    public void testUpdateUserRole_LastAdminDemotion_ThrowsRuntimeException() {
        User adminUser = createUser("admin", RoleType.ADMIN);
        Role userRole = createRole(RoleType.USER);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(roleRepository.findByRoleName(RoleType.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.countByRolesRoleName(RoleType.ADMIN)).thenReturn(1L);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUserRole(adminUser.getId(), "USER"));

        assertEquals("Không thể hạ quyền Admin cuối cùng của hệ thống.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // Xóa admin cuối cùng của hệ thống phải bị chặn để đảm bảo luôn còn ít nhất một quản trị viên.
    @Test
    public void testDeleteUser_LastAdmin_ThrowsRuntimeException() {
        User adminUser = createUser("admin", RoleType.ADMIN);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRolesRoleName(RoleType.ADMIN)).thenReturn(1L);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.deleteUser(adminUser.getId()));

        assertEquals("Không thể xoá Admin cuối cùng của hệ thống.", exception.getMessage());
        verify(userRepository, never()).deleteById(adminUser.getId());
    }

    // Public profile phải phản ánh đúng trạng thái isFollowing khi currentUser đang follow target user.
    @Test
    public void testGetPublicProfile_WhenFollowing_ReturnsIsFollowingTrue() {
        User target = createUser("target_user", RoleType.USER);
        User current = createUser("current_user", RoleType.USER);

        when(userRepository.findByUsername("target_user")).thenReturn(Optional.of(target));
        when(postRepository.countByAuthorUsername("target_user")).thenReturn(3L);
        when(userFollowRepository.countByFollowing(target)).thenReturn(5L);
        when(userFollowRepository.countByFollower(target)).thenReturn(2L);
        when(userRepository.findByUsername("current_user")).thenReturn(Optional.of(current));
        when(userFollowRepository.existsByFollowerAndFollowing(current, target)).thenReturn(true);

        PublicProfileResponse result = userService.getPublicProfile("target_user", "current_user");

        assertEquals("target_user", result.getUsername());
        assertEquals(3L, result.getTotalPosts());
        assertEquals(5, result.getFollowersCount());
        assertTrue(result.isFollowing());
    }

    // Public profile phải trả isFollowing=false khi currentUser không follow hoặc không tồn tại.
    @Test
    public void testGetPublicProfile_WhenNotFollowing_ReturnsFalse() {
        User target = createUser("target_user", RoleType.USER);

        when(userRepository.findByUsername("target_user")).thenReturn(Optional.of(target));
        when(postRepository.countByAuthorUsername("target_user")).thenReturn(0L);
        when(userFollowRepository.countByFollowing(target)).thenReturn(0L);
        when(userFollowRepository.countByFollower(target)).thenReturn(0L);
        when(userRepository.findByUsername("missing_user")).thenReturn(Optional.empty());

        PublicProfileResponse result = userService.getPublicProfile("target_user", "missing_user");

        assertFalse(result.isFollowing());
        assertEquals(0, result.getFollowersCount());
    }

    // Thống kê profile cá nhân phải tổng hợp đúng tổng bài viết, bình luận và quan hệ follow.
    @Test
    public void testGetMyProfileStats_ComposesAggregatedValues() {
        User user = createUser("stats_user", RoleType.USER);
        UserResponse mappedUser = UserResponse.builder()
                .id(user.getId())
                .username("stats_user")
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role("USER")
                .build();

        when(userRepository.findByUsername("stats_user")).thenReturn(Optional.of(user));
        when(postRepository.countByAuthorUsername("stats_user")).thenReturn(4L);
        when(commentRepository.countByAuthorUsername("stats_user")).thenReturn(7L);
        when(userFollowRepository.countByFollowing(user)).thenReturn(2L);
        when(userFollowRepository.countByFollower(user)).thenReturn(3L);
        when(userMapper.toResponse(user)).thenReturn(mappedUser);

        UserProfileResponse result = userService.getMyProfileStats("stats_user");

        assertEquals("stats_user", result.getUsername());
        assertEquals(4L, result.getTotalPosts());
        assertEquals(7L, result.getTotalComments());
        assertEquals(2L, result.getFollowersCount());
        assertEquals(3L, result.getFollowingCount());
    }

    private User createUser(String username, RoleType roleType) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFullName("Full Name " + username);
        user.setPasswordHash("encoded-password");
        user.setRoles(new HashSet<>(Set.of(createRole(roleType))));
        return user;
    }

    private Role createRole(RoleType roleType) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setRoleName(roleType);
        return role;
    }
}
