package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.request.PostRequest;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.dto.response.PostResponse;
import com.blog.blogsystem.entity.Category;
import com.blog.blogsystem.entity.Post;
import com.blog.blogsystem.entity.enums.PostStatus;
import com.blog.blogsystem.entity.Tag;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.mapper.PostMapper;
import com.blog.blogsystem.repository.CategoryRepository;
import com.blog.blogsystem.repository.PostRepository;
import com.blog.blogsystem.repository.TagRepository;
import com.blog.blogsystem.repository.UserRepository;
import com.blog.blogsystem.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;

    @org.springframework.beans.factory.annotation.Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    // ─────────────────────── CREATE ───────────────────────

    @Override
    @Transactional
    public PostResponse createPost(PostRequest request, String username) {
        // 1. Tìm Author từ JWT
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tác giả hợp lệ."));

        // 2. Tìm Category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại."));

        // 3. Tìm các Tags
        Set<Tag> tags = new HashSet<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            List<Tag> foundTags = tagRepository.findAllById(request.getTagIds());
            tags.addAll(foundTags);
        }

        // 4. Map DTO → Entity
        Post post = postMapper.toEntity(request);

        // 5. Tạo slug duy nhất từ tiêu đề
        post.setSlug(generateUniqueSlug(request.getTitle()));

        // 6. Gán quan hệ
        post.setAuthor(author);
        post.setCategory(category);
        post.setTags(tags);

        // 7. Lưu và trả về
        Post savedPost = postRepository.save(post);
        return postMapper.toResponse(savedPost);
    }

    // ─────────────────────── READ ───────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getAllPosts(String categorySlug, String tagSlug, String statusStr, String keyword,
            int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        // Chuyển chuỗi status thành Enum (nếu có)
        PostStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = PostStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Bỏ qua hoặc xử lý lỗi nếu status không hợp lệ
            }
        }

        // Fix lỗi PSQLException: function lower(bytea) does not exist
        // Khi truyền tham số null, Postgres tự gán là bytea. Truyền chuỗi rỗng "" để
        // tránh lỗi ép kiểu.
        String searchKeyword = (keyword == null || keyword.trim().isEmpty()) ? "" : keyword.trim();

        Page<Post> posts = postRepository.findPostsWithFilters(categorySlug, tagSlug, status, searchKeyword, pageable);

        List<PostResponse> content = posts.getContent().stream()
                .map(postMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<PostResponse>builder()
                .content(content)
                .pageNo(posts.getNumber())
                .pageSize(posts.getSize())
                .totalElements(posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .last(posts.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getMyPosts(String username, int pageNo, int pageSize, String sortBy,
            String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Post> posts = postRepository.findByAuthorUsername(username, pageable);

        List<PostResponse> content = posts.getContent().stream()
                .map(postMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<PostResponse>builder()
                .content(content)
                .pageNo(posts.getNumber())
                .pageSize(posts.getSize())
                .totalElements(posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .last(posts.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPostsByAuthor(String username, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());

        Page<Post> posts = postRepository.findByAuthorUsernameAndStatus(username, PostStatus.PUBLISHED, pageable);
        List<PostResponse> content = posts.getContent().stream()
                .map(postMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<PostResponse>builder()
                .content(content)
                .pageNo(posts.getNumber())
                .pageSize(posts.getSize())
                .totalElements(posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .last(posts.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(UUID id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết với id: " + id));
        return postMapper.toResponse(post);
    }

    @Override
    @Transactional
    public PostResponse getPostBySlug(String slug) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết với slug: " + slug));
        post.setViewCount(post.getViewCount() + 1);
        Post savedPost = postRepository.save(post);
        return postMapper.toResponse(savedPost);
    }

    // ─────────────────────── UPDATE ───────────────────────

    @Override
    @Transactional
    public PostResponse updatePost(UUID id, PostRequest request, String username) {
        // 1. Tìm bài viết
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy bài viết."));

        // 2. Kiểm tra quyền sở hữu
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new RuntimeException("Lỗi: Bạn không có quyền chỉnh sửa bài viết này.");
        }

        // 3. Cập nhật các trường cơ bản
        post.setTitle(request.getTitle());
        post.setSummary(request.getSummary());
        post.setContentMarkdown(request.getContentMarkdown());
        // coverImage chỉ cập nhật nếu request gửi giá trị mới (upload file → set trong
        // Controller)
        if (request.getCoverImage() != null) {
            post.setCoverImage(request.getCoverImage());
        }
        if (request.getStatus() != null) {
            post.setStatus(request.getStatus());
        }

        // 4. Cập nhật Danh mục
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại."));
        post.setCategory(category);

        // 5. Cập nhật Tags
        if (request.getTagIds() != null) {
            List<Tag> foundTags = tagRepository.findAllById(request.getTagIds());
            post.setTags(new HashSet<>(foundTags));
        }

        // 6. Lưu và trả về
        Post updatedPost = postRepository.save(post);
        return postMapper.toResponse(updatedPost);
    }

    // ─────────────────────── DELETE ───────────────────────

    @Override
    @Transactional
    public void deletePost(UUID id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + username));
        
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getRoleName() == com.blog.blogsystem.entity.enums.RoleType.ADMIN);

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy bài viết."));

        if (!isAdmin && !post.getAuthor().getUsername().equals(username)) {
            throw new RuntimeException("Lỗi: Bạn không có quyền xóa bài viết này.");
        }

        postRepository.delete(post);
    }

    // ─────────────────────── Private helper ───────────────────────

    /**
     * Tạo slug duy nhất từ title bài viết.
     * Ví dụ: "Hướng dẫn Spring Boot" → "huong-dan-spring-boot"
     * Nếu slug đã tồn tại → thêm suffix số: "huong-dan-spring-boot-1"
     */
    private String generateUniqueSlug(String title) {
        String baseSlug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");

        String slug = baseSlug;
        int suffix = 1;
        while (postRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix++;
        }
        return slug;
    }

    @Override
    @Transactional(readOnly = true)
    public String summarizePost(UUID id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết để tóm tắt"));

        String apiKey = geminiApiKey;

        if (apiKey == null || apiKey.isBlank()) {
            return generateMockSummary(post);
        }

        try {
            String articleText = "Title: " + post.getTitle() + "\nSummary: " + post.getSummary() + "\nContent:\n"
                    + post.getContentMarkdown();
            if (articleText.length() > 5000) {
                articleText = articleText.substring(0, 5000) + "...";
            }

            String prompt = "Hãy tóm tắt bài viết sau đây bằng tiếng Việt trong khoảng 3 câu ngắn gọn dưới dạng danh sách gạch đầu dòng (bullet points). Hãy tập trung vào những ý cốt lõi quan trọng nhất.\n\nBài viết:\n"
                    + articleText;

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String promptJson = mapper.writeValueAsString(prompt);
            String payload = "{\"contents\":[{\"parts\":[{\"text\":" + promptJson + "}]}]}";

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(
                            "https://generativelanguage.googleapis.com/v1/models/gemini-3.1-flash-lite:generateContent?key="
                                    + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload,
                            java.nio.charset.StandardCharsets.UTF_8))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(body);
                com.fasterxml.jackson.databind.JsonNode textNode = rootNode.path("candidates").path(0).path("content")
                        .path("parts").path(0).path("text");
                if (!textNode.isMissingNode()) {
                    return textNode.asText();
                }
            }
            return "💡 [Lưu ý: Lỗi kết nối Google API (Status: " + response.statusCode() + ", Phản hồi: " + response.body() + "), đang sử dụng chế độ dự phòng]\n\n"
                    + generateMockSummary(post);
        } catch (Exception e) {
            return "💡 [Lưu ý: Có lỗi xảy ra (" + e.getMessage() + "), đang sử dụng chế độ dự phòng thông minh]\n\n"
                    + generateMockSummary(post);
        }
    }

    private String generateMockSummary(Post post) {
        StringBuilder summary = new StringBuilder();
        summary.append("✨ Tóm tắt bài viết bởi Trợ lý AI (Chế độ Demo):\n\n");
        if (post.getSummary() != null && !post.getSummary().isBlank()) {
            summary.append("• ").append(post.getSummary()).append("\n");
        } else {
            summary.append("• Bài viết chia sẻ các thông tin hữu ích về chủ đề \"").append(post.getTitle())
                    .append("\".\n");
        }

        String content = post.getContentMarkdown() != null ? post.getContentMarkdown() : "";

        java.util.List<String> headings = new java.util.ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?m)^#{2,3}\\s+(.+)$").matcher(content);
        while (matcher.find() && headings.size() < 2) {
            headings.add(matcher.group(1).trim());
        }

        if (!headings.isEmpty()) {
            for (String heading : headings) {
                summary.append("• Bài viết tập trung phân tích khía cạnh: ").append(heading).append(".\n");
            }
        } else {
            summary.append(
                    "• Độc giả có thể tìm thấy hướng dẫn chi tiết và phân tích sâu về chủ đề này thông qua nội dung bài viết.\n");
            summary.append(
                    "• Thích hợp cho những ai muốn tìm hiểu, nghiên cứu và áp dụng kiến thức này vào thực tế công việc.\n");
        }

        summary.append("\n*⚙️ Cấu hình biến môi trường `GEMINI_API_KEY` để kích hoạt Trí tuệ Nhân tạo thực tế.*");
        return summary.toString();
    }

    @Override
    @Transactional
    public PostResponse updatePostStatusByAdmin(UUID id, PostStatus status, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + adminUsername));

        boolean isAdmin = admin.getRoles().stream()
                .anyMatch(r -> r.getRoleName() == com.blog.blogsystem.entity.enums.RoleType.ADMIN);

        if (!isAdmin) {
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này.");
        }

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy bài viết."));

        post.setStatus(status);
        Post savedPost = postRepository.save(post);
        return postMapper.toResponse(savedPost);
    }
}
