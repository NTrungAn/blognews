CREATE TABLE settings (
    key VARCHAR(100) PRIMARY KEY,
    value TEXT,
    description VARCHAR(255)
);

INSERT INTO settings (key, value, description) VALUES
('website_name', 'NewsFlow Blog CMS', 'Tên hiển thị của website'),
('website_description', 'Một hệ thống quản trị nội dung mạnh mẽ.', 'Mô tả ngắn về website'),
('default_language', 'vi', 'Ngôn ngữ mặc định (vi/en)'),
('timezone', 'Asia/Ho_Chi_Minh', 'Múi giờ hoạt động'),
('posts_per_page', '10', 'Số bài viết trên mỗi trang hiển thị'),
('maintenance_mode', 'false', 'Chế độ bảo trì hệ thống'),
('meta_title', 'NewsFlow — Tin tức & Blog chuyên sâu', 'Meta Title mặc định'),
('meta_description', 'Khám phá các bài viết chuyên sâu về công nghệ, kinh doanh, văn hóa và nhiều chủ đề thú vị khác.', 'Meta Description mặc định'),
('google_analytics_id', '', 'Google Analytics ID (ví dụ: G-XXXXXXXXXX)'),
('google_search_console', '', 'Google Search Console meta tag'),
('canonical_url', 'https://newsflow.com', 'Canonical URL của website'),
('sitemap_enabled', 'true', 'Tự động tạo sitemap XML'),
('smtp_host', 'smtp.gmail.com', 'SMTP Host gửi mail'),
('smtp_port', '587', 'SMTP Port gửi mail'),
('smtp_username', 'noreply@newsflow.com', 'SMTP Username tài khoản gửi mail'),
('smtp_password', '', 'SMTP Password / App Password gửi mail'),
('smtp_from_name', 'NewsFlow System', 'Tên hiển thị người gửi email'),
('smtp_encryption', 'TLS', 'Mã hóa SMTP (TLS/SSL/NONE)'),
('default_role', 'READER', 'Quyền mặc định gán cho người dùng mới đăng ký');
