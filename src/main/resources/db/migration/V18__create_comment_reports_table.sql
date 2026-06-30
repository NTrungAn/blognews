CREATE TABLE comment_reports (
    id UUID PRIMARY KEY,
    comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    reporter_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reason VARCHAR(50) NOT NULL,
    detail VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);
