package com.blog.blogsystem.controller;

import com.blog.blogsystem.entity.Bookmark;
import com.blog.blogsystem.entity.Category;
import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.Role;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.PostStatus;
import com.blog.blogsystem.entity.enums.RoleType;
import com.blog.blogsystem.repository.BookmarkRepository;
import com.blog.blogsystem.repository.CategoryRepository;
import com.blog.blogsystem.repository.PostRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class BookmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    // User xem danh sách bookmark của chính mình qua endpoint /bookmarks/me.
    @Test
    @WithMockUser(username = "bookmark_user", roles = {"USER"})
    public void testGetMyBookmarks_Success() throws Exception {
        User user = createUser("bookmark_user", "bookmark_user@example.com", RoleType.USER);
        User author = createUser("bookmark_author", "bookmark_author@example.com", RoleType.USER);
        Category category = saveCategory("Bookmark Category", "bookmark-category");
        Post post = savePost(author, category, "Bookmarked Post", "bookmarked-post", PostStatus.PUBLISHED);
        saveBookmark(user, post);

        mockMvc.perform(get("/api/bookmarks/me")
                .param("pageNo", "0")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].title").value("Bookmarked Post"));
    }

    // User tạo bookmark mới thành công khi bài viết chưa được lưu trước đó.
    @Test
    @WithMockUser(username = "toggle_user", roles = {"USER"})
    public void testToggleBookmark_AddSuccess() throws Exception {
        createUser("toggle_user", "toggle_user@example.com", RoleType.USER);
        User author = createUser("toggle_author", "toggle_author@example.com", RoleType.USER);
        Category category = saveCategory("Toggle Category", "toggle-category");
        Post post = savePost(author, category, "Toggle Post", "toggle-post", PostStatus.PUBLISHED);

        mockMvc.perform(post("/api/bookmarks/{postId}", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Đã lưu bài viết thành công"));
    }

    // User bỏ bookmark thành công khi bài viết đã được lưu trước đó.
    @Test
    @WithMockUser(username = "toggle_remove_user", roles = {"USER"})
    public void testToggleBookmark_RemoveSuccess() throws Exception {
        User user = createUser("toggle_remove_user", "toggle_remove_user@example.com", RoleType.USER);
        User author = createUser("toggle_remove_author", "toggle_remove_author@example.com", RoleType.USER);
        Category category = saveCategory("Toggle Remove Category", "toggle-remove-category");
        Post post = savePost(author, category, "Toggle Remove Post", "toggle-remove-post", PostStatus.PUBLISHED);
        saveBookmark(user, post);

        mockMvc.perform(post("/api/bookmarks/{postId}", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Đã bỏ lưu bài viết"));
    }

    // User kiểm tra trạng thái bookmark và nhận true khi đã bookmark bài viết.
    @Test
    @WithMockUser(username = "status_user", roles = {"USER"})
    public void testCheckBookmarkStatus_True() throws Exception {
        User user = createUser("status_user", "status_user@example.com", RoleType.USER);
        User author = createUser("status_author", "status_author@example.com", RoleType.USER);
        Category category = saveCategory("Status Category", "status-category");
        Post post = savePost(author, category, "Status Post", "status-post", PostStatus.PUBLISHED);
        saveBookmark(user, post);

        mockMvc.perform(get("/api/bookmarks/{postId}/status", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    // User kiểm tra trạng thái bookmark và nhận false khi chưa bookmark bài viết.
    @Test
    @WithMockUser(username = "status_false_user", roles = {"USER"})
    public void testCheckBookmarkStatus_False() throws Exception {
        createUser("status_false_user", "status_false_user@example.com", RoleType.USER);
        User author = createUser("status_false_author", "status_false_author@example.com", RoleType.USER);
        Category category = saveCategory("Status False Category", "status-false-category");
        Post post = savePost(author, category, "Status False Post", "status-false-post", PostStatus.PUBLISHED);

        mockMvc.perform(get("/api/bookmarks/{postId}/status", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(false));
    }

    // Người chưa đăng nhập không được phép xem danh sách bookmark cá nhân.
    @Test
    public void testGetMyBookmarks_Forbidden_Anonymous() throws Exception {
        mockMvc.perform(get("/api/bookmarks/me"))
                .andExpect(status().isForbidden());
    }

    private User createUser(String username, String email, RoleType roleType) {
        Role role = roleRepository.findByRoleName(roleType)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role " + roleType));

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode("Password@123"))
                .fullName("Full Name " + username)
                .roles(Set.of(role))
                .build();

        return userRepository.save(user);
    }

    private Category saveCategory(String name, String slug) {
        return categoryRepository.save(Category.builder()
                .name(name)
                .slug(slug)
                .description("Category for bookmark controller test")
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

    private Bookmark saveBookmark(User user, Post post) {
        return bookmarkRepository.save(Bookmark.builder()
                .user(user)
                .post(post)
                .build());
    }
}
