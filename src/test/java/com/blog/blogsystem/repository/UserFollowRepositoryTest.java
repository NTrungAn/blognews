package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.UserFollow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class UserFollowRepositoryTest {

    @Autowired
    private UserFollowRepository userFollowRepository;

    @Autowired
    private UserRepository userRepository;

    // Kiểm tra quan hệ follow tồn tại và các hàm count follower/following trả đúng số lượng.
    @Test
    public void testExistsAndCountFollowRelationship_Success() {
        User follower = saveUser("follow_repo_follower");
        User following = saveUser("follow_repo_following");
        userFollowRepository.save(UserFollow.builder()
                .follower(follower)
                .following(following)
                .build());

        boolean exists = userFollowRepository.existsByFollowerAndFollowing(follower, following);
        long followersCount = userFollowRepository.countByFollowing(following);
        long followingCount = userFollowRepository.countByFollower(follower);

        assertTrue(exists);
        assertEquals(1, followersCount);
        assertEquals(1, followingCount);
    }

    // Danh sách following theo user phải lấy đúng user mà tài khoản hiện tại đang follow.
    @Test
    public void testFindByFollower_ReturnsPagedFollowingRelationships() {
        User follower = saveUser("follow_repo_page_follower");
        User followingOne = saveUser("follow_repo_page_following_one");
        User followingTwo = saveUser("follow_repo_page_following_two");
        userFollowRepository.save(UserFollow.builder()
                .follower(follower)
                .following(followingOne)
                .build());
        userFollowRepository.save(UserFollow.builder()
                .follower(follower)
                .following(followingTwo)
                .build());

        Page<UserFollow> result = userFollowRepository.findByFollower(follower, PageRequest.of(0, 10));

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
}
