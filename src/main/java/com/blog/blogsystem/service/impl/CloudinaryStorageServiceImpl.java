package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.config.CloudinaryProperties;
import com.blog.blogsystem.service.FileStorageService;
import com.blog.blogsystem.service.support.ImageFileValidator;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Lưu ảnh lên Cloudinary (CDN).
 *
 * Kích hoạt khi app.storage.type=cloudinary.
 * URL trả về là URL tuyệt đối HTTPS, ví dụ:
 * https://res.cloudinary.com/<cloud>/image/upload/v1712345678/blognews/images/<uuid>.jpg
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "cloudinary")
public class CloudinaryStorageServiceImpl implements FileStorageService {

    private static final String DEFAULT_FOLDER = "blognews/images";
    private static final String CLOUDINARY_HOST = "res.cloudinary.com";
    private static final String UPLOAD_SEGMENT = "/upload/";

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;
    private final ImageFileValidator imageFileValidator;

    @Override
    public String storeImage(MultipartFile file) {
        // 1. Validate định dạng & dung lượng (dùng chung với chế độ local)
        imageFileValidator.validate(file);

        // 2. Upload lên Cloudinary với public_id là UUID để tránh trùng lặp
        Map<?, ?> uploadResult;
        try {
            uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", resolveFolder(),
                    "public_id", UUID.randomUUID().toString(),
                    "resource_type", "image",
                    "overwrite", false
            ));
        } catch (IOException e) {
            throw new IllegalStateException("Không thể upload ảnh lên Cloudinary: " + e.getMessage(), e);
        }

        // 3. Trả về URL HTTPS công khai
        return Optional.ofNullable(uploadResult.get("secure_url"))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalStateException("Cloudinary không trả về URL của ảnh."));
    }

    @Override
    public void deleteImage(String fileUrl) {
        // Bỏ qua URL không thuộc Cloudinary (ví dụ ảnh local cũ dạng /uploads/...)
        Optional<String> publicId = extractPublicId(fileUrl);
        if (publicId.isEmpty()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId.get(), ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException e) {
            // Chỉ log cảnh báo, không ném exception để không ảnh hưởng nghiệp vụ chính
            log.warn("Không thể xóa ảnh trên Cloudinary (publicId={}): {}", publicId.get(), e.getMessage());
        }
    }

    // ─────────────────────── Private Helper ───────────────────────

    private String resolveFolder() {
        return Optional.ofNullable(cloudinaryProperties.folder())
                .filter(folder -> !folder.isBlank())
                .orElse(DEFAULT_FOLDER);
    }

    /**
     * Tách public_id từ URL Cloudinary.
     * Ví dụ: https://res.cloudinary.com/demo/image/upload/v1712345678/blognews/images/abc.jpg
     *        → blognews/images/abc
     */
    private Optional<String> extractPublicId(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains(CLOUDINARY_HOST)) {
            return Optional.empty();
        }

        int uploadIndex = fileUrl.indexOf(UPLOAD_SEGMENT);
        if (uploadIndex < 0) {
            return Optional.empty();
        }

        // Phần sau "/upload/", bỏ query string nếu có
        String path = fileUrl.substring(uploadIndex + UPLOAD_SEGMENT.length()).split("\\?")[0];

        // Bỏ version prefix "v123456/"
        path = path.replaceFirst("^v\\d+/", "");

        // Bỏ phần mở rộng file (.jpg, .png, ...)
        int lastSlashIndex = path.lastIndexOf('/');
        int lastDotIndex = path.lastIndexOf('.');
        if (lastDotIndex > lastSlashIndex) {
            path = path.substring(0, lastDotIndex);
        }

        return Optional.of(path).filter(id -> !id.isBlank());
    }
}
