-- V29: Seed AIS vessel-traffic sample data
-- Prerequisites: ais_record_manual_vessel FK has been updated to reference vessel_profile

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. Seed shipping_zone data (AIS 航运区域)
-- ============================================================
INSERT INTO shipping_zone (id, name, type, description, created_at) VALUES
(1,  '湛江港水域',     '港口水域', '湛江港主港区及航道水域，涵盖霞山、宝满、调顺等港区，年吞吐量大，船舶交通密集',              NOW(3)),
(2,  '湛江港外锚地',   '锚地',     '湛江港外锚地区域，供船舶等泊、避风及检疫锚泊，水深条件良好',                                NOW(3)),
(3,  '琼州海峡',       '海峡通道', '琼州海峡主航道，连接北部湾与南海的重要水上通道，日均过境船舶超百艘次',                        NOW(3)),
(4,  '北部湾航道',     '航道',     '北部湾国际航运通道，连接湛江、北海、防城港等港口，是西南地区出海大通道',                      NOW(3)),
(5,  '东海岛水域',     '近岸水域', '东海岛附近水域，邻近宝钢湛江钢铁基地和中科炼化项目，工业船舶活动频繁',                        NOW(3)),
(6,  '雷州半岛西岸',   '沿岸水域', '雷州半岛西部沿岸水域，渔商混航区域，通航环境较为复杂',                                        NOW(3)),
(7,  '北海港水域',     '港口水域', '北部湾北海港水域，含铁山港区及石步岭港区，广西重要枢纽港',                                    NOW(3)),
(8,  '涠洲岛近海',     '近海区域', '涠洲岛附近水域，兼具旅游客运和渔业活动，小型船舶较多',                                        NOW(3));

-- ============================================================
-- 2. Seed ais_record_manual data (AIS 手动记录)
-- ============================================================
-- Observer: admin (user_id=1)
-- Locations are in Zhanjiang/Beibu Gulf area (POINT(lat, lon) for MySQL SRID 4326)
INSERT INTO ais_record_manual (id, ecosystem_id, observer_user_id, observed_at, location_lat, location_lng, location_point, location_name, note, created_at) VALUES
-- 湛江港水域 (zone 1)
(1,  1, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR),  21.1850, 110.4200, ST_GeomFromText('POINT(21.1850 110.4200)', 4326), '湛江港主航道',       '集装箱船通过主航道进港，航速12节',                 NOW(3)),
(2,  1, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR),  21.1780, 110.4150, ST_GeomFromText('POINT(21.1780 110.4150)', 4326), '霞山港区泊位',       '货船靠泊作业中，装卸钢材货物',                     NOW(3)),
(3,  1, 1, DATE_SUB(NOW(), INTERVAL 5 HOUR),  21.1900, 110.4250, ST_GeomFromText('POINT(21.1900 110.4250)', 4326), '宝满港区航道口',     '油轮出港，拖轮协助离泊',                           NOW(3)),

-- 湛江港外锚地 (zone 2)
(4,  2, 1, DATE_SUB(NOW(), INTERVAL 2 HOUR),  21.1050, 110.5500, ST_GeomFromText('POINT(21.1050 110.5500)', 4326), '湛江港外锚地1号位',  '大型集装箱船锚泊等泊，预计靠泊时间明天上午',       NOW(3)),
(5,  2, 1, DATE_SUB(NOW(), INTERVAL 8 HOUR),  21.0980, 110.5450, ST_GeomFromText('POINT(21.0980 110.5450)', 4326), '湛江港外锚地2号位',  '散货船锚泊候潮，吃水较深需乘潮进港',               NOW(3)),

-- 琼州海峡 (zone 3)
(6,  3, 1, DATE_SUB(NOW(), INTERVAL 1 DAY),   20.1800, 110.1500, ST_GeomFromText('POINT(20.1800 110.1500)', 4326), '琼州海峡西口',       '客滚船由海口驶往徐闻，能见度良好',                 NOW(3)),
(7,  3, 1, DATE_SUB(NOW(), INTERVAL 1 DAY),   20.2200, 110.2800, ST_GeomFromText('POINT(20.2200 110.2800)', 4326), '琼州海峡中段',       '南北向穿越海峡的货船，AIS信号稳定',                NOW(3)),

