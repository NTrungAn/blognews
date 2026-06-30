package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.response.PostLikeResponse;
import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.PostLike;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.NotificationType;
import com.blog.blogsystem.repository.PostLikeRepository;
import com.blog.blogsystem.repository.PostRepository;
import com.blog.blogsystem.repository.UserRepository;
import com.blog.blogsystem.service.NotificationService;
import com.blog.blogsystem.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReactionServiceImpl implements ReactionService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public PostLikeResponse toggleLikePost(UUID postId, String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết"));

        Optional<PostLike> existingLike = postLikeRepository.findByPostAndUser(post, user);

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
        } else {
            PostLike newLike = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();
            postLikeRepository.save(newLike);

            // Gửi thông báo cho tác giả bài viết
            if (!post.getAuthor().getId().equals(user.getId())) {
                String content = user.getUsername() + " đã thích bài viết của bạn: \"" + truncate(post.getTitle(), 50) + "\"";
                String targetUrl = "/blog/" + post.getSlug();
                notificationService.createNotification(post.getAuthor(), user, NotificationType.REACTION, content, targetUrl);
            }
        }

        int likesCount = (int) postLikeRepository.countByPost_Id(postId);
        post.setLikesCount(likesCount);
        postRepository.save(post);

        boolean liked = postLikeRepository.existsByPostAndUser(post, user);
        return PostLikeResponse.builder()
                .liked(liked)
                .likesCount(likesCount)
                .build();
    }

    private String truncate(String text, int length) {
        if (text == null) return "";
        return text.length() > length ? text.substring(0, length) + "..." : text;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPostLiked(UUID postId, String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết"));

        return postLikeRepository.existsByPostAndUser(post, user);
    }
}
