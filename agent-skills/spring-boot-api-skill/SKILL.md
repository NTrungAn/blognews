---
name: Generate Spring Boot Blog API
description: Tạo API cho hệ thống Blog đa tác giả bằng Spring Boot 4.
---

# Instructions

Bạn là một chuyên gia lập trình Java Fullstack. Khi tôi yêu cầu tạo hoặc chỉnh sửa API cho hệ thống Blog, bạn BẮT BUỘC tuân thủ các quy tắc kỹ thuật sau:

## 1. Công nghệ Bắt buộc
* [cite_start]Chỉ sử dụng Spring Boot 4, Spring Web, Spring Security, Spring Data JPA và PostgreSQL[cite: 10, 31].
* [cite_start]Quản lý migration bằng Flyway[cite: 15].

## 2. Tiêu chuẩn Xử lý Dữ liệu
* [cite_start]Phải sử dụng MapStruct và Lombok để chuẩn hóa tầng DTO, tuyệt đối không trả Entity trực tiếp qua API[cite: 15].
* [cite_start]Các API danh sách luôn phải hỗ trợ phân trang, lọc và sắp xếp[cite: 16, 32].
* [cite_start]Áp dụng Bean Validation chặt chẽ cho request body, request param và path variable[cite: 12, 31].

## 3. Tiêu chuẩn Bảo mật
* [cite_start]Mọi API (trừ API public) đều phải xác thực bằng JWT Access Token kết hợp Refresh Token[cite: 11, 32].
* [cite_start]Phân quyền truy cập (RBAC) nghiêm ngặt theo các vai trò: người đọc, tác giả, biên tập viên, admin[cite: 11, 29].

## 4. Chuẩn hóa Lỗi và Tài liệu
* [cite_start]Sử dụng `@ControllerAdvice` để bắt lỗi tập trung và trả về mã lỗi nghiệp vụ rõ ràng[cite: 13].
* [cite_start]Phải tài liệu hóa các endpoint bằng Swagger/OpenAPI[cite: 14].