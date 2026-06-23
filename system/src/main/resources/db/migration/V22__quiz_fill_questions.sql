-- 填空题（FILL 题型）插入
-- 答案格式：正确答案关键词，多个用 | 分隔（后端判分时用户答案包含关键词即判对）

-- ========== SHIP 领域（船舶） ==========

-- 船舶主尺度
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('SHIP', 'FILL', '船舶的型深（Depth）是指从甲板横梁上缘到龙骨上缘的垂直距离，请问龙骨上缘对应的英文缩写是？', '[]', 'BL', 'BL（Base Line）是船舶基线，即龙骨上缘的水平线，是型深测量的基准。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'FILL', '船舶的「载重吨位」用英文缩写______表示。', '[]', 'DWT', 'DWT（Deadweight Tonnage）是deadweight tonnage的缩写，表示船舶的载重吨位，即船舶能够承载的最大重量。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'FILL', '船舶的「总吨位」用英文缩写______表示。', '[]', 'GT', 'GT（Gross Tonnage）是gross tonnage的缩写，表示船舶的总吨位，用于计算港口费用等。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'FILL', '根据《国际海上人命安全公约》，凡从事国际航行的船舶，必须持有______证书方可营运。', '[]', 'SOLAS', 'SOLAS（Safety of Life at Sea）即《国际海上人命安全公约》，是国际海事组织（IMO）制定的最重要的公约之一。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'FILL', '船舶的「IMO编号」由______位数字组成。', '[]', '7', 'IMO编号由7位数字组成，是IMO对300总吨以上国际航行船舶分配的唯一识别号。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'FILL', '集装箱船的载箱量单位「TEU」是指______英尺标准集装箱。', '[]', '20', 'TEU（Twenty-foot Equivalent Unit）是20英尺标准集装箱的换算单位。FEU指40英尺集装箱。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'FILL', '______级钢是船舶结构中使用强度最高的船体结构钢。', '[]', 'EH36|EH40', 'EH36、EH40属于高强度船体结构钢，E表示温度等级（-40°C），H表示高强度，数字为屈服强度（MPa）。', 'HARD', 1, NOW(), NOW()),
('SHIP', 'FILL', '船舶的「干舷」（Freeboard）是指从甲板中线到______的垂直距离。', '[]', '夏季载重线|夏季水线', '干舷是船中处从甲板中线到夏季载重线（夏季水线）的垂直距离，反映船舶储备浮力。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'FILL', '螺旋桨的「螺距」（Pitch）是指螺旋桨旋转一周在理论上前进的______。', '[]', '距离', '螺距是螺旋桨旋转一周在理论上前进的距离，与实际滑失率共同决定推进效率。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'FILL', '船舶的「方型系数」（Block Coefficient）用符号______表示。', '[]', 'Cb', '方型系数Cb = 船体水下体积 / (L×B×T)，反映船体水下部分的丰满程度。瘦削船型Cb约0.5-0.6，丰满船型Cb约0.8-0.85。', 'HARD', 1, NOW(), NOW());

