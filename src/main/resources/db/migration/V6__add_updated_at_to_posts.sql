-- V6: Bổ sung cột updated_at vào bảng posts
-- Cột này được @UpdateTimestamp trong Post entity tự động cập nhật mỗi khi bản ghi thay đổi

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Khởi tạo giá trị cho các bản ghi đã tồn tại (lấy theo created_at)
UPDATE posts SET updated_at = created_at WHERE updated_at IS NULL;
