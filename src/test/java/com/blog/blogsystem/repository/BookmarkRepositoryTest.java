package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.Bookmark;
import com.blog.blogsystem.entity.Category;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class BookmarkRepositoryTest {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    // Truy vấn theo user và post phải lấy đúng bookmark đã lưu.
    @Test
    public void testFindByUserAndPost_Success() {
        User user = saveUser("bookmark_repo_user");
        User author = saveUser("bookmark_repo_author");
        Category category = saveCategory("Bookmark Repo Category", "bookmark-repo-category");
        Post post = savePost(author, category, "Bookmark Repo Post", "bookmark-repo-post");
        Bookmark bookmark = bookmarkRepository.save(Bookmark.builder().user(user).post(post).build());

        Optional<Bookmark> result = bookmarkRepository.findByUserAndPost(user, post);

        assertTrue(result.isPresent());
        assertEquals(bookmark.getId(), result.get().getId());
    }

    // Phân trang bookmark theo user phải trả đúng số lượng bản ghi của user đó.
    @Test
    public void testFindByUser_PagedSuccess() {
        User user = saveUser("bookmark_repo_page_user");
        User author = saveUser("bookmark_repo_page_author");
        Category category = saveCategory("Bookmark Repo Page Category", "bookmark-repo-page-category");
        Post postOne = savePost(author, category, "Page Post One", "page-post-one");
        Post postTwo = savePost(author, category, "Page Post Two", "page-post-two");
        bookmarkRepository.save(Bookmark.builder().user(user).post(postOne).build());
        bookmarkRepository.save(Bookmark.builder().user(user).post(postTwo).build());

        Page<Bookmark> result = bookmarkRepository.findByUser(user, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
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
                .description("Category for bookmark repository test")
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
