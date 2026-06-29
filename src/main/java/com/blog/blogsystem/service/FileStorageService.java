package com.blog.blogsystem.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Lưu file ảnh vào thư mục storage nội bộ.
     *
     * @param file File ảnh từ multipart request
     * @return URL công khai để truy cập ảnh (ví dụ: /uploads/images/uuid.jpg)
     */
    String storeImage(MultipartFile file);

    /**
     * Xóa file ảnh theo đường dẫn URL đã được lưu.
     *
     * @param fileUrl URL của ảnh cần xóa (dạng /uploads/images/uuid.jpg)
     */
    void deleteImage(String fileUrl);
}
