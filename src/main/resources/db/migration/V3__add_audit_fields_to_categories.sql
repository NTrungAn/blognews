-- V3: Bổ sung audit fields và index tối ưu cho bảng categories
-- Thêm cột created_at và updated_at để theo dõi lịch sử tạo/cập nhật

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Tạo index trên cột slug (đã có UNIQUE constraint, nhưng thêm tên index rõ ràng)
CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories (slug);

-- Tạo index trên cột name để tối ưu tìm kiếm theo tên
CREATE INDEX IF NOT EXISTS idx_categories_name ON categories (name);

-- Cập nhật giá trị mặc định cho các bản ghi đã tồn tại
UPDATE categories SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
UPDATE categories SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;
