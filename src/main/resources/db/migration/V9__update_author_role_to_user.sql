-- 1. Chuyển tất cả người dùng có quyền AUTHOR sang USER
UPDATE user_roles 
SET role_id = (SELECT id FROM roles WHERE role_name = 'USER' LIMIT 1)
WHERE role_id = (SELECT id FROM roles WHERE role_name = 'AUTHOR' LIMIT 1);

-- 2. Xóa quyền AUTHOR thừa
DELETE FROM roles WHERE role_name = 'AUTHOR';
