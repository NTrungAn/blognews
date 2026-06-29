-- Kích hoạt extension để tạo UUID tự động trong PostgreSQL
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Bảng roles
CREATE TABLE roles (
                       id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                       role_name VARCHAR(50) UNIQUE NOT NULL,
                       description TEXT
);

-- 2. Bảng users
CREATE TABLE users (
                       id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(100),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Bảng trung gian user_roles
CREATE TABLE user_roles (
                            user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                            role_id UUID REFERENCES roles(id) ON DELETE CASCADE,
                            PRIMARY KEY (user_id, role_id)
);