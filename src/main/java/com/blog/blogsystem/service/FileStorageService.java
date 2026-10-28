package com.blog.blogsystem.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Lưu file ảnh vào storage đang được cấu hình (app.storage.type = local |
     * cloudinary).
     *
     * @param file File ảnh từ multipart request
     * @return URL công khai để truy cập ảnh:
     *         - local: /uploads/images/uuid.jpg (tương đối)
     *         - cloudinary: https://res.cloudinary.com/... (tuyệt đối)
     */
    String storeImage(MultipartFile file);

    /**
     * Xóa file ảnh theo đường dẫn URL đã được lưu.
     * URL không thuộc storage hiện tại sẽ được bỏ qua.
     *
     * @param fileUrl URL của ảnh cần xóa
     */
    void deleteImage(String fileUrl);
}
