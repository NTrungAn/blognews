-- Thêm cột family_id để hỗ trợ Token Family Reuse Detection.
-- Khi phát hiện refresh token đã revoke bị sử dụng lại,
-- toàn bộ token cùng family sẽ bị thu hồi ngay lập tức.

ALTER TABLE refresh_tokens ADD COLUMN family_id UUID;

-- Backfill: gán family_id = id cho các token hiện có
UPDATE refresh_tokens SET family_id = id WHERE family_id IS NULL;

ALTER TABLE refresh_tokens ALTER COLUMN family_id SET NOT NULL;

CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
