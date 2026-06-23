-- V21__quiz_seed_data_extra2.sql
-- 扩充题库：新增约120道真实题目
-- 执行方式：mysql -u root -p123456 gsmv < 此文件

USE gsmv;

-- ========== SHIP 领域（约70道）==========

-- 船舶结构
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('SHIP', 'SINGLE', '船舶的载重线标志中，夏季载重线用什么字母表示？', '[{"label":"A","text":"S"},{"label":"B","text":"F"},{"label":"C","text":"T"},{"label":"D","text":"W"}]', 'A', '夏季载重线（Summer Load Line）用字母S表示。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船舶的载重线标志中，热带载重线用什么字母表示？', '[{"label":"A","text":"S"},{"label":"B","text":"F"},{"label":"C","text":"T"},{"label":"D","text":"W"}]', 'C', '热带载重线（Tropical Load Line）用字母T表示。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船舶的载重线标志中，冬季载重线用什么字母表示？', '[{"label":"A","text":"S"},{"label":"B","text":"F"},{"label":"C","text":"T"},{"label":"D","text":"W"}]', 'D', '冬季载重线（Winter Load Line）用字母W表示。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船体结构中的"肋骨"主要作用是？', '[{"label":"A","text":"支撑船壳板"},{"label":"B","text":"连接主机"},{"label":"C","text":"储存燃油"},{"label":"D","text":"固定锚链"}]', 'A', '肋骨是船体横向骨架，主要作用是支撑船壳板，保持船体形状。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船体结构中的"纵骨"沿什么方向布置？', '[{"label":"A","text":"横向"},{"label":"B","text":"纵向"},{"label":"C","text":"垂向"},{"label":"D","text":"任意方向"}]', 'B', '纵骨沿船长方向（纵向）布置，用以提高船体纵向强度。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船舶的"双层底"结构主要作用是？', '[{"label":"A","text":"提高抗沉性"},{"label":"B","text":"增加航速"},{"label":"C","text":"降低重心"},{"label":"D","text":"减少阻力"}]', 'A', '双层底结构在船底破损时仍能保持浮力，显著提高抗沉性，同时可储存燃油和压载水。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船舶最宽处的宽度称为？', '[{"label":"A","text":"型宽"},{"label":"B","text":"型深"},{"label":"C","text":"吃水"},{"label":"D","text":"干舷"}]', 'A', '型宽（Breadth）是船舶最宽处的宽度，是船舶主尺度之一。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船舶吃水标志通常标注在船艏和船艉的什么位置？', '[{"label":"A","text":"船壳板外侧"},{"label":"B","text":"甲板上"},{"label":"C","text":"货舱内"},{"label":"D","text":"机舱内"}]', 'A', '吃水标志（Draft Mark）通常标注在船艏和船艉的船壳板外侧，便于观察。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船舶的"方型系数"（Block Coefficient）反映的是？', '[{"label":"A","text":"船体水下部分的丰满程度"},{"label":"B","text":"船的宽度比例"},{"label":"C","text":"船的速度系数"},{"label":"D","text":"船的稳性高度"}]', 'A', '方型系数Cb是船体水下体积与外接长方体体积之比，反映船体水下部分的丰满程度。', 'HARD', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船舶的"菱形系数"（Prismatic Coefficient）主要影响因素是？', '[{"label":"A","text":"船体线型"},{"label":"B","text":"船舶主尺度"},{"label":"C","text":"载重量"},{"label":"D","text":"航速"}]', 'A', '菱形系数Cp反映船体沿船长方向的分布特征，与船体线型设计密切相关。', 'HARD', 1, NOW(), NOW());

-- 船舶类型
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('SHIP', 'SINGLE', 'VLCC是指载重量多少吨以上的油轮？', '[{"label":"A","text":"10万吨"},{"label":"B","text":"15万吨"},{"label":"C","text":"20万吨"},{"label":"D","text":"30万吨"}]', 'C', 'VLCC（Very Large Crude Carrier）指载重量20万吨以上的超大型原油船。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', 'ULCC是指载重量多少吨以上的油轮？', '[{"label":"A","text":"20万吨"},{"label":"B","text":"30万吨"},{"label":"C","text":"40万吨"},{"label":"D","text":"50万吨"}]', 'B', 'ULCC（Ultra Large Crude Carrier）指载重量30万吨以上的特大型原油船。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '集装箱船的"TEU"是指什么？', '[{"label":"A","text":"20英尺标准集装箱"},{"label":"B","text":"40英尺标准集装箱"},{"label":"C","text":"船舶载重量单位"},{"label":"D","text":"船舶长度单位"}]', 'A', 'TEU（Twenty-foot Equivalent Unit）是20英尺标准集装箱的换算单位。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '下列哪种船舶通常采用双壳结构？', '[{"label":"A","text":"油轮"},{"label":"B","text":"散货船"},{"label":"C","text":"集装箱船"},{"label":"D","text":"以上都是"}]', 'D', 'MARPOL公约要求油轮、散货船、集装箱船等都必须采用双壳结构，防止油污泄漏。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', 'LNG船是指运输什么货物的船舶？', '[{"label":"A","text":"液化天然气"},{"label":"B","text":"液化石油气"},{"label":"C","text":"化学品"},{"label":"D","text":"冷藏货物"}]', 'A', 'LNG（Liquefied Natural Gas）船专门运输液化天然气，货物温度约-163°C。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', 'LPG船是指运输什么货物的船舶？', '[{"label":"A","text":"液化天然气"},{"label":"B","text":"液化石油气"},{"label":"C","text":"氨水"},{"label":"D","text":"甲醇"}]', 'B', 'LPG（Liquefied Petroleum Gas）船专门运输液化石油气，如丙烷、丁烷等。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '"汽车运输船"（PCTC）的主要特征是？', '[{"label":"A","text":"多层甲板"},{"label":"B","text":"深舱结构"},{"label":"C","text":"双壳结构"},{"label":"D","text":"集装箱绑扎系统"}]', 'A', 'PCTC（Pure Car/Truck Carrier）采用多层甲板设计，可高效装载大量汽车。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '下列哪种船舶不属于液货船？', '[{"label":"A","text":"油轮"},{"label":"B","text":"化学品船"},{"label":"C","text":"LNG船"},{"label":"D","text":"滚装船"}]', 'D', '滚装船（Ro-Ro）通过跳板装卸轮式货物，不属于液货船。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '"支线集装箱船"（Feeder）通常的载箱量范围是？', '[{"label":"A","text":"100-500 TEU"},{"label":"B","text":"500-3000 TEU"},{"label":"C","text":"3000-8000 TEU"},{"label":"D","text":"8000 TEU以上"}]', 'B', '支线集装箱船（Feeder）载箱量通常在500-3000 TEU之间，用于港口间驳运。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '"超巴拿马型"船舶是指？', '[{"label":"A","text":"可以通过巴拿马运河的船舶"},{"label":"B","text":"不能通过巴拿马运河的船舶"},{"label":"C","text":"可以通过苏伊士运河的船舶"},{"label":"D","text":"可以通过马六甲海峡的船舶"}]', 'B', '超巴拿马型船舶是指主尺度超过巴拿马运河船闸限制的船舶，通常宽度超过32.3米。', 'MEDIUM', 1, NOW(), NOW());

-- 导航设备
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('SHIP', 'SINGLE', 'GPS卫星导航系统由多少颗卫星组成全球定位网？', '[{"label":"A","text":"18颗"},{"label":"B","text":"24颗"},{"label":"C","text":"30颗"},{"label":"D","text":"36颗"}]', 'B', 'GPS系统由24颗工作卫星（21颗工作+3颗备用）组成全球定位网。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', 'GPS提供的定位精度在没有增强的情况下约为？', '[{"label":"A","text":"1-5米"},{"label":"B","text":"10-20米"},{"label":"C","text":"50-100米"},{"label":"D","text":"100-200米"}]', 'B', '标准GPS单点定位精度约为10-20米，使用差分GPS（DGPS）可提高至1-5米。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', 'AIS系统使用的VHF频段是？', '[{"label":"A","text":"VHF 16频道"},{"label":"B","text":"VHF 70频道"},{"label":"C","text":"VHF 87B/88B频道"},{"label":"D","text":"VHF 13频道"}]', 'C', 'AIS使用VHF信道的87B（161.975MHz）和88B（162.025MHz）进行数据传输。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '雷达测距的精度主要受什么因素影响？', '[{"label":"A","text":"脉冲宽度"},{"label":"B","text":"天线增益"},{"label":"C","text":"显示器亮度"},{"label":"D","text":"电源电压"}]', 'A', '脉冲宽度越窄，距离分辨率越高，测距精度越好。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '雷达测方位时，电子方位线的精度通常为？', '[{"label":"A","text":"±0.1°"},{"label":"B","text":"±0.5°"},{"label":"C","text":"±1°"},{"label":"D","text":"±5°"}]', 'B', '雷达测方位的精度通常为±0.5°左右，受天线波束宽度和显示器分辨率影响。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', 'ECDIS是指？', '[{"label":"A","text":"电子海图显示与信息系统"},{"label":"B","text":"全球定位系统"},{"label":"C","text":"自动识别系统"},{"label":"D","text":"船舶监控系统"}]', 'A', 'ECDIS（Electronic Chart Display and Information System）是电子海图显示与信息系统，是IMO要求的航行安全系统。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', 'ECDIS使用的电子海图数据标准是由哪个组织制定的？', '[{"label":"A","text":"IMO"},{"label":"B","text":"IHO"},{"label":"C","text":"ISO"},{"label":"D","text":"ITU"}]', 'B', 'IHO（国际海道测量组织）制定了S-57和S-101电子海图数据标准。', 'HARD', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '磁罗经的"自差"（Deviation）是指？', '[{"label":"A","text":"磁北与真北的夹角"},{"label":"B","text":"罗经北与磁北的夹角"},{"label":"C","text":"磁北与罗经北的夹角"},{"label":"D","text":"真北与罗经北的夹角"}]', 'B', '自差（Deviation）是船上钢铁结构导致的罗经误差，即罗经北与磁北之间的夹角。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '陀螺罗经的指北原理是基于？', '[{"label":"A","text":"地球磁场"},{"label":"B","text":"陀螺仪的定向性"},{"label":"C","text":"太阳方位"},{"label":"D","text":"星体位置"}]', 'B', '陀螺罗经利用高速旋转陀螺仪的定向性和进动性，在地球自转作用下自动找北。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船舶测深仪（Echo Sounder）的工作原理是？', '[{"label":"A","text":"声波反射"},{"label":"B","text":"电磁波反射"},{"label":"C","text":"重力感应"},{"label":"D","text":"水压测量"}]', 'A', '测深仪向水底发射声波，接收反射回波，根据声波往返时间计算水深。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', 'VDR（Voyage Data Recorder）是指？', '[{"label":"A","text":"航行数据记录仪"},{"label":"B","text":"自动识别系统"},{"label":"C","text":"电子海图系统"},{"label":"D","text":"卫星应急示位标"}]', 'A', 'VDR（Voyage Data Recorder）即船舶航行数据记录仪，类似航空器黑匣子，记录船舶航行数据。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', 'AIS系统的消息发送间隔，在船舶高速航行时约为？', '[{"label":"A","text":"2秒"},{"label":"B","text":"10秒"},{"label":"C","text":"30秒"},{"label":"D","text":"60秒"}]', 'A', 'AIS在航速超过23节时，消息发送间隔约为2秒；航速2-23节时约10秒。', 'HARD', 1, NOW(), NOW());

-- 国际公约与法规
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('SHIP', 'SINGLE', 'SOLAS公约的全称是？', '[{"label":"A","text":"国际海上人命安全公约"},{"label":"B","text":"国际防止船舶造成污染公约"},{"label":"C","text":"海员培训、发证和值班标准国际公约"},{"label":"D","text":"国际载重线公约"}]', 'A', 'SOLAS（International Convention for the Safety of Life at Sea）即国际海上人命安全公约，是最重要的国际海事安全公约。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', 'SOLAS公约适用于多少总吨以上的国际航行船舶？', '[{"label":"A","text":"100总吨"},{"label":"B","text":"300总吨"},{"label":"C","text":"500总吨"},{"label":"D","text":"1000总吨"}]', 'C', 'SOLAS公约适用于500总吨及以上的国际航行船舶。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', 'MARPOL公约是针对什么问题的国际公约？', '[{"label":"A","text":"海上安全"},{"label":"B","text":"防止船舶污染海洋"},{"label":"C","text":"船员培训标准"},{"label":"D","text":"船舶载重线"}]', 'B', 'MARPOL（International Convention for the Prevention of Pollution from Ships）即国际防止船舶造成污染公约。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', 'MARPOL公约附则I是关于什么的？', '[{"label":"A","text":"防止油类污染"},{"label":"B","text":"防止散装有毒液体污染"},{"label":"C","text":"防止包装有害物质污染"},{"label":"D","text":"防止垃圾污染"}]', 'A', 'MARPOL附则I：防止油类污染规则；附则II：散装有毒液体物质；附则IV：生活污水；附则V：垃圾。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', 'STCW公约的全称是？', '[{"label":"A","text":"国际海上人命安全公约"},{"label":"B","text":"海员培训、发证和值班标准国际公约"},{"label":"C","text":"国际劳工公约"},{"label":"D","text":"国际海事公约"}]', 'B', 'STCW（Standards of Training, Certification and Watchkeeping for Seafarers）即海员培训、发证和值班标准国际公约。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '国际海事组织（IMO）的总部设在哪个城市？', '[{"label":"A","text":"伦敦"},{"label":"B","text":"日内瓦"},{"label":"C","text":"纽约"},{"label":"D","text":"巴黎"}]', 'A', 'IMO总部设在英国伦敦。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '《国际海上避碰规则》规定，机动船在航时应当显示什么号灯？', '[{"label":"A","text":"桅灯、舷灯、尾灯"},{"label":"B","text":"锚灯"},{"label":"C","text":"失控灯"},{"label":"D","text":"限于吃水灯"}]', 'A', '机动船在航时应显示：桅灯（白，前）、舷灯（红左绿右）、尾灯（白，后）。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '《国际海上避碰规则》规定，船舶在锚泊时应显示？', '[{"label":"A","text":"锚灯（白，环照）"},{"label":"B","text":"桅灯和舷灯"},{"label":"C","text":"失控灯"},{"label":"D","text":"拖带灯"}]', 'A', '锚泊船应显示锚灯（白色环照灯），船长超过100米时还应在前部另加一盏锚灯。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '根据避碰规则，追越船是指从被追越船正横后多少度以外赶上的船舶？', '[{"label":"A","text":"10°"},{"label":"B","text":"22.5°"},{"label":"C","text":"45°"},{"label":"D","text":"90°"}]', 'B', '追越船是指从被追越船正横后22.5°以后方向上赶来的船舶。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '根据避碰规则，一艘机动船与另一艘机动船交叉相遇致有构成碰撞危险时，有什么规定？', '[{"label":"A","text":"有他船在本船右舷者，应给他船让路"},{"label":"B","text":"有他船在本船左舷者，应给他船让路"},{"label":"C","text":"两船各自向右转向"},{"label":"D","text":"两船各自向左转向"}]', 'A', '交叉相遇局面中，有他船在本船右舷者（即看见他船红灯者），应给他船让路（右舷对左舷原则）。', 'MEDIUM', 1, NOW(), NOW());

-- 船舶稳性
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('SHIP', 'SINGLE', '船舶初稳性高度（GM）为正值时，船舶具有什么稳性？', '[{"label":"A","text":"正稳性"},{"label":"B","text":"负稳性"},{"label":"C","text":"中性稳性"},{"label":"D","text":"动稳性"}]', 'A', 'GM>0时船舶具有正稳性，受到外力倾斜后会自行回正。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船舶稳性高度（GM）为负值时，船舶处于什么状态？', '[{"label":"A","text":"稳定平衡"},{"label":"B","text":"不稳定平衡（倾覆危险）"},{"label":"C","text":"中性平衡"},{"label":"D","text":"动稳性状态"}]', 'B', 'GM<0时船舶处于不稳定平衡状态，受到外力后会继续倾斜直至倾覆。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '自由液面对船舶稳性的影响是？', '[{"label":"A","text":"降低稳性"},{"label":"B","text":"提高稳性"},{"label":"C","text":"不影响稳性"},{"label":"D","text":"只影响纵稳性"}]', 'A', '液舱内液体自由表面会在船舶倾斜时产生额外力矩，降低船舶稳性。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '集装箱船在货物配载时，应特别注意什么？', '[{"label":"A","text":"重心不能过高"},{"label":"B","text":"纵向强度"},{"label":"C","text":"自由液面"},{"label":"D","text":"以上都是"}]', 'D', '集装箱船配载需综合考虑重心高度（稳性）、纵向强度分布、集装箱重量等级等多方面因素。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船舶的"静稳性曲线"（GZ曲线）中，最大静稳性臂对应的角度称为？', '[{"label":"A","text":"初稳性角度"},{"label":"B","text":"动稳性角度"},{"label":"C","text":"甲板边缘入水角"},{"label":"D","text":"最大静稳性臂对应角"}]', 'D', 'GZ曲线峰值对应的角度称为最大静稳性臂对应角，超过此角度后稳性臂开始减小。', 'HARD', 1, NOW(), NOW());

-- 动力与推进
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('SHIP', 'SINGLE', '船舶主机大多使用什么类型的发动机？', '[{"label":"A","text":"汽油机"},{"label":"B","text":"蒸汽机"},{"label":"C","text":"柴油机"},{"label":"D","text":"电动机"}]', 'C', '现代船舶主机绝大多数使用柴油机，因为柴油机热效率高、可靠性好、燃油经济。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '大型低速二冲程柴油机的转速范围通常为？', '[{"label":"A","text":"50-250 rpm"},{"label":"B","text":"300-1000 rpm"},{"label":"C","text":"1000-3000 rpm"},{"label":"D","text":"3000 rpm以上"}]', 'A', '大型低速二冲程柴油机转速通常为50-250 rpm，可直接驱动螺旋桨，无需减速齿轮箱。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '螺旋桨的"滑失"（Slip）是指？', '[{"label":"A","text":"理论进程与实际进程之差"},{"label":"B","text":"螺旋桨叶片损坏"},{"label":"C","text":"螺旋桨空转"},{"label":"D","text":"螺旋桨反转"}]', 'A', '滑失（Slip）是螺旋桨理论进程（螺距×转速）与实际进程之差，通常用百分比表示。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船舶的"倒车功率"通常为主机额定功率的多少？', '[{"label":"A","text":"50%以下"},{"label":"B","text":"50-70%"},{"label":"C","text":"70-90%"},{"label":"D","text":"100%"}]', 'A', '受排气温度和机械强度限制，倒车功率通常不超过额定功率的50%。', 'HARD', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船舶发电柴油机（辅机）通常使用什么转速？', '[{"label":"A","text":"低速"},{"label":"B","text":"中速"},{"label":"C","text":"高速"},{"label":"D","text":"不一定"}]', 'C', '发电柴油机（辅机）通常为高速柴油机（1000-1800 rpm），直接驱动发电机。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '平衡舵的特点是？', '[{"label":"A","text":"舵面积部分在舵杆前方"},{"label":"B","text":"舵面积全部在舵杆后方"},{"label":"C","text":"有两个舵叶"},{"label":"D","text":"可360度旋转"}]', 'A', '平衡舵的舵面积有一部分位于舵杆前方，可减小转舵力矩，节省舵机功率。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船舶侧推器（Side Thruster）通常安装在什么位置？', '[{"label":"A","text":"船艏"},{"label":"B","text":"船艉"},{"label":"C","text":"船中"},{"label":"D","text":"船艏或船艉"}]', 'D', '侧推器可安装在船艏（艏侧推）和/或船艉（艉侧推），用于提高船舶靠离泊操纵性。', 'EASY', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '船舶舵的工作原理是基于？', '[{"label":"A","text":"伯努利原理"},{"label":"B","text":"牛顿第三定律"},{"label":"C","text":"阿基米德原理"},{"label":"D","text":"以上都是"}]', 'D', '舵的工作原理涉及流体力学多个方面：伯努利原理（压力差产生升力）、牛顿第三定律（水流偏转产生反作用力）。', 'MEDIUM', 1, NOW(), NOW());

-- 货物操作
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('SHIP', 'SINGLE', '散装谷物在运输过程中的主要风险是？', '[{"label":"A","text":"因自由液面导致稳性降低"},{"label":"B","text":"因谷物移动导致船舶倾斜"},{"label":"C","text":"因挥发导致爆炸"},{"label":"D","text":"因吸湿导致重量增加"}]', 'B', '散装谷物在船舶摇摆时可能发生移动，导致船舶倾斜甚至倾覆，是散货船运输的主要风险。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '集装箱船的"配载计划"（Bay Plan）中，集装箱位置用几位代码表示？', '[{"label":"A","text":"4位"},{"label":"B","text":"6位"},{"label":"C","text":"8位"},{"label":"D","text":"10位"}]', 'B', '集装箱位置代码通常为6位：Bay（2位）+ Row（2位）+ Tier（2位）。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '油轮装卸货时，"油气置换"（Gas Freeing）的目的是？', '[{"label":"A","text":"提高卸货速度"},{"label":"B","text":"确保安全，防止爆炸"},{"label":"C","text":"提高货物质量"},{"label":"D","text":"减少蒸发损失"}]', 'B', '油气置换是用惰性气体或新鲜空气替换货舱内的可燃油气，防止爆炸事故发生。', 'MEDIUM', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '化学品船运输货物时，"兼容性"（Compatibility）是指？', '[{"label":"A","text":"不同化学品能否相邻装载"},{"label":"B","text":"化学品与船体材料的相容性"},{"label":"C","text":"以上都是"},{"label":"D","text":"以上都不是"}]', 'C', '化学品运输的兼容性包括：不同化学品之间是否反应、化学品与船体结构材料是否相容等多个方面。', 'HARD', 1, NOW(), NOW()),
('SHIP', 'SINGLE', '冷藏集装箱（Reefer Container）的温度控制依据是？', '[{"label":"A","text":"固定为0°C至+5°C"},{"label":"B","text":"取决于具体货物的要求"},{"label":"C","text":"固定为-18°C"},{"label":"D","text":"由船长决定"}]', 'B', '冷藏集装箱的温度控制范围取决于所运输的具体货物，不同货物有不同的温度要求。', 'EASY', 1, NOW(), NOW());

-- ========== WEATHER 领域（约40道）==========

-- 蒲福风级
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('WEATHER', 'SINGLE', '蒲福风级中，1级风（轻风）的风速范围是？', '[{"label":"A","text":"0.3-1.5 m/s"},{"label":"B","text":"1.6-3.3 m/s"},{"label":"C","text":"3.4-5.4 m/s"},{"label":"D","text":"5.5-7.9 m/s"}]', 'B', '蒲福1级风（轻风）风速1.6-3.3 m/s，烟能表示风向，但风向标不能转动。', 'EASY', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '蒲福风级中，6级风（强风）的风速范围是？', '[{"label":"A","text":"10.8-13.8 m/s"},{"label":"B","text":"13.9-17.1 m/s"},{"label":"C","text":"17.2-20.7 m/s"},{"label":"D","text":"20.8-24.4 m/s"}]', 'A', '蒲福6级风（强风）风速10.8-13.8 m/s，大树枝摇动，电线呼呼有声，举伞困难。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '蒲福风级中，12级风（飓风）的最低风速是？', '[{"label":"A","text":"32.7 m/s"},{"label":"B","text":"38.0 m/s"},{"label":"C","text":"44.0 m/s"},{"label":"D","text":"51.0 m/s"}]', 'A', '蒲福12级风（飓风）风速≥32.7 m/s（约118 km/h或64节）。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '蒲福风级中，0级风（无风）的海面特征是？', '[{"label":"A","text":"海面如镜"},{"label":"B","text":"鳞状小波"},{"label":"C","text":"短小波浪"},{"label":"D","text":"大浪"}]', 'A', '0级风（无风）海面如镜，完全没有波浪。', 'EASY', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '蒲福风级中，9级风（烈风）的风速范围是？', '[{"label":"A","text":"20.8-24.4 m/s"},{"label":"B","text":"24.5-28.4 m/s"},{"label":"C","text":"28.5-32.6 m/s"},{"label":"D","text":"32.7-36.9 m/s"}]', 'A', '蒲福9级风（烈风）风速20.8-24.4 m/s，烟囱顶部和平屋被吹损，小屋受损。', 'MEDIUM', 1, NOW(), NOW());

-- 海况与海浪
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('WEATHER', 'SINGLE', '道格拉斯海况等级中，3级海况（涌浪）对应的波高是？', '[{"label":"A","text":"0-0.1米"},{"label":"B","text":"0.1-0.5米"},{"label":"C","text":"0.5-1.25米"},{"label":"D","text":"1.25-2.5米"}]', 'C', '道格拉斯海况3级（涌浪）波高0.5-1.25米，波浪不大但较为明显。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '有效波高（Significant Wave Height）是指？', '[{"label":"A","text":"最高波高"},{"label":"B","text":"三分之一最高波高的平均值"},{"label":"C","text":"平均波高"},{"label":"D","text":"最低波高"}]', 'B', '有效波高（Hs）是指波浪记录中最高三分之一波高的平均值，是描述海浪大小最常用的参数。', 'HARD', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '海浪的"波长"是指？', '[{"label":"A","text":"波峰到波谷的垂直距离"},{"label":"B","text":"相邻两波峰之间的水平距离"},{"label":"C","text":"波浪传播的速度"},{"label":"D","text":"波浪的周期"}]', 'B', '波长（Wavelength）是相邻两个波峰（或波谷）之间的水平距离。', 'EASY', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '海浪的"波周期"是指？', '[{"label":"A","text":"波浪传播100米所需时间"},{"label":"B","text":"相邻两波峰通过某点的时间间隔"},{"label":"C","text":"波浪从生成到消失的时间"},{"label":"D","text":"一天内波浪的数量"}]', 'B', '波周期（Wave Period）是相邻两波峰通过固定点的时间间隔，单位为秒。', 'EASY', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '当波浪传播到浅水区时，会发生什么现象？', '[{"label":"A","text":"波速增加，波长变长"},{"label":"B","text":"波速降低，波长变短，波高增大"},{"label":"C","text":"波形不变"},{"label":"D","text":"波浪消失"}]', 'B', '波浪进入浅水区后，波速降低，波长变短，波高增大，最终可能破碎形成拍岸浪。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '"疯狗浪"（Rogue Wave）的波高通常是有效波高的几倍？', '[{"label":"A","text":"1倍"},{"label":"B","text":"1.5倍"},{"label":"C","text":"2倍以上"},{"label":"D","text":"0.5倍"}]', 'C', '疯狗浪（异常大浪）的波高通常是有效波高的2倍甚至更高，具有突发性和破坏性。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '海洋中最大的海浪通常出现在？', '[{"label":"A","text":"太平洋南部"},{"label":"B","text":"大西洋北部（咆哮西风带）"},{"label":"C","text":"印度洋"},{"label":"D","text":"北冰洋"}]', 'B', '南大洋（咆哮西风带，40°S-60°S）终年盛行西风，无陆地阻挡，是世界上浪最大的海域。', 'MEDIUM', 1, NOW(), NOW());

-- 雾与能见度
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('WEATHER', 'SINGLE', '船舶导航中，"平流雾"是如何形成的？', '[{"label":"A","text":"地表辐射冷却"},{"label":"B","text":"暖湿空气流经冷海面"},{"label":"C","text":"两股冷空气混合"},{"label":"D","text":"降水蒸发"}]', 'B', '平流雾是暖湿空气流经冷海面时，底层空气冷却至露点以下形成的雾，是海上最常见的雾。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '"辐射雾"通常在什么时候最容易出现？', '[{"label":"A","text":"白天"},{"label":"B","text":"傍晚"},{"label":"C","text":"夜间至清晨"},{"label":"D","text":"中午"}]', 'C', '辐射雾在夜间至清晨最易出现，因为此时地表辐射冷却最强，空气温度降至露点以下。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '海上雾中最常用的导航设备是？', '[{"label":"A","text":"雷达"},{"label":"B","text":"GPS"},{"label":"C","text":"罗经"},{"label":"D","text":"测深仪"}]', 'A', '雷达是雾中航行最重要的导航设备，可以发现周围船舶和障碍物。', 'EASY', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '根据国际海上避碰规则，船舶在雾中航行时应？', '[{"label":"A","text":"以安全航速行驶，并随时准备停车"},{"label":"B","text":"正常速度航行"},{"label":"C","text":"加速通过雾区"},{"label":"D","text":"抛锚等待雾散"}]', 'A', '雾中航行规则要求船舶以安全航速行驶，并随时准备停车，同时应每不超过2分钟鸣放声响信号。', 'EASY', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '雾中航行时，每艘船舶应至少每隔多少分钟鸣放一次声响信号？', '[{"label":"A","text":"1分钟"},{"label":"B","text":"2分钟"},{"label":"C","text":"5分钟"},{"label":"D","text":"10分钟"}]', 'B', '在能见度不良的水域中，机动船在航时应每隔不超过2分钟鸣放一长声。', 'MEDIUM', 1, NOW(), NOW());

-- 台风与气旋
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('WEATHER', 'SINGLE', '西北太平洋上生成的热带气旋称为？', '[{"label":"A","text":"飓风"},{"label":"B","text":"台风"},{"label":"C","text":"气旋"},{"label":"D","text":"以上都可以"}]', 'B', '西北太平洋上生成的热带气旋称为台风（Typhoon）；北大西洋和东北太平洋称为飓风（Hurricane）；印度洋称为气旋（Cyclone）。', 'EASY', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '台风的"危险半圆"是指？', '[{"label":"A","text":"台风前进方向的右半圆"},{"label":"B","text":"台风前进方向的左半圆"},{"label":"C","text":"台风中心区域"},{"label":"D","text":"台风后方区域"}]', 'A', '在北半球，台风前进方向的右半圆称为危险半圆，因为该区域风速更大且更容易将船舶吹入台风中心。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '台风的"可航半圆"是指？', '[{"label":"A","text":"台风前进方向的右半圆"},{"label":"B","text":"台风前进方向的左半圆"},{"label":"C","text":"台风中心区域"},{"label":"D","text":"台风外围区域"}]', 'B', '在北半球，台风前进方向的左半圆称为可航半圆，该区域风速相对较小，偏离台风中心。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '台风中心（风眼）的风速特征是？', '[{"label":"A","text":"风速最大"},{"label":"B","text":"风速很小甚至无风"},{"label":"C","text":"风速中等"},{"label":"D","text":"与外围相同"}]', 'B', '台风风眼内风速很小甚至无风，天空可能出现短暂放晴，但风眼周围（眼壁）风速最大。', 'EASY', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '热带气旋按最大持续风速分类，台风（Typhoon）的风速标准是？', '[{"label":"A",">63节"},{"label":"B",">64节"},{"label":"C",">74节"},{"label":"D",">84节"}]', 'C', '热带气旋分类：热带低压（<34节）、热带风暴（34-63节）、强热带风暴（64-73节）、台风（≥74节）。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '船舶在台风中的正确应对措施（北半球）是？', '[{"label":"A","text":"在危险半圆内应使船尾受风"},{"label":"B","text":"在可航半圆内应使船首受风"},{"label":"C","text":"以上都是"},{"label":"D","text":"以上都不是"}]', 'C', '北半球台风中：危险半圆（右半圆）应使船尾受风，全速驶离；可航半圆（左半圆）应使船首受风，全速驶离。', 'HARD', 1, NOW(), NOW());

-- 洋流与潮汐
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('WEATHER', 'SINGLE', '世界第一大洋流是？', '[{"label":"A","text":"墨西哥湾流"},{"label":"B","text":"黑潮"},{"label":"C","text":"南极绕极流"},{"label":"D","text":"北大西洋暖流"}]', 'C', '南极绕极流（Antarctic Circumpolar Current）是世界上最大的洋流，流量约为100 Sv。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '黑潮（Kuroshio Current）是沿哪个海域流动的暖流？', '[{"label":"A","text":"沿南美洲西海岸"},{"label":"B","text":"沿日本列岛和台湾以东"},{"label":"C","text":"沿非洲西海岸"},{"label":"D","text":"沿澳大利亚东海岸"}]', 'B', '黑潮是沿台湾以东、日本列岛东岸向北流动的暖流，是世界第二大洋流。', 'EASY', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '在潮汐表中，"高潮"是指？', '[{"label":"A","text":"潮水最高时刻"},{"label":"B","text":"潮水最低时刻"},{"label":"C","text":"涨潮最快时刻"},{"label":"D","text":"落潮最快时刻"}]', 'A', '高潮（High Water）是潮汐涨至最高位置的时刻和状态。', 'EASY', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '半日潮是指一天内有几次高潮和低潮？', '[{"label":"A","text":"一次高潮一次低潮"},{"label":"B","text":"两次高潮两次低潮"},{"label":"C","text":"四次高潮四次低潮"},{"label":"D","text":"不一定"}]', 'B', '半日潮（Semi-diurnal Tide）在一天（约24小时50分钟）内有两次高潮和两次低潮。', 'EASY', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '"大潮"（Spring Tide）出现在什么月相时？', '[{"label":"A","text":"新月和满月"},{"label":"B","text":"上弦月和下弦月"},{"label":"C","text":"朔望之间"},{"label":"D","text":"任意月相"}]', 'A', '大潮出现在新月（朔）和满月（望）时，此时太阳、月球和地球几乎在一条直线上，引潮力最大。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '"小潮"（Neap Tide）出现在什么月相时？', '[{"label":"A","text":"新月和满月"},{"label":"B","text":"上弦月和下弦月"},{"label":"C","text":"朔望之间"},{"label":"D","text":"任意月相"}]', 'B', '小潮出现在上弦月和弦月时，此时太阳和月球的引潮力相互抵消，潮汐幅度最小。', 'MEDIUM', 1, NOW(), NOW()),
('WEATHER', 'SINGLE', '中国沿海的潮汐类型以什么为主？', '[{"label":"A","text":"半日潮"},{"label":"B","text":"全日潮"},{"label":"C","text":"混合潮"},{"label":"D","text":"不规则潮"}]', 'A', '中国沿海（如青岛、上海）以半日潮为主，一天内有两次高潮和两次低潮。', 'MEDIUM', 1, NOW(), NOW());

-- ========== SEA_AREA 领域（约30道）==========

-- 中国海域
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('SEA_AREA', 'SINGLE', '中国最大的海域是？', '[{"label":"A","text":"渤海"},{"label":"B","text":"黄海"},{"label":"C","text":"东海"},{"label":"D","text":"南海"}]', 'D', '南海面积约350万平方公里，是中国最大的海域，也是西太平洋最大的边缘海。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '渤海与黄海的分界线是？', '[{"label":"A","text":"辽东半岛南端至山东半岛北岸"},{"label":"B","text":"长江口北岸至济州岛"},{"label":"C","text":"广东南澳岛至台湾南端"},{"label":"D","text":"台湾海峡"}]', 'A', '渤海与黄海以辽东半岛南端老铁山角至山东半岛北岸蓬莱角的连线为界。', 'MEDIUM', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '黄海与东海的分界线是？', '[{"label":"A","text":"辽东半岛南端至山东半岛"},{"label":"B","text":"长江口北岸至韩国济州岛"},{"label":"C","text":"广东南澳岛至台湾"},{"label":"D","text":"台湾海峡中线"}]', 'B', '黄海与东海以长江口北岸至韩国济州岛西南角的连线为界。', 'MEDIUM', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '东海与南海的分界线是？', '[{"label":"A","text":"台湾南端（鹅銮鼻）至广东南澳岛"},{"label":"B","text":"琼州海峡"},{"label":"C","text":"巴士海峡"},{"label":"D","text":"台湾海峡"}]', 'A', '东海与南海以台湾南端鹅銮鼻至广东南澳岛的连线为界。', 'MEDIUM', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '琼州海峡连接的是哪两个水域？', '[{"label":"A","text":"南海和北部湾"},{"label":"B","text":"南海和东海"},{"label":"C","text":"黄海和东海"},{"label":"D","text":"渤海和黄海"}]', 'A', '琼州海峡位于雷州半岛和海南岛之间，连接南海和北部湾。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '台湾海峡连接的是哪两个水域？', '[{"label":"A","text":"南海和北部湾"},{"label":"B","text":"东海和南海"},{"label":"C","text":"黄海和东海"},{"label":"D","text":"渤海和黄海"}]', 'B', '台湾海峡位于台湾岛和中国大陆之间，连接东海和南海。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '中国最大的半岛是？', '[{"label":"A","text":"辽东半岛"},{"label":"B","text":"山东半岛"},{"label":"C","text":"雷州半岛"},{"label":"D","text":"九龙半岛"}]', 'B', '山东半岛是中国最大的半岛，面积约7.3万平方公里。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '中国最大的岛屿是？', '[{"label":"A","text":"海南岛"},{"label":"B","text":"台湾岛"},{"label":"C","text":"崇明岛"},{"label":"D","text":"舟山岛"}]', 'B', '台湾岛面积约3.6万平方公里，是中国最大的岛屿。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '舟山群岛位于中国哪个海域？', '[{"label":"A","text":"渤海"},{"label":"B","text":"黄海"},{"label":"C","text":"东海"},{"label":"D","text":"南海"}]', 'C', '舟山群岛位于浙江省东北部、杭州湾外东海中，是中国最大的群岛。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '南海诸岛中，位置最南的是？', '[{"label":"A","text":"东沙群岛"},{"label":"B","text":"西沙群岛"},{"label":"C","text":"中沙群岛"},{"label":"D","text":"南沙群岛"}]', 'D', '南沙群岛位于南海最南部，其中曾母暗沙是中国领土最南端。', 'EASY', 1, NOW(), NOW());

-- 世界重要海域
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('SEA_AREA', 'SINGLE', '世界上最大的洋是？', '[{"label":"A","text":"大西洋"},{"label":"B","text":"太平洋"},{"label":"C","text":"印度洋"},{"label":"D","text":"北冰洋"}]', 'B', '太平洋面积约1.65亿平方公里，占地球表面积的约32%，是世界第一大洋。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '世界上最大的海是？', '[{"label":"A","text":"南海"},{"label":"B","text":"珊瑚海"},{"label":"C","text":"阿拉伯海"},{"label":"D","text":"加勒比海"}]', 'B', '珊瑚海位于太平洋西南部，面积约479万平方公里，是世界上最大的海。', 'MEDIUM', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '波罗的海的盐度特征是？', '[{"label":"A","text":"盐度很高"},{"label":"B","text":"盐度很低（半咸水）"},{"label":"C","text":"盐度与世界大洋相同"},{"label":"D","text":"盐度随季节剧烈变化"}]', 'B', '波罗的海因大量淡水注入且蒸发量小，盐度很低（7-8‰），部分地区甚至低至2‰，是半咸水海。', 'MEDIUM', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '死海是世界上海拔最低的湖泊，其盐度约为？', '[{"label":"A","text":"约3.5%"},{"label":"B","text":"约10%"},{"label":"C","text":"约34.2%"},{"label":"D","text":"约0.5%"}]', 'C', '死海盐度约为34.2%，是普通海水的约10倍，人可漂浮其上。', 'MEDIUM', 1, NOW(), NOW());

-- 海峡与运河
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('SEA_AREA', 'SINGLE', '马六甲海峡连接的是哪两个水域？', '[{"label":"A","text":"南海和安达曼海"},{"label":"B","text":"红海和地中海"},{"label":"C","text":"波斯湾和阿曼湾"},{"label":"D","text":"太平洋和大西洋"}]', 'A', '马六甲海峡位于马来半岛和苏门答腊岛之间，连接南海（太平洋）和安达曼海（印度洋）。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '霍尔木兹海峡连接的是哪两个水域？', '[{"label":"A","text":"红海和亚丁湾"},{"label":"B","text":"波斯湾和阿曼湾"},{"label":"C","text":"马六甲海峡和南海"},{"label":"D","text":"台湾海峡和东海"}]', 'B', '霍尔木兹海峡位于伊朗和阿曼之间，连接波斯湾和阿曼湾（阿拉伯海），是世界石油运输最重要的咽喉要道。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '巴拿马运河连接的是哪两个大洋？', '[{"label":"A","text":"大西洋和太平洋"},{"label":"B","text":"太平洋和印度洋"},{"label":"C","text":"大西洋和印度洋"},{"label":"D","text":"北冰洋和大西洋"}]', 'A', '巴拿马运河横穿巴拿马地峡，连接大西洋（加勒比海）和太平洋，大大缩短了美洲东西海岸的航程。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '苏伊士运河连接的是哪两个水域？', '[{"label":"A","text":"大西洋和太平洋"},{"label":"B","text":"红海和地中海"},{"label":"C","text":"波斯湾和印度洋"},{"label":"D","text":"黑海和地中海"}]', 'B', '苏伊士运河连接地中海和红海（印度洋），1869年通航，是世界上最重要的海运通道之一。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '土耳其海峡（黑海海峡）连接的是哪两个水域？', '[{"label":"A","text":"黑海和地中海"},{"label":"B","text":"红海和地中海"},{"label":"C","text":"波斯湾和阿曼湾"},{"label":"D","text":"波罗的海和北海"}]', 'A', '土耳其海峡（包括博斯普鲁斯海峡和达达尼尔海峡）连接黑海和地中海，是黑海沿岸国家的唯一出海通道。', 'MEDIUM', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '麦哲伦海峡位于哪个大洲的最南端？', '[{"label":"A","text":"非洲"},{"label":"B","text":"南美洲"},{"label":"C","text":"北美洲"},{"label":"D","text":"亚洲"}]', 'B', '麦哲伦海峡位于南美洲最南端，连接大西洋和太平洋，在巴拿马运河开通前是必经航路。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '白令海峡连接的是哪两个水域？', '[{"label":"A","text":"北冰洋和太平洋"},{"label":"B","text":"大西洋和太平洋"},{"label":"C","text":"印度洋和太平洋"},{"label":"D","text":"黑海和地中海"}]', 'A', '白令海峡位于俄罗斯（楚科奇半岛）和美国（阿拉斯加）之间，连接北冰洋（楚科奇海）和太平洋（白令海）。', 'MEDIUM', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '直布罗陀海峡连接的是哪两个水域？', '[{"label":"A","text":"地中海和大西洋"},{"label":"B","text":"红海和地中海"},{"label":"C","text":"波斯湾和阿曼湾"},{"label":"D","text":"波罗的海和北海"}]', 'A', '直布罗陀海峡位于西班牙和摩洛哥之间，连接地中海和大西洋，是地中海的唯一出海口。', 'EASY', 1, NOW(), NOW());

-- 世界主要港口
INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) VALUES
('SEA_AREA', 'SINGLE', '世界上集装箱吞吐量最大的港口是？', '[{"label":"A","text":"新加坡港"},{"label":"B","text":"上海港"},{"label":"C","text":"鹿特丹港"},{"label":"D","text":"洛杉矶港"}]', 'B', '上海港自2010年起连续多年位居世界第一集装箱大港，年吞吐量超过4000万TEU。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '新加坡港位于哪个海峡的咽喉位置？', '[{"label":"A","text":"马六甲海峡"},{"label":"B","text":"霍尔木兹海峡"},{"label":"C","text":"英吉利海峡"},{"label":"D","text":"巴拿马运河"}]', 'A', '新加坡港位于马六甲海峡的东南端，扼守太平洋与印度洋之间的交通要道。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '鹿特丹港位于哪个国家？', '[{"label":"A","text":"德国"},{"label":"B","text":"荷兰"},{"label":"C","text":"比利时"},{"label":"D","text":"丹麦"}]', 'B', '鹿特丹港位于荷兰，是欧洲最大的港口，素有"欧洲门户"之称。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '"好望角"位于哪个国家？', '[{"label":"A","text":"澳大利亚"},{"label":"B","text":"南非"},{"label":"C","text":"阿根廷"},{"label":"D","text":"智利"}]', 'B', '好望角位于南非共和国西南端，是连接大西洋和印度洋的重要航道节点。', 'EASY', 1, NOW(), NOW()),
('SEA_AREA', 'SINGLE', '巴拿马运河的船闸系统需要消耗大量淡水，这些水来自？', '[{"label":"A","text":"海水淡化"},{"label":"B","text":"加通湖（淡水湖）"},{"label":"C","text":"地下水"},{"label":"D","text":"雨水收集"}]', 'B', '巴拿马运河的船闸系统使用加通湖（Gatun Lake）的淡水，每次船舶通过约消耗2亿升淡水。', 'MEDIUM', 1, NOW(), NOW());
