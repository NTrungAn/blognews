package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.service.FileStorageService;
import com.blog.blogsystem.service.support.ImageFileValidator;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Lưu ảnh vào thư mục cục bộ trên server.
 *
 * Kích hoạt khi app.storage.type=local (mặc định nếu không cấu hình).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class FileStorageServiceImpl implements FileStorageService {

    // Sub-folder dành riêng cho ảnh bài viết
    private static final String IMAGE_SUBDIR = "images";

    // Thư mục gốc lưu file (cấu hình trong application.properties)
    private final String uploadDir;
    private final ImageFileValidator imageFileValidator;

    private Path imageStoragePath;

    public FileStorageServiceImpl(
            @Value("${app.upload.dir:uploads}") String uploadDir,
            ImageFileValidator imageFileValidator
    ) {
        this.uploadDir = uploadDir;
        this.imageFileValidator = imageFileValidator;
    }

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
        // 1-3. Kiểm tra file rỗng, MIME type, kích thước
        imageFileValidator.validate(file);

        // 4. Lấy phần mở rộng từ tên file gốc
        String extension = imageFileValidator.resolveExtension(file);

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
        // Chỉ xử lý URL local dạng /uploads/... (bỏ qua URL tuyệt đối như Cloudinary)
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) return;

        // Chuyển URL tương đối về đường dẫn thực trên đĩa
        // Ví dụ: "/uploads/images/uuid.jpg" → <uploadDir>/images/uuid.jpg
        String relativePath = fileUrl.replaceFirst("^/uploads/", "");
        Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(relativePath);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log warning nhưng không ném exception để không ảnh hưởng nghiệp vụ chính
            log.warn("Không thể xóa file ảnh: {} - {}", filePath, e.getMessage());
        }
    }
}
