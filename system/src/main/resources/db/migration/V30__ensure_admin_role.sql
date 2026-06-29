-- ============================================
-- 确保 admin 用户拥有 ADMIN 角色和 USER_ADMIN 权限
-- 解决角色管理页面无法访问的问题
-- ============================================

-- 1. 确保 USER_ADMIN 权限存在
INSERT IGNORE INTO sys_permission (code, name, description) VALUES
('USER_ADMIN', '用户管理', '允许维护用户和角色');

-- 2. 确保 ADMIN 角色拥有 USER_ADMIN 权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'ADMIN' AND p.code = 'USER_ADMIN';

-- 3. 为 admin 用户分配 ADMIN 角色
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.code = 'ADMIN';
