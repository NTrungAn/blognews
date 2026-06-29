-- V4: Bổ sung trường trạng thái (is_active) và updated_at vào bảng users
-- is_active = true  → tài khoản đang hoạt động bình thường
-- is_active = false → tài khoản đã bị khóa / vô hiệu hóa

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_active  BOOLEAN   NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP          DEFAULT CURRENT_TIMESTAMP;

-- Khởi tạo giá trị updated_at cho các bản ghi đã tồn tại
UPDATE users SET updated_at = created_at WHERE updated_at IS NULL;

-- Index tối ưu truy vấn lọc theo trạng thái tài khoản
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users (is_active);
