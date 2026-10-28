package com.blog.blogsystem.service.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Kiểm tra tính hợp lệ của file ảnh upload.
 *
 * Dùng chung cho mọi implementation của FileStorageService (local, Cloudinary, ...)
 * để đảm bảo quy tắc validate nhất quán bất kể nơi lưu trữ.
 */
@Component
public class ImageFileValidator {

    // Danh sách MIME type được phép upload
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    // Giới hạn kích thước file (5 MB)
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    private static final String DEFAULT_EXTENSION = ".jpg";

    /**
     * Ném IllegalArgumentException nếu file rỗng, sai định dạng hoặc vượt quá dung lượng.
     */
    public void validate(MultipartFile file) {
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
    }

    /**
     * Lấy phần mở rộng (kèm dấu chấm, chữ thường) từ tên file gốc, mặc định ".jpg".
     */
    public String resolveExtension(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(
                Optional.ofNullable(file.getOriginalFilename()).orElse("image"));

        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex >= 0 && lastDotIndex < originalFilename.length() - 1) {
            return "." + originalFilename.substring(lastDotIndex + 1).toLowerCase();
        }
        return DEFAULT_EXTENSION;
    }
}
