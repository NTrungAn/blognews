package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    // Thư mục gốc lưu file (cấu hình trong application.properties)
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // Sub-folder dành riêng cho ảnh bài viết
    private static final String IMAGE_SUBDIR = "images";

    // Danh sách MIME type được phép upload
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    // Giới hạn kích thước file (5 MB)
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    private Path imageStoragePath;

    /**
     * Tự động tạo thư mục lưu trữ khi service khởi động.
     */
    @PostConstruct
    public void init() {
        imageStoragePath = Paths.get(uploadDir, IMAGE_SUBDIR).toAbsolutePath().normalize();
        try {
            Files.createDirectories(imageStoragePath);
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục lưu ảnh: " + imageStoragePath, e);
        }
    }

    @Override
    public String storeImage(MultipartFile file) {
        // 1. Kiểm tra file không rỗng
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống.");
        }

        // 2. Kiểm tra MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Định dạng file không hợp lệ. Chỉ chấp nhận: JPEG, PNG, WebP, GIF.");
        }

        // 3. Kiểm tra kích thước file
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Kích thước file vượt quá giới hạn 5 MB.");
        }

        // 4. Lấy phần mở rộng từ tên file gốc
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "image");
        String extension = getFileExtension(originalFilename);

        // 5. Tạo tên file duy nhất bằng UUID để tránh trùng lặp
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // 6. Lưu file vào đĩa
        Path targetPath = imageStoragePath.resolve(uniqueFilename);
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file ảnh: " + uniqueFilename, e);
        }

        // 7. Trả về URL công khai tương đối (sẽ được expose qua Static Resource Handler)
        return "/uploads/" + IMAGE_SUBDIR + "/" + uniqueFilename;
    }

    @Override
    public void deleteImage(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        // Chuyển URL tương đối về đường dẫn thực trên đĩa
        // Ví dụ: "/uploads/images/uuid.jpg" → <uploadDir>/images/uuid.jpg
        String relativePath = fileUrl.replaceFirst("^/uploads/", "");
        Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(relativePath);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log warning nhưng không ném exception để không ảnh hưởng nghiệp vụ chính
            System.err.println("Cảnh báo: Không thể xóa file ảnh: " + filePath + " - " + e.getMessage());
        }
    }

    // ─────────────────────── Private Helper ───────────────────────

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex >= 0 && lastDotIndex < filename.length() - 1) {
            return "." + filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return ".jpg"; // Default extension nếu không xác định được
    }
}
