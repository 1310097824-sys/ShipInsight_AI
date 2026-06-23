-- ============================================
-- Quiz Knowledge Base (知识问答库)
-- 船舶 / 天气 / 海域 相关题库
-- ============================================

CREATE TABLE IF NOT EXISTS quiz_question (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    category     VARCHAR(32)  NOT NULL COMMENT '分类: SHIP / WEATHER / SEA_AREA',
    type         VARCHAR(16)  NOT NULL COMMENT '题型: SINGLE / MULTI / JUDGE',
    title        VARCHAR(512) NOT NULL COMMENT '题目文本',
    options      JSON         NOT NULL COMMENT '选项 [{label,text}]，判断题固定为 [{"label":"A","text":"正确"},{"label":"B","text":"错误"}]',
    answer       VARCHAR(128) NOT NULL COMMENT '正确答案，单选选label如A，多选用逗号分隔如A,C，判断填A或B',
    explanation  TEXT         NULL     COMMENT '答案解析',
    difficulty   VARCHAR(8)   NOT NULL DEFAULT 'EASY' COMMENT '难度: EASY / MEDIUM / HARD',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_quiz_category (category),
    KEY idx_quiz_type     (type),
    KEY idx_quiz_difficulty (difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库';

CREATE TABLE IF NOT EXISTS quiz_record (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL COMMENT '答题人',
    score        INT          NOT NULL DEFAULT 0 COMMENT '得分',
    total        INT          NOT NULL DEFAULT 0 COMMENT '总题数',
    categories   VARCHAR(128) NULL     COMMENT '选择的分类，逗号分隔',
    mode         VARCHAR(16)  NOT NULL DEFAULT 'RANDOM' COMMENT '模式: RANDOM / SEQUENTIAL / CHALLENGE',
    started_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    finished_at  DATETIME(3)  NULL,
    KEY idx_quiz_record_user (user_id),
    KEY idx_quiz_record_time (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录';

CREATE TABLE IF NOT EXISTS quiz_answer (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id    BIGINT       NOT NULL COMMENT '关联答题记录',
    question_id  BIGINT       NOT NULL COMMENT '关联题目',
    user_answer  VARCHAR(128) NULL     COMMENT '用户答案',
    is_correct   TINYINT      NOT NULL DEFAULT 0 COMMENT '1=正确 0=错误',
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_quiz_answer_record (record_id),
    KEY idx_quiz_answer_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题详情';

-- 权限
INSERT IGNORE INTO sys_permission (code, name) VALUES
('QUIZ_READ', '知识问答-查看'),
('QUIZ_WRITE', '知识问答-管理');

-- admin 角色给予权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.code = 'ADMIN' AND p.code IN ('QUIZ_READ', 'QUIZ_WRITE');

-- observer 角色给予查看权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.code = 'OBSERVER' AND p.code = 'QUIZ_READ';
