-- ============================================
-- AIS 船舶交通态势平台 — 角色体系重构
-- 将生物多样性科研角色 → AIS 航运主题角色
-- ============================================

-- 1. 创建新角色
INSERT IGNORE INTO sys_role (code, name, description) VALUES
('CONTROLLER', '交通管制员', '监控与管理AIS船舶交通态势，维护船舶档案及知识库'),
('OPERATOR',   '船舶运营方', '管理自有船舶档案，查看航线态势与报告数据'),
('ANALYST',    '数据分析师', '分析船舶交通态势数据，生成报告与知识问答'),
('OBSERVER',   '公众观察员', '查看公开的船舶交通态势与基础数据');

-- 2. 迁移现有用户的角色分配 (旧代码 → 新代码)
UPDATE sys_user_role ur
JOIN sys_role old_r ON ur.role_id = old_r.id AND old_r.code = 'RESEARCHER'
JOIN sys_role new_r ON new_r.code = 'CONTROLLER'
SET ur.role_id = new_r.id;

UPDATE sys_user_role ur
JOIN sys_role old_r ON ur.role_id = old_r.id AND old_r.code = 'STUDENT'
JOIN sys_role new_r ON new_r.code = 'OPERATOR'
SET ur.role_id = new_r.id;

UPDATE sys_user_role ur
JOIN sys_role old_r ON ur.role_id = old_r.id AND old_r.code = 'VIEWER'
JOIN sys_role new_r ON new_r.code = 'ANALYST'
SET ur.role_id = new_r.id;

UPDATE sys_user_role ur
JOIN sys_role old_r ON ur.role_id = old_r.id AND old_r.code = 'PUBLIC'
JOIN sys_role new_r ON new_r.code = 'OBSERVER'
SET ur.role_id = new_r.id;

-- 3. 清理旧角色-权限映射
DELETE rp FROM sys_role_permission rp
JOIN sys_role r ON rp.role_id = r.id
WHERE r.code IN ('RESEARCHER', 'STUDENT', 'VIEWER', 'PUBLIC');

-- 4. 删除旧角色
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM sys_role WHERE code IN ('RESEARCHER', 'STUDENT', 'VIEWER', 'PUBLIC');
SET FOREIGN_KEY_CHECKS = 1;

-- 5. 创建新角色-权限映射

-- CONTROLLER 交通管制员: 船舶读写、报表读、知识库读写、问答读写、附件读写、AI复核读写、审计读
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'CONTROLLER' AND p.code IN (
  'VESSEL_READ', 'VESSEL_WRITE',
  'REPORT_READ',
  'RAG_READ', 'RAG_MANAGE',
  'QUIZ_READ', 'QUIZ_WRITE',
  'MEDIA_READ', 'MEDIA_WRITE',
  'AI_REVIEW_READ', 'AI_REVIEW_WRITE',
  'AUDIT_READ'
);

-- OPERATOR 船舶运营方: 船舶读写、报表读、知识库读、问答查看、附件读写
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'OPERATOR' AND p.code IN (
  'VESSEL_READ', 'VESSEL_WRITE',
  'REPORT_READ',
  'RAG_READ',
  'QUIZ_READ',
  'MEDIA_READ', 'MEDIA_WRITE'
);

-- ANALYST 数据分析师: 船舶读、报表读、知识库读、问答读写、附件读写、AI复核读写
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'ANALYST' AND p.code IN (
  'VESSEL_READ',
  'REPORT_READ',
  'RAG_READ',
  'QUIZ_READ', 'QUIZ_WRITE',
  'MEDIA_READ', 'MEDIA_WRITE',
  'AI_REVIEW_READ', 'AI_REVIEW_WRITE'
);

-- OBSERVER 公众观察员: 船舶读、报表读、问答查看、附件读
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'OBSERVER' AND p.code IN (
  'VESSEL_READ',
  'REPORT_READ',
  'QUIZ_READ',
  'MEDIA_READ'
);
