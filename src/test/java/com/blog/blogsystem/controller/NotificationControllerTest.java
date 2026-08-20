package com.blog.blogsystem.controller;

import com.blog.blogsystem.entity.Notification;
import com.blog.blogsystem.entity.Role;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.NotificationType;
import com.blog.blogsystem.entity.enums.RoleType;
import com.blog.blogsystem.repository.NotificationRepository;
import com.blog.blogsystem.repository.RoleRepository;
import com.blog.blogsystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ──────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────

    private User createUser(String username, String email, RoleType roleType) {
        Role role = roleRepository.findByRoleName(roleType)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role " + roleType));

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode("SecureP@ss123"))
                .fullName("Test User " + username)
                .roles(Set.of(role))
                .isActive(true)
                .build();

        return userRepository.save(user);
    }

    private Notification createNotification(User recipient, User actor, NotificationType type, String content) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .content(content)
                .targetUrl("/api/posts/some-post")
                .isRead(false)
                .build();
        return notificationRepository.save(notification);
    }

    // ──────────────────────────────────────────
    // Test Cases
    // ──────────────────────────────────────────

    @Test
    @WithMockUser(username = "notif_user", roles = {"USER"})
    public void testGetMyNotifications_Success() throws Exception {
        User recipient = createUser("notif_user", "notif_user@example.com", RoleType.USER);
        User actor = createUser("actor_user", "actor_user@example.com", RoleType.USER);
        createNotification(recipient, actor, NotificationType.COMMENT, "actor_user đã bình luận về bài viết của bạn.");

        mockMvc.perform(get("/api/notifications")
                .param("pageNo", "0")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].content").value("actor_user đã bình luận về bài viết của bạn."));
    }

    @Test
    @WithMockUser(username = "unread_user", roles = {"READER"})
    public void testGetUnreadCount_Success() throws Exception {
        User recipient = createUser("unread_user", "unread_user@example.com", RoleType.READER);
        User actor = createUser("actor_user2", "actor_user2@example.com", RoleType.USER);
        createNotification(recipient, actor, NotificationType.SYSTEM, "actor_user2 đã theo dõi bạn.");
        createNotification(recipient, actor, NotificationType.REACTION, "actor_user2 đã thích bình luận của bạn.");

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    @WithMockUser(username = "read_user", roles = {"USER"})
    public void testMarkAsRead_Success() throws Exception {
        User recipient = createUser("read_user", "read_user@example.com", RoleType.USER);
        User actor = createUser("actor_user3", "actor_user3@example.com", RoleType.USER);
        Notification notif = createNotification(recipient, actor, NotificationType.SYSTEM, "Có bài viết mới từ actor_user3");

        mockMvc.perform(put("/api/notifications/{id}/read", notif.getId()))
                .andExpect(status().isOk());

        // Verify state
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    @WithMockUser(username = "read_all_user", roles = {"USER"})
    public void testMarkAllAsRead_Success() throws Exception {
        User recipient = createUser("read_all_user", "read_all_user@example.com", RoleType.USER);
        User actor = createUser("actor_user4", "actor_user4@example.com", RoleType.USER);
        createNotification(recipient, actor, NotificationType.COMMENT, "Thông báo 1");
        createNotification(recipient, actor, NotificationType.SYSTEM, "Thông báo 2");

        mockMvc.perform(put("/api/notifications/read-all"))
                .andExpect(status().isOk());

        // Verify count is 0
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    public void testGetMyNotifications_Unauthorized_WithoutToken() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }
}
