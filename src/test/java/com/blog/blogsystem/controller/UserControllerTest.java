package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.request.UserUpdateRequest;
import com.blog.blogsystem.entity.Category;
import com.blog.blogsystem.entity.Comment;
import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.Role;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.UserFollow;
import com.blog.blogsystem.entity.enums.PostStatus;
import com.blog.blogsystem.entity.enums.RoleType;
import com.blog.blogsystem.repository.CategoryRepository;
import com.blog.blogsystem.repository.CommentRepository;
import com.blog.blogsystem.repository.PostRepository;
import com.blog.blogsystem.repository.RoleRepository;
import com.blog.blogsystem.repository.UserFollowRepository;
import com.blog.blogsystem.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserFollowRepository userFollowRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // API public lấy hồ sơ công khai và đánh dấu đúng trạng thái isFollowing của người xem hiện tại.
    @Test
    @WithMockUser(username = "viewer_user", roles = {"USER"})
    public void testGetPublicProfile_Success_WithFollowingStatus() throws Exception {
        User viewer = createUser("viewer_user", "viewer_user@example.com", RoleType.USER);
        User author = createUser("author_public", "author_public@example.com", RoleType.USER);
        Category category = saveCategory("Public Profile Category", "public-profile-category");
        savePost(author, category, "Public Profile Post", "public-profile-post", PostStatus.PUBLISHED);
        saveFollow(viewer, author);

        mockMvc.perform(get("/api/users/{username}", author.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.username").value(author.getUsername()))
                .andExpect(jsonPath("$.data.totalPosts").value(1))
                .andExpect(jsonPath("$.data.isFollowing").value(true));
    }

    // API public lấy danh sách bài viết của tác giả và chỉ trả về bài PUBLISHED.
    @Test
    public void testGetAuthorPosts_Success_PublicOnlyPublished() throws Exception {
        User author = createUser("author_posts", "author_posts@example.com", RoleType.USER);
        Category category = saveCategory("Author Posts Category", "author-posts-category");
        savePost(author, category, "Published Post", "published-post-author", PostStatus.PUBLISHED);
        savePost(author, category, "Draft Post", "draft-post-author", PostStatus.DRAFT);

        mockMvc.perform(get("/api/users/{username}/posts", author.getUsername())
                .param("pageNo", "0")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[*].title").value(hasItem("Published Post")));
    }

    // User đang đăng nhập xem được hồ sơ của chính mình qua endpoint /me.
    @Test
    @WithMockUser(username = "current_user", roles = {"USER"})
    public void testGetCurrentUser_Success() throws Exception {
        createUser("current_user", "current_user@example.com", RoleType.USER);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("current_user"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    // User xem thống kê profile cá nhân gồm số bài viết, bình luận, followers và following.
    @Test
    @WithMockUser(username = "stats_user", roles = {"USER"})
    public void testGetMyProfileStats_Success() throws Exception {
        User statsUser = createUser("stats_user", "stats_user@example.com", RoleType.USER);
        User follower = createUser("stats_follower", "stats_follower@example.com", RoleType.USER);
        User followingTarget = createUser("stats_following", "stats_following@example.com", RoleType.USER);
        Category category = saveCategory("Stats Category", "stats-category");
        Post userPost = savePost(statsUser, category, "Stats Post", "stats-post", PostStatus.PUBLISHED);
        Post otherPost = savePost(follower, category, "Other Post", "other-post-for-comment", PostStatus.PUBLISHED);
        saveComment(userPost, statsUser, "My comment on my post", null);
        saveComment(otherPost, statsUser, "My comment on another post", null);
        saveFollow(follower, statsUser);
        saveFollow(statsUser, followingTarget);

        mockMvc.perform(get("/api/users/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("stats_user"))
                .andExpect(jsonPath("$.data.totalPosts").value(1))
                .andExpect(jsonPath("$.data.totalComments").value(2))
                .andExpect(jsonPath("$.data.followersCount").value(1))
                .andExpect(jsonPath("$.data.followingCount").value(1));
    }

    // User xem danh sách followers của chính mình.
    @Test
    @WithMockUser(username = "followed_user", roles = {"USER"})
    public void testGetMyFollowers_Success() throws Exception {
        User target = createUser("followed_user", "followed_user@example.com", RoleType.USER);
        User follower = createUser("follower_user", "follower_user@example.com", RoleType.USER);
        saveFollow(follower, target);

        mockMvc.perform(get("/api/users/me/followers")
                .param("pageNo", "0")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].username").value("follower_user"));
    }

    // User xem danh sách tài khoản mình đang theo dõi.
    @Test
    @WithMockUser(username = "follower_list_user", roles = {"USER"})
    public void testGetMyFollowing_Success() throws Exception {
        User follower = createUser("follower_list_user", "follower_list_user@example.com", RoleType.USER);
        User target = createUser("following_target", "following_target@example.com", RoleType.USER);
        saveFollow(follower, target);

        mockMvc.perform(get("/api/users/me/following")
                .param("pageNo", "0")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].username").value("following_target"));
    }

    // User cập nhật hồ sơ cá nhân thành công với fullName và biography mới.
    @Test
    @WithMockUser(username = "profile_editor", roles = {"USER"})
    public void testUpdateCurrentUser_Success() throws Exception {
        createUser("profile_editor", "profile_editor@example.com", RoleType.USER);

        UserUpdateRequest request = UserUpdateRequest.builder()
                .fullName("Profile Editor Updated")
                .biography("Tiểu sử mới")
                .build();

        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fullName").value("Profile Editor Updated"))
                .andExpect(jsonPath("$.data.biography").value("Tiểu sử mới"));
    }

    // Validate request cập nhật hồ sơ khi biography vượt quá giới hạn cho phép.
    @Test
    @WithMockUser(username = "profile_validation", roles = {"USER"})
    public void testUpdateCurrentUser_ValidationFail() throws Exception {
        createUser("profile_validation", "profile_validation@example.com", RoleType.USER);

        UserUpdateRequest request = UserUpdateRequest.builder()
                .biography("a".repeat(501))
                .build();

        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.biography", containsString("Tiểu sử không được vượt quá 500 ký tự")));
    }

    // ADMIN lấy danh sách toàn bộ user theo dạng phân trang.
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetAllUsers_Success_WithAdmin() throws Exception {
        createUser("list_user_one", "list_user_one@example.com", RoleType.USER);
        createUser("list_user_two", "list_user_two@example.com", RoleType.EDITOR);

        mockMvc.perform(get("/api/users")
                .param("pageNo", "0")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ADMIN cập nhật role của một user khác thành công.
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testUpdateUserRole_Success_WithAdmin() throws Exception {
        User user = createUser("role_target", "role_target@example.com", RoleType.USER);

        mockMvc.perform(put("/api/users/{id}/role", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"EDITOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("role_target"))
                .andExpect(jsonPath("$.data.role").value("EDITOR"));
    }

    // ADMIN xóa thành công một user thường không có ràng buộc dữ liệu khác.
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testDeleteUser_Success_WithAdmin() throws Exception {
        User user = createUser("delete_user_target", "delete_user_target@example.com", RoleType.USER);

        mockMvc.perform(delete("/api/users/{id}", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Xóa người dùng thành công"));
    }

    // USER thường không có quyền truy cập danh sách toàn bộ người dùng.
    @Test
    @WithMockUser(username = "plain_user", roles = {"USER"})
    public void testGetAllUsers_Forbidden_WithNonAdmin() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này"));
    }

    private User createUser(String username, String email, RoleType roleType) {
        Role role = roleRepository.findByRoleName(roleType)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role " + roleType));

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode("Password@123"))
                .fullName("Full Name " + username)
                .roles(new HashSet<>(Set.of(role)))
                .build();

        return userRepository.save(user);
    }

    private Category saveCategory(String name, String slug) {
        return categoryRepository.save(Category.builder()
                .name(name)
                .slug(slug)
                .description("Category for user controller test")
                .build());
    }

    private Post savePost(User author, Category category, String title, String slug, PostStatus status) {
        return postRepository.save(Post.builder()
                .title(title)
                .slug(slug)
                .summary("Summary " + title)
                .contentMarkdown("Content " + title)
                .author(author)
                .category(category)
                .status(status)
                .build());
    }

    private Comment saveComment(Post post, User author, String content, Comment parent) {
        return commentRepository.save(Comment.builder()
                .post(post)
                .author(author)
                .content(content)
                .parent(parent)
                .build());
    }

    private UserFollow saveFollow(User follower, User following) {
        return userFollowRepository.save(UserFollow.builder()
                .follower(follower)
                .following(following)
                .build());
    }
}
