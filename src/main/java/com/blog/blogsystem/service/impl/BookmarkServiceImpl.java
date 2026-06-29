package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.dto.response.PostResponse;
import com.blog.blogsystem.entity.Bookmark;
import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.mapper.PostMapper;
import com.blog.blogsystem.repository.BookmarkRepository;
import com.blog.blogsystem.repository.PostRepository;
import com.blog.blogsystem.repository.UserRepository;
import com.blog.blogsystem.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostMapper postMapper;

    @Override
    @Transactional
    public String toggleBookmark(UUID postId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Bài viết không tồn tại"));

        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserAndPost(user, post);

        if (existingBookmark.isPresent()) {
            bookmarkRepository.delete(existingBookmark.get());
            return "Đã bỏ lưu bài viết";
        } else {
            Bookmark newBookmark = Bookmark.builder()
                    .user(user)
                    .post(post)
                    .build();
            bookmarkRepository.save(newBookmark);
            return "Đã lưu bài viết thành công";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkBookmarkStatus(UUID postId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Bài viết không tồn tại"));

        return bookmarkRepository.findByUserAndPost(user, post).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getMyBookmarks(String username, int pageNo, int pageSize) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
        Page<Bookmark> bookmarkPage = bookmarkRepository.findByUser(user, pageable);

        List<PostResponse> content = bookmarkPage.getContent().stream()
                .map(bookmark -> postMapper.toResponse(bookmark.getPost()))
                .collect(Collectors.toList());

        return PageResponse.<PostResponse>builder()
                .content(content)
                .pageNo(bookmarkPage.getNumber())
                .pageSize(bookmarkPage.getSize())
                .totalElements(bookmarkPage.getTotalElements())
                .totalPages(bookmarkPage.getTotalPages())
                .last(bookmarkPage.isLast())
                .build();
    }
}
