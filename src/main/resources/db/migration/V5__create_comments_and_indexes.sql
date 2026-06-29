-- V5: Tạo bảng comments với tính năng tự tham chiếu (self-referencing) để hỗ trợ reply lồng nhau
-- Đồng thời bổ sung index cho bảng tags và post_tags

-- 1. Bảng comments
CREATE TABLE comments (
    id         UUID      DEFAULT uuid_generate_v4() PRIMARY KEY,
    post_id    UUID      NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    author_id  UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- Self-referencing: NULL = bình luận gốc, UUID = bình luận reply
    parent_id  UUID               REFERENCES comments(id) ON DELETE CASCADE,
    content    TEXT      NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Index tối ưu truy vấn
CREATE INDEX IF NOT EXISTS idx_comments_post_id   ON comments (post_id);
CREATE INDEX IF NOT EXISTS idx_comments_author_id ON comments (author_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent_id ON comments (parent_id);

-- 3. Bổ sung index cho bảng tags (đã tồn tại từ V2)
CREATE INDEX IF NOT EXISTS idx_tags_slug ON tags (slug);

-- 4. Bổ sung cột slug vào bảng posts nếu chưa có (để truy cập bài viết qua URL thân thiện)
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS slug VARCHAR(255) UNIQUE;

CREATE INDEX IF NOT EXISTS idx_posts_slug   ON posts (slug);
CREATE INDEX IF NOT EXISTS idx_posts_status ON posts (status);
