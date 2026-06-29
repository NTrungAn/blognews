package com.blog.blogsystem.controller;

import com.blog.blogsystem.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Controller xử lý upload file.
 *
 * Cung cấp endpoint riêng biệt để upload ảnh,
 * tái sử dụng được ở nhiều tính năng: ảnh bìa bài viết, avatar, ...
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    /**
     * Upload một ảnh lên server.
     *
     * Method: POST
     * Content-Type: multipart/form-data
     * Field name: "file"
     *
     * @param file File ảnh cần upload (JPEG / PNG / WebP / GIF, tối đa 5 MB)
     * @return JSON chứa URL công khai của ảnh vừa upload
     */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file
    ) {
        String imageUrl = fileStorageService.storeImage(file);

        // Trả về dạng {"url": "/uploads/images/uuid.jpg"}
        return ResponseEntity.ok(Map.of("url", imageUrl));
    }
}
