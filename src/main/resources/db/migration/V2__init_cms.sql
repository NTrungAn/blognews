-- 1. Bảng Categories (Danh mục)
CREATE TABLE categories (
                            id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                            name VARCHAR(255) NOT NULL,
                            slug VARCHAR(255) UNIQUE NOT NULL,
                            description TEXT
);

-- 2. Bảng Tags (Thẻ từ khóa)
CREATE TABLE tags (
                      id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                      name VARCHAR(100) NOT NULL,
                      slug VARCHAR(100) UNIQUE NOT NULL
);

-- 3. Bảng Posts (Bài viết)
CREATE TABLE posts (
                       id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                       author_id UUID REFERENCES users(id) ON DELETE SET NULL, -- Ràng buộc khóa ngoại tới bảng users
                       category_id UUID REFERENCES categories(id) ON DELETE SET NULL, -- Ràng buộc khóa ngoại tới bảng categories
                       title VARCHAR(255) NOT NULL,
                       summary TEXT,
                       content_markdown TEXT NOT NULL,
                       cover_image VARCHAR(255),
                       status VARCHAR(20) DEFAULT 'DRAFT', -- Trạng thái mặc định là bản nháp
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       published_at TIMESTAMP
);

-- 4. Bảng trung gian Post_Tags (Quan hệ n-n giữa bài viết và thẻ)
CREATE TABLE post_tags (
                           post_id UUID REFERENCES posts(id) ON DELETE CASCADE,
                           tag_id UUID REFERENCES tags(id) ON DELETE CASCADE,
                           PRIMARY KEY (post_id, tag_id)
);