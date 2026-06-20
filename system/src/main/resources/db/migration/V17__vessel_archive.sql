SET FOREIGN_KEY_CHECKS = 0;
UPDATE ai_review_ticket SET final_species_id = NULL WHERE final_species_id IS NOT NULL;
DELETE FROM observation_species;
DELETE FROM species;
DELETE FROM taxon;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO sys_permission (code, name, description)
SELECT 'VESSEL_READ', '查看船舶', '允许查看船舶档案'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'VESSEL_READ');

INSERT INTO sys_permission (code, name, description)
SELECT 'VESSEL_WRITE', '维护船舶', '允许新增、编辑或归档船舶档案'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'VESSEL_WRITE');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p
WHERE r.code = 'ADMIN'
  AND p.code IN ('VESSEL_READ', 'VESSEL_WRITE')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p
WHERE r.code IN ('RESEARCHER', 'VIEWER')
  AND p.code = 'VESSEL_READ'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

CREATE TABLE IF NOT EXISTS vessel_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_vessel_type_code (code),
    KEY idx_vessel_type_parent (parent_id),
    CONSTRAINT fk_vessel_type_parent FOREIGN KEY (parent_id) REFERENCES vessel_type(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vessel_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vessel_name VARCHAR(128) NOT NULL,
    mmsi VARCHAR(32) NULL,
    imo VARCHAR(32) NULL,
    call_sign VARCHAR(32) NULL,
    vessel_type_id BIGINT NULL,
    flag_state VARCHAR(64) NULL,
    operator_name VARCHAR(128) NULL,
    owner_name VARCHAR(128) NULL,
    length_m DECIMAL(10, 2) NULL,
    width_m DECIMAL(10, 2) NULL,
    draft_m DECIMAL(10, 2) NULL,
    gross_tonnage DECIMAL(14, 2) NULL,
    deadweight_tonnage DECIMAL(14, 2) NULL,
    risk_level VARCHAR(32) NULL,
    navigation_status VARCHAR(32) NULL,
    home_port VARCHAR(128) NULL,
    usual_region VARCHAR(255) NULL,
    route_area TEXT NULL,
    note TEXT NULL,
    source_text TEXT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_vessel_mmsi (mmsi),
    UNIQUE KEY uk_vessel_imo (imo),
    KEY idx_vessel_type (vessel_type_id),
    KEY idx_vessel_risk (risk_level),
    KEY idx_vessel_navigation_status (navigation_status),
    KEY idx_vessel_status (status),
    CONSTRAINT fk_vessel_type FOREIGN KEY (vessel_type_id) REFERENCES vessel_type(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO vessel_type (parent_id, code, name, description)
SELECT NULL, 'CARGO', '货船', '从事普通货物运输的船舶'
WHERE NOT EXISTS (SELECT 1 FROM vessel_type WHERE code = 'CARGO');

INSERT INTO vessel_type (parent_id, code, name, description)
SELECT NULL, 'TANKER', '油轮/液货船', '运输原油、成品油或液体化工品的船舶'
WHERE NOT EXISTS (SELECT 1 FROM vessel_type WHERE code = 'TANKER');

INSERT INTO vessel_type (parent_id, code, name, description)
SELECT NULL, 'CONTAINER', '集装箱船', '从事集装箱班轮运输的船舶'
WHERE NOT EXISTS (SELECT 1 FROM vessel_type WHERE code = 'CONTAINER');

INSERT INTO vessel_type (parent_id, code, name, description)
SELECT NULL, 'PASSENGER', '客船', '从事客运、邮轮或客滚运输的船舶'
WHERE NOT EXISTS (SELECT 1 FROM vessel_type WHERE code = 'PASSENGER');

INSERT INTO vessel_type (parent_id, code, name, description)
SELECT NULL, 'TUG', '拖轮/港作船', '从事拖带、协助靠离泊或港内作业的船舶'
WHERE NOT EXISTS (SELECT 1 FROM vessel_type WHERE code = 'TUG');

INSERT INTO vessel_type (parent_id, code, name, description)
SELECT p.id, 'CONTAINER_MAINLINE', '远洋集装箱船', '远洋干线集装箱船'
FROM vessel_type p
WHERE p.code = 'CONTAINER'
  AND NOT EXISTS (SELECT 1 FROM vessel_type WHERE code = 'CONTAINER_MAINLINE');

INSERT INTO vessel_type (parent_id, code, name, description)
SELECT p.id, 'PRODUCT_TANKER', '成品油轮', '运输成品油的液货船'
FROM vessel_type p
WHERE p.code = 'TANKER'
  AND NOT EXISTS (SELECT 1 FROM vessel_type WHERE code = 'PRODUCT_TANKER');

INSERT INTO vessel_type (parent_id, code, name, description)
SELECT p.id, 'HARBOUR_TUG', '港作拖轮', '港区拖带与协助作业船舶'
FROM vessel_type p
WHERE p.code = 'TUG'
  AND NOT EXISTS (SELECT 1 FROM vessel_type WHERE code = 'HARBOUR_TUG');

INSERT INTO vessel_profile (
  vessel_name, mmsi, imo, call_sign, vessel_type_id, flag_state, operator_name, owner_name,
  length_m, width_m, draft_m, gross_tonnage, deadweight_tonnage, risk_level,
  navigation_status, home_port, usual_region, route_area, note, source_text, status
)
SELECT 'APL HORIZON', '367145000', '9236597', 'WDB9956', vt.id, '美国', 'APL', 'APL Maritime',
       294.10, 32.20, 12.20, 65590, 67500, '重点关注',
       '在航', '洛杉矶港', '北美西岸 / 跨太平洋航线', '洛杉矶港、长滩港至北太平洋主干线',
       '样例船舶档案，用于替换旧海洋生物样例。', '系统初始化样例', 1
FROM vessel_type vt
WHERE vt.code = 'CONTAINER_MAINLINE'
  AND NOT EXISTS (SELECT 1 FROM vessel_profile WHERE mmsi = '367145000');

INSERT INTO vessel_profile (
  vessel_name, mmsi, imo, call_sign, vessel_type_id, flag_state, operator_name, owner_name,
  length_m, width_m, draft_m, gross_tonnage, deadweight_tonnage, risk_level,
  navigation_status, home_port, usual_region, route_area, note, source_text, status
)
SELECT 'EVER SIGNAL', '563841000', '9300403', '9V7589', vt.id, '新加坡', 'Evergreen Marine', 'Evergreen Marine',
       300.00, 42.80, 13.80, 75484, 78900, '普通关注',
       '锚泊', '长滩港', '北美西岸 / 亚洲航线', '长滩外锚地、San Pedro 外锚地、跨太平洋航线',
       '样例船舶档案，用于船舶主档查询和维护。', '系统初始化样例', 1
FROM vessel_type vt
WHERE vt.code = 'CONTAINER_MAINLINE'
  AND NOT EXISTS (SELECT 1 FROM vessel_profile WHERE mmsi = '563841000');

INSERT INTO vessel_profile (
  vessel_name, mmsi, imo, call_sign, vessel_type_id, flag_state, operator_name, owner_name,
  length_m, width_m, draft_m, gross_tonnage, deadweight_tonnage, risk_level,
  navigation_status, home_port, usual_region, route_area, note, source_text, status
)
SELECT 'PACIFIC TUG 07', '366998710', NULL, 'WDF4321', vt.id, '美国', 'Pacific Harbor Services', 'Pacific Harbor Services',
       28.60, 9.40, 4.10, 486, 210, '低风险',
       '港内作业', '洛杉矶港', '洛杉矶港内 / 长滩港内', '防波堤入口、港内接驳环线、码头协助靠离泊',
       '港作拖轮样例档案。', '系统初始化样例', 1
FROM vessel_type vt
WHERE vt.code = 'HARBOUR_TUG'
  AND NOT EXISTS (SELECT 1 FROM vessel_profile WHERE mmsi = '366998710');