-- 船舶设备与仪器
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('SHIP', 'FILL', '雷达测得的「物标距离」是电磁波从发射到接收所经过的路程的______。', '[]', '一半', '雷达测距原理：发射脉冲与回波脉冲的时间差的一半乘以光速，即为物标距离。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'FILL', '______罗经是依赖地球磁场工作的导航仪器。', '[]', '磁|Magnetic', '磁罗经（Magnetic Compass）利用地球磁场指示磁北，是船舶最基本的导航设备之一。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'FILL', '陀螺罗经（Gyro Compass）指向的是______。', '[]', '真北|地理北|True North', '陀螺罗经利用陀螺仪的定轴性和进动性，指向地球的地理北极（真北），不受磁偏角影响。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'FILL', 'AIS系统的全称是______。', '[]', 'Automatic Identification System', 'AIS（Automatic Identification System）即自动识别系统，用于船舶之间及船舶与岸基之间的自动信息交换。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'FILL', 'ECDIS的全称是______。', '[]', 'Electronic Chart Display and Information System', 'ECDIS即电子海图显示与信息系统，是现代船舶强制配备的导航系统。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'FILL', '船舶的主机（Main Engine）通常安装在______。', '[]', '机舱|轮机舱|Engine Room', '主机通常安装在船体的机舱（Engine Room）内，通过轴系与螺旋桨连接。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'FILL', '「船舶识别号」即IMO Number，该编号在船舶的整个营运期内保持______。', '[]', '不变|唯一|不改变', 'IMO编号在船舶整个营运期内保持不变，即使船名、船旗、船东变更也不改变。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'FILL', '船舶的「服务航速」（Service Speed）通常比最大航速低______节左右。', '[]', '2|两|2-3', '服务航速是船舶在日常营运中使用的经济航速，通常比最大航速低约2节，以节省燃油。', 'MEDIUM', 1, NOW(), NOW());

-- 国际公约
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('SHIP', 'FILL', 'MARPOL公约是关于防止船舶______污染的公约。', '[]', '海洋|marine', 'MARPOL（International Convention for the Prevention of Pollution from Ships）即《国际防止船舶造成污染公约》。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'FILL', 'STCW公约是关于船员______和发证标准的国际公约。', '[]', '培训|Training|训练', 'STCW（Standards of Training, Certification and Watchkeeping）即《海员培训、发证和值班标准国际公约》。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'FILL', 'MLC 2006公约是关于船上______福利的国际公约。', '[]', '船员|海员|seafarer', 'MLC（Maritime Labour Convention 2006）即《2006年海事劳工公约》，被称为"海员的权利法案"。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'FILL', '载重线公约（LL Convention）规定，船舶在两水域间航行时，应按______的载重线装货。', '[]', '较小|低|小', '当船舶在两水域间航行时，应按要求较严（干舷较小）一方的载重线装货，以确保安全。', 'HARD', 1, NOW(), NOW());

-- ========== WEATHER 领域（天气/气象） ==========

INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('WEATHER', 'FILL', '蒲福风级（Beaufort Scale）中，______级风对应风速约为13.9-17.1m/s，在海面表现为大浪。', '[]', '7|七', '蒲福7级风称为"Near Gale"（疾风），风速13.9-17.1m/s，海面出现大浪，白沫沿风向被吹成条带。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'FILL', '蒲福风级中，12级风的风速下限是______m/s。', '[]', '32.7|32.6', '蒲福12级风（Hurricane/飓风）风速≥32.7m/s（约118km/h或64节）。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'FILL', '热带气旋在西北太平洋称为______，在东北太平洋和大西洋称为______。', '[]', '台风|飓风|Typhoon|Hurricane', '热带气旋在不同海域有不同名称：西北太平洋称"台风"（Typhoon），大西洋和东北太平洋称"飓风"（Hurricane），印度洋称"气旋"（Cyclone）。', 'EASY', 1, NOW(), NOW()),
('WEATHER', 'FILL', '台风的「眼壁」（Eyewall）是台风中天气最______的区域。', '[]', '恶劣|坏|严重', '眼壁是围绕台风眼的环状强对流区，风速最大、降水最强、天气最恶劣。眼内反而天气晴朗。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'FILL', '海浪的「有效波高」（Significant Wave Height）是指海面______最高的波浪的平均波高。', '[]', '三分之一', '有效波高Hs定义为海面三分之一最高的波浪的平均波高，是描述海浪强度的常用指标。', 'HARD', 1, NOW(), NOW()),
('WEATHER', 'FILL', '海况等级（Sea State）中，______级海况对应波高0-0.1米，海面如镜。', '[]', '0|零', '海况0级（Calm）对应波高0-0.1米，海面光滑如镜。海况1级波高0.1-0.5米。', 'EASY', 1, NOW(), NOW()),
('WEATHER', 'FILL', '浓雾按成因可分为辐射雾、平流雾和______雾。', '[]', '蒸发|蒸汽|advection', '平流雾（Advection Fog）是暖湿空气流经冷海面形成，最常见于春夏季；辐射雾是夜间地面辐射冷却形成；蒸发雾是冷空气流经暖海面形成。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'FILL', '我国沿海雾季一般出现在______季。', '[]', '春|春夏季|3-7月', '我国沿海雾季通常为3-7月，南海始于3月，东海4-6月最盛，黄海5-7月为高峰。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'FILL', '海水密度的垂直分布中，______层是密度迅速增加的深度层。', '[]', '跃层|密度跃层|pycnocline', '密度跃层（Pycnocline）是海水中密度随深度迅速增加的层位，对潜艇隐蔽和声波传播有重要影响。', 'HARD', 1, NOW(), NOW()),
('WEATHER', 'FILL', '北半球大洋表层海流呈______时针方向流动。', '[]', '顺|反气旋式', '北半球大洋表层环流受科氏力影响呈顺时针（反气旋式）流动，南半球呈逆时针（气旋式）流动。', 'HARD', 1, NOW(), NOW()),
('WEATHER', 'FILL', '墨西哥湾流（Gulf Stream）属于______洋环流系统的一部分。', '[]', '北大西洋|North Atlantic', '墨西哥湾流是北大西洋副热带环流系统的西边界流，是世界上最强的暖流之一。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'FILL', '「海气温差」越大，海面______越强，越有利于雾的形成。', '[]', '蒸发|水汽蒸发', '当海温高于气温时，海水强烈蒸发，水汽在冷空气中凝结形成蒸发雾（蒸汽雾）。', 'MEDIUM', 1, NOW(), NOW());

-- ========== SEA_AREA 领域（海域/地理） ==========

INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('SEA_AREA', 'FILL', '台湾海峡连接了______海和______海。', '[]', '东|南|东海|南海', '台湾海峡位于台湾岛与中国大陆之间，北接东海，南接南海，是重要的国际航运通道。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'FILL', '琼州海峡位于______省和______省（区）之间。', '[]', '广东|海南', '琼州海峡位于雷州半岛（广东省）和海南岛（海南省）之间，最窄处约19.4公里。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'FILL', '马六甲海峡连接了______洋和______洋。', '[]', '印度|太平|Pacific|Indian', '马六甲海峡连接印度洋和太平洋，是沟通亚洲与欧洲、非洲的重要航道，全长约1080公里。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'FILL', '霍尔木兹海峡是______湾与______海之间的咽喉要道。', '[]', '波斯|阿曼', '霍尔木兹海峡位于波斯湾与阿曼湾之间，是世界上最重要的石油运输咽喉，约20%的世界石油贸易经此通过。', 'MEDIUM', 1, NOW(), NOW()),
('SEA_AREA', 'FILL', '苏伊士运河连接了______海和______海。', '[]', '地中|红|Mediterranean|Red', '苏伊士运河连接地中海和红海，1869年通航，大大缩短了欧洲与亚洲之间的航程。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'FILL', '巴拿马运河连接了______洋和______洋。', '[]', '大西|太平|Atlantic|Pacific', '巴拿马运河连接大西洋和太平洋，1914年通航，使美洲东西海岸之间的航程缩短了约13000公里。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'FILL', '我国最大的半岛是______半岛。', '[]', '山东|辽东|雷州', '山东半岛是我国最大的半岛，面积约39000平方公里。辽东半岛和雷州半岛分别是第二、第三大半岛。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'FILL', '我国最大的岛屿是______岛。', '[]', '台湾', '台湾岛面积约3.6万平方公里，是我国第一大岛。海南岛（约3.4万平方公里）是第二大岛。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'FILL', '「世界船运十字路口」指的是______海峡。', '[]', '马六甲|Malacca', '马六甲海峡是沟通太平洋与印度洋的咽喉要道，每年通过的船只约10万艘，被称为"世界船运十字路口"。', 'MEDIUM', 1, NOW(), NOW()),
('SEA_AREA', 'FILL', '南海四大群岛包括东沙群岛、西沙群岛、中沙群岛和______群岛。', '[]', '南沙', '南海四大群岛是中国在南海的重要领土组成部分，其中南沙群岛岛礁最多、分布最广。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'FILL', '好望角位于______洲南端，是印度洋与大西洋之间的重要航道节点。', '[]', '非|Africa', '好望角位于非洲南端，1488年由葡萄牙探险家迪亚士发现，是绕非洲航线的关键节点。', 'MEDIUM', 1, NOW(), NOW()),
('SEA_AREA', 'FILL', '世界最大的大洋是______洋。', '[]', '太平|Pacific', '太平洋面积约1.65亿平方公里，占地球表面积的约32%，是世界上最大、最深的海洋。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'FILL', '世界最深的海沟是______海沟，位于太平洋西部。', '[]', '马里亚纳|Mariana', '马里亚纳海沟最深处挑战者深渊（Challenger Deep）深达约11034米，是地球表面的最低点。', 'MEDIUM', 1, NOW(), NOW()),
('SEA_AREA', 'FILL', '______海是世界最大的内海（海域），面积约376万平方公里。', '[]', '珊瑚|大堡礁|Coral', '珊瑚海位于太平洋西南部，面积376万平方公里，是世界上最大的海。大堡礁位于珊瑚海中。', 'MEDIUM', 1, NOW(), NOW());
