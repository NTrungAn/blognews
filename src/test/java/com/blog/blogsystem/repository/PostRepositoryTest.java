package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.Category;
import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.enums.PostStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    public void testFindBySlug_Success() {
        // Arrange
        Category category = new Category();
        category.setName("Tech");
        category.setSlug("tech-category");
        category = categoryRepository.save(category);

        Post post = new Post();
        post.setTitle("Test Post");
        post.setSlug("test-post-repository");
        post.setContentMarkdown("Nội dung bài viết");
        post.setStatus(PostStatus.PUBLISHED);
        post.setCategory(category);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);

        // Act
        Optional<Post> foundPost = postRepository.findBySlug("test-post-repository");

        // Assert
        assertTrue(foundPost.isPresent());
        assertEquals("Test Post", foundPost.get().getTitle());
    }
}
