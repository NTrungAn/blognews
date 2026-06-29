package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.Category;
import com.blog.blogsystem.entity.Comment;
import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.PostStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    // Query lấy comment gốc phải loại bỏ replies và chỉ trả về top-level comments.
    @Test
    public void testFindByPostIdAndParentIsNull_ReturnsOnlyTopLevelComments() {
        User author = saveUser("comment_repo_author");
        User commenter = saveUser("comment_repo_commenter");
        Category category = saveCategory("Comment Repo Category", "comment-repo-category");
        Post post = savePost(author, category, "Comment Repo Post", "comment-repo-post");
        Comment parent = commentRepository.save(Comment.builder()
                .post(post)
                .author(commenter)
                .content("Parent comment")
                .build());
        commentRepository.save(Comment.builder()
                .post(post)
                .author(commenter)
                .content("Reply comment")
                .parent(parent)
                .build());
        commentRepository.save(Comment.builder()
                .post(post)
                .author(commenter)
                .content("Another top level comment")
                .build());

        Page<Comment> result = commentRepository.findByPostIdAndParentIsNull(post.getId(), PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }

    // Đếm comment theo author username phải tính cả comment gốc và reply của user đó.
    @Test
    public void testCountByAuthorUsername_CountsAllComments() {
        User author = saveUser("comment_count_author");
        User commenter = saveUser("comment_count_user");
        Category category = saveCategory("Comment Count Category", "comment-count-category");
        Post post = savePost(author, category, "Comment Count Post", "comment-count-post");
        Comment parent = commentRepository.save(Comment.builder()
                .post(post)
                .author(commenter)
                .content("First comment")
                .build());
        commentRepository.save(Comment.builder()
                .post(post)
                .author(commenter)
                .content("Reply comment")
                .parent(parent)
                .build());

        long result = commentRepository.countByAuthorUsername("comment_count_user");

        assertEquals(2, result);
    }

    private User saveUser(String username) {
        User user = User.builder()
                .username(username)
                .email(username + "@example.com")
                .passwordHash("encoded-password")
                .fullName("Full Name " + username)
                .roles(new HashSet<>())
                .build();
        return userRepository.save(user);
    }

    private Category saveCategory(String name, String slug) {
        return categoryRepository.save(Category.builder()
                .name(name)
                .slug(slug)
                .description("Category for comment repository test")
                .build());
    }

    private Post savePost(User author, Category category, String title, String slug) {
        return postRepository.save(Post.builder()
                .title(title)
                .slug(slug)
                .summary("Summary " + title)
                .contentMarkdown("Content " + title)
                .author(author)
                .category(category)
                .status(PostStatus.PUBLISHED)
                .build());
    }
}
