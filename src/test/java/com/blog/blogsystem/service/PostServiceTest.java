package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.response.PostResponse;
import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.mapper.PostMapper;
import com.blog.blogsystem.repository.PostRepository;
import com.blog.blogsystem.service.impl.PostServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostServiceImpl postService;

    @Test
    public void testGetPostBySlug_Success() {
        // Arrange
        String slug = "bai-viet-test";
        Post mockPost = new Post();
        mockPost.setId(UUID.randomUUID());
        mockPost.setSlug(slug);
        mockPost.setViewCount(10);

        PostResponse mockResponse = new PostResponse();
        mockResponse.setSlug(slug);

        when(postRepository.findBySlug(slug)).thenReturn(Optional.of(mockPost));
        when(postRepository.save(any(Post.class))).thenReturn(mockPost);
        when(postMapper.toResponse(mockPost)).thenReturn(mockResponse);

        // Act
        PostResponse result = postService.getPostBySlug(slug);

        // Assert
        assertNotNull(result);
        assertEquals(slug, result.getSlug());
        verify(postRepository, times(1)).findBySlug(slug);
        verify(postRepository, times(1)).save(mockPost); // Kiểm tra logic tăng view count
    }

    @Test
    public void testGetPostBySlug_NotFound() {
        // Arrange
        String slug = "bai-viet-khong-ton-tai";
        when(postRepository.findBySlug(slug)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            postService.getPostBySlug(slug);
        });

        assertTrue(exception.getMessage().contains("Không tìm thấy bài viết"));
        verify(postRepository, times(1)).findBySlug(slug);
        verify(postRepository, never()).save(any());
    }
}
