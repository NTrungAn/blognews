package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.dto.response.PostResponse;
import com.blog.blogsystem.entity.Bookmark;
import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.mapper.PostMapper;
import com.blog.blogsystem.repository.BookmarkRepository;
import com.blog.blogsystem.repository.PostRepository;
import com.blog.blogsystem.repository.UserRepository;
import com.blog.blogsystem.service.impl.BookmarkServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private BookmarkServiceImpl bookmarkService;

    // Khi chưa có bookmark, service phải tạo mới và trả đúng thông báo thành công.
    @Test
    public void testToggleBookmark_AddSuccess() {
        User user = createUser("bookmark_user");
        Post post = createPost();

        when(userRepository.findByUsername("bookmark_user")).thenReturn(Optional.of(user));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(bookmarkRepository.findByUserAndPost(user, post)).thenReturn(Optional.empty());

        String result = bookmarkService.toggleBookmark(post.getId(), "bookmark_user");

        assertEquals("Đã lưu bài viết thành công", result);
        verify(bookmarkRepository, times(1)).save(any(Bookmark.class));
        verify(bookmarkRepository, never()).delete(any());
    }

    // Khi bookmark đã tồn tại, service phải xóa bookmark cũ và trả thông báo bỏ lưu.
    @Test
    public void testToggleBookmark_RemoveSuccess() {
        User user = createUser("bookmark_user");
        Post post = createPost();
        Bookmark existingBookmark = Bookmark.builder().user(user).post(post).build();

        when(userRepository.findByUsername("bookmark_user")).thenReturn(Optional.of(user));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(bookmarkRepository.findByUserAndPost(user, post)).thenReturn(Optional.of(existingBookmark));

        String result = bookmarkService.toggleBookmark(post.getId(), "bookmark_user");

        assertEquals("Đã bỏ lưu bài viết", result);
        verify(bookmarkRepository, times(1)).delete(existingBookmark);
        verify(bookmarkRepository, never()).save(any(Bookmark.class));
    }

    // Trạng thái bookmark phải trả về true khi tồn tại bản ghi bookmark tương ứng.
    @Test
    public void testCheckBookmarkStatus_ReturnsTrue() {
        User user = createUser("bookmark_user");
        Post post = createPost();
        Bookmark existingBookmark = Bookmark.builder().user(user).post(post).build();

        when(userRepository.findByUsername("bookmark_user")).thenReturn(Optional.of(user));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(bookmarkRepository.findByUserAndPost(user, post)).thenReturn(Optional.of(existingBookmark));

        boolean result = bookmarkService.checkBookmarkStatus(post.getId(), "bookmark_user");

        assertTrue(result);
    }

    // Trạng thái bookmark phải trả về false khi chưa có bản ghi bookmark.
    @Test
    public void testCheckBookmarkStatus_ReturnsFalse() {
        User user = createUser("bookmark_user");
        Post post = createPost();

        when(userRepository.findByUsername("bookmark_user")).thenReturn(Optional.of(user));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(bookmarkRepository.findByUserAndPost(user, post)).thenReturn(Optional.empty());

        boolean result = bookmarkService.checkBookmarkStatus(post.getId(), "bookmark_user");

        assertFalse(result);
    }

    // Danh sách bookmark cá nhân phải được map sang PageResponse<PostResponse> đúng metadata phân trang.
    @Test
    public void testGetMyBookmarks_MapsPageResponse() {
        User user = createUser("bookmark_user");
        Post post = createPost();
        Bookmark bookmark = Bookmark.builder().user(user).post(post).build();
        PostResponse mappedResponse = new PostResponse();
        mappedResponse.setTitle("Saved post");

        when(userRepository.findByUsername("bookmark_user")).thenReturn(Optional.of(user));
        when(bookmarkRepository.findByUser(user, PageRequest.of(0, 5, org.springframework.data.domain.Sort.by("createdAt").descending())))
                .thenReturn(new PageImpl<>(List.of(bookmark), PageRequest.of(0, 5), 1));
        when(postMapper.toResponse(post)).thenReturn(mappedResponse);

        PageResponse<PostResponse> result = bookmarkService.getMyBookmarks("bookmark_user", 0, 5);

        assertEquals(1, result.getContent().size());
        assertEquals("Saved post", result.getContent().get(0).getTitle());
        assertEquals(0, result.getPageNo());
        assertEquals(5, result.getPageSize());
        assertEquals(1, result.getTotalElements());
        assertTrue(result.isLast());
    }

    private User createUser(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        return user;
    }

    private Post createPost() {
        Post post = new Post();
        post.setId(UUID.randomUUID());
        post.setTitle("Test Post");
        return post;
    }
}
