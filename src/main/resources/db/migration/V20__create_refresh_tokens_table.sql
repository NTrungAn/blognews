-- Bảng lưu refresh token (hash) để hỗ trợ revoke, rotation, và quản lý phiên đăng nhập.
CREATE TABLE refresh_tokens (
    id            UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash    VARCHAR(64)  NOT NULL UNIQUE,
    device_info   VARCHAR(255),
    expires_at    TIMESTAMP    NOT NULL,
    revoked       BOOLEAN      DEFAULT FALSE,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
