-- Thêm cột ảnh nền (cover image) cho bảng users
ALTER TABLE users ADD COLUMN IF NOT EXISTS cover_image VARCHAR(500);
