package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.response.FollowerResponse;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.UserFollow;
import com.blog.blogsystem.repository.UserFollowRepository;
import com.blog.blogsystem.repository.UserRepository;
import com.blog.blogsystem.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void followUser(String targetUsername, String currentUsername) {
        if (targetUsername.equals(currentUsername)) {
            throw new IllegalArgumentException("Bạn không thể tự theo dõi chính mình");
        }

        User follower = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng hiện tại không tồn tại"));

        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng mục tiêu không tồn tại"));

        if (userFollowRepository.existsByFollowerAndFollowing(follower, targetUser)) {
            throw new IllegalArgumentException("Bạn đã theo dõi người dùng này rồi");
        }

        UserFollow follow = new UserFollow();
        follow.setFollower(follower);
        follow.setFollowing(targetUser);
        userFollowRepository.save(follow);

        // Cập nhật số lượng
        targetUser.setFollowersCount(targetUser.getFollowersCount() + 1);
        follower.setFollowingCount(follower.getFollowingCount() + 1);
        userRepository.save(targetUser);
        userRepository.save(follower);
    }

    @Override
    @Transactional
    public void unfollowUser(String targetUsername, String currentUsername) {
        User follower = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng hiện tại không tồn tại"));

        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng mục tiêu không tồn tại"));

        UserFollow follow = userFollowRepository.findByFollowerAndFollowing(follower, targetUser)
                .orElseThrow(() -> new IllegalArgumentException("Bạn chưa theo dõi người dùng này"));

        userFollowRepository.delete(follow);

        // Cập nhật số lượng
        targetUser.setFollowersCount(Math.max(0, targetUser.getFollowersCount() - 1));
        follower.setFollowingCount(Math.max(0, follower.getFollowingCount() - 1));
        userRepository.save(targetUser);
        userRepository.save(follower);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowerResponse> getFollowers(String username, int pageNo, int pageSize) {
        User targetUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
        Page<UserFollow> followPage = userFollowRepository.findByFollowing(targetUser, pageable);

        List<FollowerResponse> responses = followPage.getContent().stream()
                .map(follow -> FollowerResponse.builder()
                        .id(follow.getFollower().getId())
                        .username(follow.getFollower().getUsername())
                        .fullName(follow.getFollower().getFullName())
                        .avatar(follow.getFollower().getAvatar())
                        .build())
                .collect(Collectors.toList());

        return PageResponse.<FollowerResponse>builder()
                .content(responses)
                .pageNo(followPage.getNumber())
                .pageSize(followPage.getSize())
                .totalElements(followPage.getTotalElements())
                .totalPages(followPage.getTotalPages())
                .last(followPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowerResponse> getFollowing(String username, int pageNo, int pageSize) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
        Page<UserFollow> followPage = userFollowRepository.findByFollower(user, pageable);

        List<FollowerResponse> responses = followPage.getContent().stream()
                .map(follow -> FollowerResponse.builder()
                        .id(follow.getFollowing().getId())
                        .username(follow.getFollowing().getUsername())
                        .fullName(follow.getFollowing().getFullName())
                        .avatar(follow.getFollowing().getAvatar())
                        .build())
                .collect(Collectors.toList());

        return PageResponse.<FollowerResponse>builder()
                .content(responses)
                .pageNo(followPage.getNumber())
                .pageSize(followPage.getSize())
                .totalElements(followPage.getTotalElements())
                .totalPages(followPage.getTotalPages())
                .last(followPage.isLast())
                .build();
    }
}
