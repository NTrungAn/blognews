package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.request.CommentRequest;
import com.blog.blogsystem.dto.request.ReactionRequest;
import com.blog.blogsystem.entity.Category;
import com.blog.blogsystem.entity.Comment;
import com.blog.blogsystem.entity.CommentReaction;
import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.Role;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.PostStatus;
import com.blog.blogsystem.entity.enums.RoleType;
import com.blog.blogsystem.repository.CategoryRepository;
import com.blog.blogsystem.repository.CommentReactionRepository;
import com.blog.blogsystem.repository.CommentRepository;
import com.blog.blogsystem.repository.PostRepository;
import com.blog.blogsystem.repository.RoleRepository;
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

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentReactionRepository commentReactionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // API public lấy danh sách comment gốc của bài viết và trả về cấu trúc phân trang.
    @Test
    public void testGetCommentsByPost_Success() throws Exception {
        User author = createUser("comment_author_public", "comment_author_public@example.com", RoleType.USER);
        User commenter = createUser("commenter_public", "commenter_public@example.com", RoleType.USER);
        Category category = saveCategory("Comment Public Category", "comment-public-category");
        Post post = savePost(author, category, "Comment Public Post", "comment-public-post", PostStatus.PUBLISHED);
        saveComment(post, commenter, "Đây là bình luận công khai", null);

        mockMvc.perform(get("/api/posts/{postId}/comments", post.getId())
                .param("pageNo", "0")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].content").value("Đây là bình luận công khai"))
                .andExpect(jsonPath("$.data.content[0].author.username").value("commenter_public"));
    }

    // User tạo bình luận gốc thành công cho một bài viết hợp lệ.
    @Test
    @WithMockUser(username = "comment_creator", roles = {"USER"})
    public void testCreateComment_Success() throws Exception {
        createUser("comment_creator", "comment_creator@example.com", RoleType.USER);
        User postAuthor = createUser("comment_post_author", "comment_post_author@example.com", RoleType.USER);
        Category category = saveCategory("Comment Create Category", "comment-create-category");
        Post post = savePost(postAuthor, category, "Comment Create Post", "comment-create-post", PostStatus.PUBLISHED);

        CommentRequest request = new CommentRequest();
        request.setContent("Bình luận mới");

        mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.content").value("Bình luận mới"))
                .andExpect(jsonPath("$.data.author.username").value("comment_creator"));
    }

    // User tạo reply thành công khi parentId thuộc đúng bài viết hiện tại.
    @Test
    @WithMockUser(username = "reply_creator", roles = {"USER"})
    public void testCreateReplyComment_Success() throws Exception {
        User replier = createUser("reply_creator", "reply_creator@example.com", RoleType.USER);
        User postAuthor = createUser("reply_post_author", "reply_post_author@example.com", RoleType.USER);
        User parentAuthor = createUser("reply_parent_author", "reply_parent_author@example.com", RoleType.USER);
        Category category = saveCategory("Reply Category", "reply-category");
        Post post = savePost(postAuthor, category, "Reply Post", "reply-post", PostStatus.PUBLISHED);
        Comment parent = saveComment(post, parentAuthor, "Bình luận cha", null);

        CommentRequest request = new CommentRequest();
        request.setContent("Đây là reply");
        request.setParentId(parent.getId());

        mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.content").value("Đây là reply"))
                .andExpect(jsonPath("$.data.parentId").value(parent.getId().toString()))
                .andExpect(jsonPath("$.data.author.username").value(replier.getUsername()));
    }

    // Validate request tạo comment khi content bị bỏ trống.
    @Test
    @WithMockUser(username = "comment_validator", roles = {"USER"})
    public void testCreateComment_ValidationFail_BlankContent() throws Exception {
        createUser("comment_validator", "comment_validator@example.com", RoleType.USER);
        User postAuthor = createUser("comment_validation_author", "comment_validation_author@example.com", RoleType.USER);
        Category category = saveCategory("Comment Validation Category", "comment-validation-category");
        Post post = savePost(postAuthor, category, "Comment Validation Post", "comment-validation-post", PostStatus.PUBLISHED);

        CommentRequest request = new CommentRequest();
        request.setContent(" ");

        mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Nội dung bình luận hoặc ảnh đính kèm không được để trống"));
    }

    // Chủ bình luận được phép cập nhật lại nội dung comment của mình.
    @Test
    @WithMockUser(username = "comment_owner", roles = {"USER"})
    public void testUpdateComment_Success_WithOwner() throws Exception {
        User owner = createUser("comment_owner", "comment_owner@example.com", RoleType.USER);
        User postAuthor = createUser("comment_update_post_author", "comment_update_post_author@example.com", RoleType.USER);
        Category category = saveCategory("Comment Update Category", "comment-update-category");
        Post post = savePost(postAuthor, category, "Comment Update Post", "comment-update-post", PostStatus.PUBLISHED);
        Comment comment = saveComment(post, owner, "Nội dung cũ", null);

        CommentRequest request = new CommentRequest();
        request.setContent("Nội dung đã cập nhật");

        mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("Nội dung đã cập nhật"));
    }

    // Chủ bình luận được phép xóa comment của mình.
    @Test
    @WithMockUser(username = "comment_delete_owner", roles = {"USER"})
    public void testDeleteComment_Success_WithOwner() throws Exception {
        User owner = createUser("comment_delete_owner", "comment_delete_owner@example.com", RoleType.USER);
        User postAuthor = createUser("comment_delete_post_author", "comment_delete_post_author@example.com", RoleType.USER);
        Category category = saveCategory("Comment Delete Category", "comment-delete-category");
        Post post = savePost(postAuthor, category, "Comment Delete Post", "comment-delete-post", PostStatus.PUBLISHED);
        Comment comment = saveComment(post, owner, "Bình luận sẽ bị xóa", null);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Xóa bình luận thành công!"));
    }

    // User thêm reaction thành công cho một comment hợp lệ.
    @Test
    @WithMockUser(username = "reaction_user", roles = {"USER"})
    public void testAddReaction_Success() throws Exception {
        User reactionUser = createUser("reaction_user", "reaction_user@example.com", RoleType.USER);
        User postAuthor = createUser("reaction_post_author", "reaction_post_author@example.com", RoleType.USER);
        User commentAuthor = createUser("reaction_comment_author", "reaction_comment_author@example.com", RoleType.USER);
        Category category = saveCategory("Reaction Category", "reaction-category");
        Post post = savePost(postAuthor, category, "Reaction Post", "reaction-post", PostStatus.PUBLISHED);
        Comment comment = saveComment(post, commentAuthor, "Comment để react", null);

        ReactionRequest request = new ReactionRequest();
        request.setEmoji("LIKE");

        mockMvc.perform(post("/api/posts/{postId}/comments/{commentId}/reactions", post.getId(), comment.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"));
    }

    // User xóa reaction thành công khi trước đó đã react vào comment.
    @Test
    @WithMockUser(username = "reaction_remove_user", roles = {"USER"})
    public void testRemoveReaction_Success() throws Exception {
        User reactionUser = createUser("reaction_remove_user", "reaction_remove_user@example.com", RoleType.USER);
        User postAuthor = createUser("reaction_remove_post_author", "reaction_remove_post_author@example.com", RoleType.USER);
        User commentAuthor = createUser("reaction_remove_comment_author", "reaction_remove_comment_author@example.com", RoleType.USER);
        Category category = saveCategory("Reaction Remove Category", "reaction-remove-category");
        Post post = savePost(postAuthor, category, "Reaction Remove Post", "reaction-remove-post", PostStatus.PUBLISHED);
        Comment comment = saveComment(post, commentAuthor, "Comment để xóa react", null);
        saveReaction(comment, reactionUser, "LOVE");

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}/reactions", post.getId(), comment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"));
    }

    // Người chưa đăng nhập không được phép tạo comment mới.
    @Test
    public void testCreateComment_Forbidden_Anonymous() throws Exception {
        User postAuthor = createUser("anonymous_comment_post_author", "anonymous_comment_post_author@example.com", RoleType.USER);
        Category category = saveCategory("Anonymous Comment Category", "anonymous-comment-category");
        Post post = savePost(postAuthor, category, "Anonymous Comment Post", "anonymous-comment-post", PostStatus.PUBLISHED);

        CommentRequest request = new CommentRequest();
        request.setContent("Anonymous comment");

        mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
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
                .description("Category for comment controller test")
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

    private CommentReaction saveReaction(Comment comment, User user, String emoji) {
        return commentReactionRepository.save(CommentReaction.builder()
                .comment(comment)
                .user(user)
                .emoji(emoji)
                .build());
    }
}
