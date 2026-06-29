package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.request.CommentRequest;
import com.blog.blogsystem.dto.response.CommentResponse;
import com.blog.blogsystem.entity.Comment;
import com.blog.blogsystem.entity.CommentReaction;
import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.NotificationType;
import com.blog.blogsystem.mapper.CommentMapper;
import com.blog.blogsystem.repository.CommentReactionRepository;
import com.blog.blogsystem.repository.CommentRepository;
import com.blog.blogsystem.repository.PostRepository;
import com.blog.blogsystem.repository.UserRepository;
import com.blog.blogsystem.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentReactionRepository commentReactionRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentServiceImpl commentService;

    // Tạo bình luận gốc thành công phải lưu comment và gửi notification cho tác giả bài viết.
    @Test
    public void testCreateComment_TopLevelSuccess_SendsPostNotification() {
        User postAuthor = createUser("post_author");
        User commenter = createUser("commenter");
        Post post = createPost("top-level-post", postAuthor);
        CommentRequest request = createCommentRequest("Đây là bình luận gốc", null);
        Comment mappedComment = new Comment();
        Comment savedComment = new Comment();
        savedComment.setId(UUID.randomUUID());
        savedComment.setPost(post);
        savedComment.setAuthor(commenter);
        CommentResponse mappedResponse = new CommentResponse();
        mappedResponse.setContent(request.getContent());

        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(userRepository.findByUsername("commenter")).thenReturn(Optional.of(commenter));
        when(commentMapper.toEntity(request)).thenReturn(mappedComment);
        when(commentRepository.save(mappedComment)).thenReturn(savedComment);
        when(commentMapper.toResponse(savedComment)).thenReturn(mappedResponse);

        CommentResponse result = commentService.createComment(post.getId(), request, "commenter");

        assertEquals("Đây là bình luận gốc", result.getContent());
        verify(commentRepository, times(1)).save(mappedComment);
        verify(notificationService, times(1)).createNotification(
                eq(postAuthor),
                eq(commenter),
                eq(NotificationType.COMMENT),
                org.mockito.ArgumentMatchers.contains("commenter đã bình luận"),
                eq("/blog/top-level-post"));
    }

    // Reply vào comment thuộc bài viết khác phải bị chặn bằng IllegalArgumentException.
    @Test
    public void testCreateComment_ReplyToDifferentPost_ThrowsIllegalArgumentException() {
        User commenter = createUser("commenter");
        Post currentPost = createPost("current-post", createUser("author-one"));
        Post otherPost = createPost("other-post", createUser("author-two"));
        Comment parent = new Comment();
        parent.setId(UUID.randomUUID());
        parent.setPost(otherPost);
        CommentRequest request = createCommentRequest("Reply sai bài", parent.getId());
        Comment mappedComment = new Comment();

        when(postRepository.findById(currentPost.getId())).thenReturn(Optional.of(currentPost));
        when(userRepository.findByUsername("commenter")).thenReturn(Optional.of(commenter));
        when(commentMapper.toEntity(request)).thenReturn(mappedComment);
        when(commentRepository.findById(parent.getId())).thenReturn(Optional.of(parent));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> commentService.createComment(currentPost.getId(), request, "commenter"));

        assertEquals("Bình luận cha không thuộc bài viết này.", exception.getMessage());
        verify(commentRepository, never()).save(any(Comment.class));
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }

    // Chỉ chủ sở hữu comment mới được cập nhật nội dung bình luận.
    @Test
    public void testUpdateComment_NotOwner_ThrowsRuntimeException() {
        User owner = createUser("owner");
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setAuthor(owner);
        CommentRequest request = createCommentRequest("Nội dung mới", null);

        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> commentService.updateComment(comment.getId(), request, "another_user"));

        assertEquals("Bạn không có quyền thao tác với bình luận này.", exception.getMessage());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    // Khi reaction đã tồn tại, service chỉ cập nhật emoji và không tạo notification mới.
    @Test
    public void testAddReaction_ExistingReaction_UpdatesWithoutNotification() {
        User reactor = createUser("reactor");
        User commentAuthor = createUser("comment_author");
        Post post = createPost("reaction-post", commentAuthor);
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setPost(post);
        comment.setAuthor(commentAuthor);

        CommentReaction existingReaction = CommentReaction.builder()
                .id(UUID.randomUUID())
                .comment(comment)
                .user(reactor)
                .emoji("LIKE")
                .build();

        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(userRepository.findByUsername("reactor")).thenReturn(Optional.of(reactor));
        when(commentReactionRepository.findByCommentIdAndUserId(comment.getId(), reactor.getId()))
                .thenReturn(Optional.of(existingReaction));

        commentService.addReaction(comment.getId(), "reactor", "LOVE");

        assertEquals("LOVE", existingReaction.getEmoji());
        verify(commentReactionRepository, times(1)).save(existingReaction);
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }

    // Khi tạo reaction mới cho comment của người khác, service phải lưu reaction và gửi notification.
    @Test
    public void testAddReaction_NewReaction_CreatesNotification() {
        User reactor = createUser("reactor");
        User commentAuthor = createUser("comment_author");
        Post post = createPost("reaction-post", commentAuthor);
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setPost(post);
        comment.setAuthor(commentAuthor);

        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(userRepository.findByUsername("reactor")).thenReturn(Optional.of(reactor));
        when(commentReactionRepository.findByCommentIdAndUserId(comment.getId(), reactor.getId()))
                .thenReturn(Optional.empty());

        commentService.addReaction(comment.getId(), "reactor", "LIKE");

        ArgumentCaptor<CommentReaction> captor = ArgumentCaptor.forClass(CommentReaction.class);
        verify(commentReactionRepository, times(1)).save(captor.capture());
        assertEquals("LIKE", captor.getValue().getEmoji());
        assertEquals(comment, captor.getValue().getComment());
        verify(notificationService, times(1)).createNotification(
                eq(commentAuthor),
                eq(reactor),
                eq(NotificationType.REACTION),
                org.mockito.ArgumentMatchers.contains("đã thả cảm xúc LIKE"),
                eq("/blog/reaction-post"));
    }

    // Xóa reaction chỉ thực hiện delete khi reaction tồn tại cho user hiện tại.
    @Test
    public void testRemoveReaction_ExistingReaction_DeletesSuccessfully() {
        User reactor = createUser("reactor");
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        CommentReaction reaction = CommentReaction.builder()
                .id(UUID.randomUUID())
                .comment(comment)
                .user(reactor)
                .emoji("LIKE")
                .build();

        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(userRepository.findByUsername("reactor")).thenReturn(Optional.of(reactor));
        when(commentReactionRepository.findByCommentIdAndUserId(comment.getId(), reactor.getId()))
                .thenReturn(Optional.of(reaction));

        commentService.removeReaction(comment.getId(), "reactor");

        verify(commentReactionRepository, times(1)).delete(reaction);
    }

    // Lấy comment theo bài viết phải map đúng dữ liệu phân trang của các comment gốc.
    @Test
    public void testGetCommentsByPost_ReturnsPagedTopLevelComments() {
        UUID postId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        CommentResponse response = new CommentResponse();
        response.setContent("Top level comment");

        when(postRepository.existsById(postId)).thenReturn(true);
        when(commentRepository.findByPostIdAndParentIsNull(postId, PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").ascending())))
                .thenReturn(new PageImpl<>(List.of(comment), PageRequest.of(0, 10), 1));
        when(commentMapper.toResponse(comment)).thenReturn(response);

        var result = commentService.getCommentsByPost(postId, 0, 10);

        assertEquals(1, result.getContent().size());
        assertEquals("Top level comment", result.getContent().get(0).getContent());
        assertTrue(result.isLast());
    }

    private User createUser(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        return user;
    }

    private Post createPost(String slug, User author) {
        Post post = new Post();
        post.setId(UUID.randomUUID());
        post.setSlug(slug);
        post.setAuthor(author);
        return post;
    }

    private CommentRequest createCommentRequest(String content, UUID parentId) {
        CommentRequest request = new CommentRequest();
        request.setContent(content);
        request.setParentId(parentId);
        return request;
    }
}
