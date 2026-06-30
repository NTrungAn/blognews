package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.request.CommentRequest;
import com.blog.blogsystem.dto.request.CommentReportRequest;
import com.blog.blogsystem.dto.response.CommentResponse;
import com.blog.blogsystem.dto.response.CommentReportResponse;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.entity.Comment;
import com.blog.blogsystem.entity.CommentReaction;
import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.mapper.CommentMapper;
import com.blog.blogsystem.repository.CommentReactionRepository;
import com.blog.blogsystem.repository.CommentRepository;
import com.blog.blogsystem.repository.CommentReportRepository;
import com.blog.blogsystem.repository.PostRepository;
import com.blog.blogsystem.repository.UserRepository;
import com.blog.blogsystem.service.CommentService;
import com.blog.blogsystem.service.NotificationService;
import com.blog.blogsystem.entity.enums.NotificationType;
import com.blog.blogsystem.entity.CommentReport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final CommentReportRepository commentReportRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public CommentResponse createComment(UUID postId, CommentRequest request, String authorUsername) {
        if ((request.getContent() == null || request.getContent().trim().isEmpty())
                && (request.getImageUrl() == null || request.getImageUrl().trim().isEmpty())) {
            throw new IllegalArgumentException("Nội dung bình luận hoặc ảnh đính kèm không được để trống");
        }

        // 1. Tìm bài viết
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết với id: " + postId));

        // 2. Tìm tác giả bình luận từ JWT – KHÔNG lấy từ request body
        User author = userRepository.findByUsername(authorUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + authorUsername));

        // 3. Ánh xạ DTO → Entity (chỉ lấy content)
        Comment comment = commentMapper.toEntity(request);
        comment.setPost(post);
        comment.setAuthor(author);

        // 4. Xử lý tính năng reply: nếu có parentId → đây là bình luận con
        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy bình luận cha với id: " + request.getParentId()));

            // Đảm bảo bình luận cha cùng bài viết
            if (!parent.getPost().getId().equals(postId)) {
                throw new IllegalArgumentException("Bình luận cha không thuộc bài viết này.");
            }
            comment.setParent(parent);
        }

        // 7. Lưu bình luận xuống database
        Comment savedComment = commentRepository.save(comment);

        // 8. Tạo thông báo (Notification)
        if (request.getParentId() != null) {
            // Thông báo cho người được reply
            Comment parent = savedComment.getParent();
            String content = author.getUsername() + " đã trả lời bình luận của bạn: \""
                    + truncate(request.getContent(), 50) + "\"";
            String targetUrl = "/blog/" + post.getSlug(); // Có thể hash #comment-id nếu hỗ trợ
            notificationService.createNotification(parent.getAuthor(), author, NotificationType.COMMENT, content,
                    targetUrl);
        } else {
            // Thông báo cho tác giả bài viết
            String content = author.getUsername() + " đã bình luận về bài viết của bạn: \""
                    + truncate(request.getContent(), 50) + "\"";
            String targetUrl = "/blog/" + post.getSlug();
            notificationService.createNotification(post.getAuthor(), author, NotificationType.COMMENT, content,
                    targetUrl);
        }

        return commentMapper.toResponse(savedComment);
    }

    private String truncate(String text, int length) {
        if (text == null)
            return "";
        return text.length() > length ? text.substring(0, length) + "..." : text;
    }

    @Override
    @Transactional
    public CommentResponse updateComment(UUID commentId, CommentRequest request, String currentUsername) {
        Comment comment = findCommentAndCheckOwnership(commentId, currentUsername);
        comment.setContent(request.getContent());
        return commentMapper.toResponse(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId, String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + currentUsername));
        
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getRoleName() == com.blog.blogsystem.entity.enums.RoleType.ADMIN);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận với id: " + commentId));

        if (!isAdmin && !comment.getAuthor().getUsername().equals(currentUsername)) {
            throw new RuntimeException("Bạn không có quyền thao tác với bình luận này.");
        }
        
        commentRepository.delete(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getCommentsByPost(UUID postId, int pageNo, int pageSize) {
        // Xác nhận bài viết tồn tại
        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Không tìm thấy bài viết với id: " + postId);
        }

        // Chỉ lấy bình luận gốc (top-level), replies đã được MapStruct ánh xạ qua quan
        // hệ OneToMany
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").ascending());
        Page<Comment> page = commentRepository.findByPostIdAndParentIsNull(postId, pageable);

        List<CommentResponse> content = page.getContent().stream()
                .map(commentMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<CommentResponse>builder()
                .content(content)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional
    public void addReaction(UUID commentId, String currentUsername, String emoji) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận với id: " + commentId));
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + currentUsername));

        java.util.Optional<CommentReaction> existingReaction = commentReactionRepository
                .findByCommentIdAndUserId(commentId, user.getId());

        if (existingReaction.isPresent()) {
            CommentReaction reaction = existingReaction.get();
            reaction.setEmoji(emoji);
            commentReactionRepository.save(reaction);
        } else {
            CommentReaction reaction = CommentReaction.builder()
                    .comment(comment)
                    .user(user)
                    .emoji(emoji)
                    .build();
            commentReactionRepository.save(reaction);

            // Gửi thông báo cho chủ nhân bình luận
            if (!comment.getAuthor().getId().equals(user.getId())) {
                String content = user.getUsername() + " đã thả cảm xúc " + emoji + " vào bình luận của bạn.";
                String targetUrl = "/blog/" + comment.getPost().getSlug();
                notificationService.createNotification(comment.getAuthor(), user, NotificationType.REACTION, content,
                        targetUrl);
            }
        }
    }

    @Override
    @Transactional
    public void removeReaction(UUID commentId, String currentUsername) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận với id: " + commentId));
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + currentUsername));

        commentReactionRepository.findByCommentIdAndUserId(commentId, user.getId())
                .ifPresent(commentReactionRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getAllCommentsForAdmin(String keyword, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
        Page<Comment> commentPage = commentRepository.findAllComments(keyword, pageable);

        List<CommentResponse> content = commentPage.getContent().stream()
                .map(commentMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<CommentResponse>builder()
                .content(content)
                .pageNo(commentPage.getNumber())
                .pageSize(commentPage.getSize())
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .last(commentPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public void reportComment(UUID commentId, CommentReportRequest request, String reporterUsername) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận với id: " + commentId));
        
        User reporter = userRepository.findByUsername(reporterUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng báo cáo: " + reporterUsername));

        // Lưu thông tin báo cáo chi tiết
        CommentReport commentReport = CommentReport.builder()
                .comment(comment)
                .reporter(reporter)
                .reason(request.getReason())
                .detail(request.getDetail())
                .build();
        commentReportRepository.save(commentReport);

        // Tăng tổng số lượt báo cáo trong bảng comments
        comment.setReportCount(comment.getReportCount() + 1);
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void dismissCommentReport(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận với id: " + commentId));
        
        // Reset report count
        comment.setReportCount(0);
        commentRepository.save(comment);

        // Xóa tất cả các báo cáo chi tiết liên quan
        commentReportRepository.deleteByCommentId(commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getReportedComments(String keyword, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("reportCount").descending().and(Sort.by("createdAt").descending()));
        Page<Comment> commentPage = commentRepository.findReportedComments(keyword, pageable);

        List<CommentResponse> content = commentPage.getContent().stream()
                .map(c -> {
                    CommentResponse res = commentMapper.toResponse(c);
                    
                    // Lấy danh sách báo cáo chi tiết cho bình luận này
                    List<CommentReport> reports = commentReportRepository.findByCommentIdOrderByCreatedAtDesc(c.getId());
                    List<CommentReportResponse> reportResponses = reports.stream()
                            .map(r -> CommentReportResponse.builder()
                                    .id(r.getId())
                                    .reason(r.getReason())
                                    .detail(r.getDetail())
                                    .reporterUsername(r.getReporter() != null ? r.getReporter().getUsername() : "Ẩn danh")
                                    .createdAt(r.getCreatedAt())
                                    .build())
                            .collect(Collectors.toList());
                    
                    res.setReports(reportResponses);
                    return res;
                })
                .collect(Collectors.toList());

        return PageResponse.<CommentResponse>builder()
                .content(content)
                .pageNo(commentPage.getNumber())
                .pageSize(commentPage.getSize())
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .last(commentPage.isLast())
                .build();
    }

    // ───────────────────── Private helper ─────────────────────

    /**
     * Tìm bình luận theo ID và kiểm tra quyền sở hữu.
     * Ném RuntimeException nếu không tìm thấy hoặc người dùng không phải chủ bình
     * luận.
     */
    private Comment findCommentAndCheckOwnership(UUID commentId, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận với id: " + commentId));

        if (!comment.getAuthor().getUsername().equals(username)) {
            throw new RuntimeException("Bạn không có quyền thao tác với bình luận này.");
        }
        return comment;
    }
}