-- 北部湾航道 (zone 4)
(8,  4, 1, DATE_SUB(NOW(), INTERVAL 2 DAY),   20.8500, 109.5000, ST_GeomFromText('POINT(20.8500 109.5000)', 4326), '北部湾主航道',       '集装箱船沿推荐航线南行，驶往新加坡方向',           NOW(3)),
(9,  4, 1, DATE_SUB(NOW(), INTERVAL 2 DAY),   20.5500, 108.9000, ST_GeomFromText('POINT(20.5500 108.9000)', 4326), '北部湾西侧水域',     '油轮驶往防城港方向',                              NOW(3)),

-- 东海岛水域 (zone 5)
(10, 5, 1, DATE_SUB(NOW(), INTERVAL 4 DAY),   21.0300, 110.5600, ST_GeomFromText('POINT(21.0300 110.5600)', 4326), '东海岛南侧',         '散货船驶往宝钢码头方向',                           NOW(3)),
(11, 5, 1, DATE_SUB(NOW(), INTERVAL 5 DAY),   21.0450, 110.5400, ST_GeomFromText('POINT(21.0450 110.5400)', 4326), '东海岛北侧',         '拖轮协助大型货船靠泊钢铁基地码头',                NOW(3)),

-- 雷州半岛西岸 (zone 6)
(12, 6, 1, DATE_SUB(NOW(), INTERVAL 3 DAY),   21.0000, 110.3000, ST_GeomFromText('POINT(21.0000 110.3000)', 4326), '雷州半岛西岸',       '渔船与商船交汇区域，需注意避让',                   NOW(3)),

-- 北海港水域 (zone 7)
(13, 7, 1, DATE_SUB(NOW(), INTERVAL 6 DAY),   21.4500, 109.0800, ST_GeomFromText('POINT(21.4500 109.0800)', 4326), '北海铁山港区',       '散货船装卸作业中',                                NOW(3)),
(14, 7, 1, DATE_SUB(NOW(), INTERVAL 1 DAY),   21.4800, 109.0500, ST_GeomFromText('POINT(21.4800 109.0500)', 4326), '石步岭港区航道',     '客船进出港，交通管制中',                           NOW(3)),

-- 涠洲岛近海 (zone 8)
(15, 8, 1, DATE_SUB(NOW(), INTERVAL 7 DAY),   21.0500, 109.1200, ST_GeomFromText('POINT(21.0500 109.1200)', 4326), '涠洲岛东北水域',     '旅游客船由北海驶往涠洲岛',                         NOW(3));

-- ============================================================
-- 3. Seed ais_record_manual_vessel data (AIS记录-船舶关联)
-- ============================================================
-- Linking records to existing vessel_profiles
INSERT INTO ais_record_manual_vessel (id, observation_id, vessel_id, count_estimated, behavior, comment) VALUES
(1,  1,  7,  1, '进港航行',  'COSCO FAITH 满载集装箱进港'),
(2,  2,  1,  1, '靠泊作业',  'APL HORIZON 靠泊霞山港区装卸钢材'),
(3,  3,  13, 1, '出港航行',  'GEORGE II 拖轮协助离泊出港'),
(4,  4,  2,  1, '锚泊',      'EVER SIGNAL 锚泊等泊，预计明日靠泊'),
(5,  5,  14, 1, '锚泊候潮',  'MARCELLUS LADY 吃水深需乘潮进港'),
(6,  6,  12, 1, '穿越海峡',  'COSCO SPAIN 穿越琼州海峡驶往南海'),
(7,  7,  4,  1, '南北通行',  'SEA POWER 穿越琼州海峡'),
(8,  8,  18, 1, '南行',      'STARDUST 沿推荐航线驶往新加坡'),
(9,  9,  8,  1, '西北行',    'ALASKAN FRONTIER 油轮驶往防城港'),
(10, 10, 16, 1, '进港',      'RED BLUFF 驶往宝钢基地码头'),
(11, 11, 5,  1, '拖轮作业',  'CLEVELAND 拖轮协助靠泊'),
(12, 13, 15, 1, '装卸作业',  'RANDALL D CHAMNESS 散货装卸'),
(13, 14, 19, 1, '进出港',    'TRACY FEDKOE 客船进出港'),
(14, 15, 11, 1, '航行',      'GILBERT TAYLOR 驶往涠洲岛方向'),
(15, 1,  12, 1, '进港航行',  'COSCO SPAIN 同航道进港');

SET FOREIGN_KEY_CHECKS = 1;
