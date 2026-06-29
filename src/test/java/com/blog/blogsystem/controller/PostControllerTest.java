package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.request.PostRequest;
import com.blog.blogsystem.entity.Category;
import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.Role;
import com.blog.blogsystem.entity.Tag;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.PostStatus;
import com.blog.blogsystem.entity.enums.RoleType;
import com.blog.blogsystem.repository.CategoryRepository;
import com.blog.blogsystem.repository.PostRepository;
import com.blog.blogsystem.repository.RoleRepository;
import com.blog.blogsystem.repository.TagRepository;
import com.blog.blogsystem.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // API public lấy danh sách post theo filter cơ bản và trả về PageResponse.
    @Test
    public void testGetAllPosts_Success() throws Exception {
        String suffix = uniqueSuffix();
        User author = createUser("public_author_" + suffix, "public_" + suffix + "@example.com", RoleType.USER);
        Category category = saveCategory("Public Category " + suffix, "public-category-" + suffix);
        Post post = savePost(author, category, null, "Public Post " + suffix, "public-post-" + suffix, PostStatus.PUBLISHED);

        mockMvc.perform(get("/api/posts")
                .param("pageNo", "0")
                .param("pageSize", "10")
                .param("keyword", suffix)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].slug").value(post.getSlug()));
    }

    // API public lấy chi tiết post theo slug và đồng thời tăng viewCount.
    @Test
    public void testGetPostBySlug_Success() throws Exception {
        String suffix = uniqueSuffix();
        User author = createUser("slug_author_" + suffix, "slug_" + suffix + "@example.com", RoleType.USER);
        Category category = saveCategory("Slug Category " + suffix, "slug-category-" + suffix);
        Post post = savePost(author, category, null, "Slug Post " + suffix, "slug-post-" + suffix, PostStatus.PUBLISHED);

        mockMvc.perform(get("/api/posts/slug/{slug}", post.getSlug())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.slug").value(post.getSlug()))
                .andExpect(jsonPath("$.data.viewCount").value(1));
    }

    // Lấy post với slug không tồn tại phải rơi vào lỗi từ service hiện tại.
    @Test
    public void testGetPostBySlug_NotFound() throws Exception {
        mockMvc.perform(get("/api/posts/slug/bai-viet-khong-ton-tai-123456")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message", containsString("Không tìm thấy bài viết")));
    }

    // Tạo post khi chưa đăng nhập phải bị Spring Security chặn.
    @Test
    public void testCreatePost_Unauthorized_NoToken() throws Exception {
        PostRequest request = new PostRequest();
        request.setTitle("Unauthorized Post");
        request.setContentMarkdown("Nội dung");

        MockMultipartFile data = jsonPart("data", request);

        mockMvc.perform(multipart("/api/posts")
                .file(data))
                .andExpect(status().isForbidden());
    }

    // User đã đăng nhập nhưng gửi request thiếu title/category nên phải bị validate thủ công trong controller.
    @Test
    @WithMockUser(username = "create_author", roles = {"USER"})
    public void testCreatePost_ValidationFail_WithMockUser() throws Exception {
        PostRequest request = new PostRequest();
        request.setTitle("   ");
        request.setContentMarkdown("Nội dung hợp lệ");

        MockMultipartFile data = jsonPart("data", request);

        mockMvc.perform(multipart("/api/posts")
                .file(data))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Tiêu đề không được để trống"));
    }

    // User hợp lệ tạo post thành công bằng multipart/form-data.
    @Test
    @WithMockUser(username = "create_owner", roles = {"USER"})
    public void testCreatePost_Success_WithMockUser() throws Exception {
        User author = createUser("create_owner", "create_owner@example.com", RoleType.USER);
        Category category = saveCategory("Create Category", "create-category");
        Tag tag = saveTag("CreateTag", "create-tag");

        PostRequest request = new PostRequest();
        request.setTitle("Create Post Success");
        request.setSummary("Tóm tắt bài viết");
        request.setContentMarkdown("## Nội dung bài viết");
        request.setCategoryId(category.getId());
        request.setTagIds(Set.of(tag.getId()));
        request.setStatus(PostStatus.PUBLISHED);

        MockMultipartFile data = jsonPart("data", request);

        mockMvc.perform(multipart("/api/posts")
                .file(data))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.title").value("Create Post Success"))
                .andExpect(jsonPath("$.data.authorUsername").value(author.getUsername()))
                .andExpect(jsonPath("$.data.category.id").value(category.getId().toString()))
                .andExpect(jsonPath("$.data.tags[0].slug").value(tag.getSlug()));
    }

    // API my-posts chỉ trả về bài viết của chính user đang đăng nhập.
    @Test
    @WithMockUser(username = "my_posts_author", roles = {"USER"})
    public void testGetMyPosts_Success_WithMockUser() throws Exception {
        String suffix = uniqueSuffix();
        User author = createUser("my_posts_author", "my_posts_author@example.com", RoleType.USER);
        User otherAuthor = createUser("other_author_" + suffix, "other_" + suffix + "@example.com", RoleType.USER);
        Category category = saveCategory("My Posts Category " + suffix, "my-posts-category-" + suffix);

        savePost(author, category, null, "My Post " + suffix, "my-post-" + suffix, PostStatus.DRAFT);
        savePost(otherAuthor, category, null, "Other Post " + suffix, "other-post-" + suffix, PostStatus.PUBLISHED);

        mockMvc.perform(get("/api/posts/my-posts")
                .param("pageNo", "0")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].authorUsername").value(author.getUsername()))
                .andExpect(jsonPath("$.data.content[0].title").value("My Post " + suffix));
    }

    // Chủ sở hữu bài viết được phép cập nhật post bằng request multipart kiểu PUT.
    @Test
    @WithMockUser(username = "post_owner", roles = {"USER"})
    public void testUpdatePost_Success_WithOwner() throws Exception {
        User owner = createUser("post_owner", "post_owner@example.com", RoleType.USER);
        Category category = saveCategory("Update Category", "update-category");
        Tag tag = saveTag("UpdateTag", "update-tag");
        Post post = savePost(owner, category, Set.of(tag), "Original Post", "original-post", PostStatus.DRAFT);

        PostRequest request = new PostRequest();
        request.setTitle("Updated Post");
        request.setSummary("Tóm tắt mới");
        request.setContentMarkdown("Nội dung mới");
        request.setCategoryId(category.getId());
        request.setTagIds(Set.of(tag.getId()));
        request.setStatus(PostStatus.PUBLISHED);

        MockMultipartFile data = jsonPart("data", request);

        mockMvc.perform(multipart("/api/posts/{id}", post.getId())
                .file(data)
                .with(req -> {
                    req.setMethod("PUT");
                    return req;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Updated Post"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    // Chủ sở hữu bài viết được phép xóa bài viết của mình.
    @Test
    @WithMockUser(username = "delete_owner", roles = {"USER"})
    public void testDeletePost_Success_WithOwner() throws Exception {
        User owner = createUser("delete_owner", "delete_owner@example.com", RoleType.USER);
        Category category = saveCategory("Delete Post Category", "delete-post-category");
        Post post = savePost(owner, category, null, "Delete Me", "delete-me", PostStatus.DRAFT);

        mockMvc.perform(delete("/api/posts/{id}", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data").value("Xóa bài viết thành công!"));
    }

    private MockMultipartFile jsonPart(String name, Object value) throws Exception {
        return new MockMultipartFile(
                name,
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_8)
        );
    }

    private User createUser(String username, String email, RoleType roleType) {
        Role role = roleRepository.findByRoleName(roleType)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role " + roleType));

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode("Password@123"))
                .fullName("Test User " + username)
                .roles(Set.of(role))
                .build();

        return userRepository.save(user);
    }

    private Category saveCategory(String name, String slug) {
        return categoryRepository.save(Category.builder()
                .name(name)
                .slug(slug)
                .description("Category for post test")
                .build());
    }

    private Tag saveTag(String name, String slug) {
        return tagRepository.save(Tag.builder()
                .name(name)
                .slug(slug)
                .build());
    }

    private Post savePost(User author, Category category, Set<Tag> tags, String title, String slug, PostStatus status) {
        Post post = Post.builder()
                .title(title)
                .slug(slug)
                .summary("Summary for " + title)
                .contentMarkdown("Content for " + title)
                .author(author)
                .category(category)
                .tags(tags)
                .status(status)
                .build();
        return postRepository.save(post);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
