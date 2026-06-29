package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.response.PostLikeResponse;
import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.PostLike;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.repository.PostLikeRepository;
import com.blog.blogsystem.repository.PostRepository;
import com.blog.blogsystem.repository.UserRepository;
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
