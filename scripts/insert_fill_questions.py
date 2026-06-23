import mysql.connector
import sys
import os

# 设置标准输出编码为 UTF-8
sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

# 填空题数据
questions = [
    # ===== SHIP 领域 =====
    ("SHIP", "FILL", "船舶的型深（Depth）是指从甲板横梁上缘到龙骨上缘的垂直距离，请问龙骨上缘对应的英文缩写是？",
     "BL", "BL（Base Line）是船舶基线，即龙骨上缘的水平线，是型深测量的基准。", "MEDIUM"),
    ("SHIP", "FILL", "船舶的「载重吨位」用英文缩写______表示。",
     "DWT", "DWT（Deadweight Tonnage）是deadweight tonnage的缩写，表示船舶的载重吨位。", "EASY"),
    ("SHIP", "FILL", "船舶的「总吨位」用英文缩写______表示。",
     "GT", "GT（Gross Tonnage）是gross tonnage的缩写，表示船舶的总吨位。", "EASY"),
    ("SHIP", "FILL", "根据《国际海上人命安全公约》，凡从事国际航行的船舶，必须持有______证书方可营运。",
     "SOLAS", "SOLAS公约是国际海事组织（IMO）制定的最重要的公约之一。", "MEDIUM"),
    ("SHIP", "FILL", "船舶的「IMO编号」由______位数字组成。",
     "7", "IMO编号由7位数字组成，是IMO对300总吨以上国际航行船舶分配的唯一识别号。", "EASY"),
    ("SHIP", "FILL", "集装箱船的载箱量单位「TEU」是指______英尺标准集装箱。",
     "20", "TEU是20英尺标准集装箱的换算单位。FEU指40英尺集装箱。", "EASY"),
    ("SHIP", "FILL", "船舶的「干舷」（Freeboard）是指甲板中线到______的垂直距离。",
     "夏季载重线|夏季水线", "干舷是船中处从甲板中线到夏季载重线的垂直距离，反映船舶储备浮力。", "MEDIUM"),
    ("SHIP", "FILL", "螺旋桨的「螺距」（Pitch）是指螺旋桨旋转一周在理论上前进的______。",
     "距离", "螺距是螺旋桨旋转一周在理论上前进的距离，与实际滑失率共同决定推进效率。", "MEDIUM"),
    ("SHIP", "FILL", "雷达测得的「物标距离」是电磁波从发射到接收所经过的路程的______。",
     "一半", "雷达测距原理：发射脉冲与回波脉冲的时间差的一半乘以光速，即为物标距离。", "MEDIUM"),
    ("SHIP", "FILL", "______罗经是依赖地球磁场工作的导航仪器。",
     "磁|Magnetic", "磁罗经利用地球磁场指示磁北，是船舶最基本的导航设备之一。", "EASY"),
    ("SHIP", "FILL", "AIS系统的全称是______。",
     "Automatic Identification System", "AIS即自动识别系统，用于船舶之间及船舶与岸基之间的自动信息交换。", "EASY"),
    ("SHIP", "FILL", "ECDIS的全称是______。",
     "Electronic Chart Display and Information System", "ECDIS即电子海图显示与信息系统，是现代船舶强制配备的导航系统。", "MEDIUM"),
    ("SHIP", "FILL", "船舶的主机（Main Engine）通常安装在______。",
     "机舱|轮机舱|Engine Room", "主机通常安装在船体的机舱内，通过轴系与螺旋桨连接。", "EASY"),
    ("SHIP", "FILL", "MARPOL公约是关于防止船舶______污染的公约。",
     "海洋|marine", "MARPOL公约即《国际防止船舶造成污染公约》。", "EASY"),
    ("SHIP", "FILL", "STCW公约是关于船员______和发证标准的国际公约。",
     "培训|Training|训练", "STCW公约即《海员培训、发证和值班标准国际公约》。", "MEDIUM"),

    # ===== WEATHER 领域 =====
    ("WEATHER", "FILL", "蒲福风级（Beaufort Scale）中，______级风对应风速约为13.9-17.1m/s，在海面表现为大浪。",
     "7|七", "蒲福7级风称为Near Gale（疾风），海面出现大浪。", "MEDIUM"),
    ("WEATHER", "FILL", "蒲福风级中，12级风的风速下限是______m/s。",
     "32.7|32.6", "蒲福12级风（Hurricane）风速≥32.7m/s。", "MEDIUM"),
    ("WEATHER", "FILL", "热带气旋在西北太平洋称为______，在大西洋和东北太平洋称为______。",
     "台风|飓风|Typhoon|Hurricane", "热带气旋在不同海域有不同名称。", "EASY"),
    ("WEATHER", "FILL", "台风的「眼壁」（Eyewall）是台风中天气最______的区域。",
     "恶劣|坏|严重", "眼壁是围绕台风眼的环状强对流区，风速最大、降水最强。", "MEDIUM"),
    ("WEATHER", "FILL", "海浪的「有效波高」（Significant Wave Height）是指海面______最高的波浪的平均波高。",
     "三分之一", "有效波高Hs定义为海面三分之一最高的波浪的平均波高。", "HARD"),
    ("WEATHER", "FILL", "海况等级（Sea State）中，______级海况对应波高0-0.1米，海面如镜。",
     "0|零", "海况0级（Calm）对应波高0-0.1米，海面光滑如镜。", "EASY"),
    ("WEATHER", "FILL", "浓雾按成因可分为辐射雾、平流雾和______雾。",
     "蒸发|蒸汽|advection", "平流雾是暖湿空气流经冷海面形成；蒸发雾是冷空气流经暖海面形成。", "MEDIUM"),
    ("WEATHER", "FILL", "我国沿海雾季一般出现在______季。",
     "春|春夏季|3-7月", "我国沿海雾季通常为3-7月，南海始于3月，东海4-6月最盛。", "MEDIUM"),
    ("WEATHER", "FILL", "海水密度的垂直分布中，______层是密度迅速增加的深度层。",
     "跃层|密度跃层|pycnocline", "密度跃层对潜艇隐蔽和声波传播有重要影响。", "HARD"),
    ("WEATHER", "FILL", "北半球大洋表层海流呈______时针方向流动。",
     "顺|反气旋式", "北半球大洋表层环流受科氏力影响呈顺时针（反气旋式）流动。", "HARD"),
    ("WEATHER", "FILL", "「海气温差」越大，海面______越强，越有利于雾的形成。",
     "蒸发|水汽蒸发", "当海温高于气温时，海水强烈蒸发，水汽在冷空气中凝结形成蒸发雾。", "MEDIUM"),

    # ===== SEA_AREA 领域 =====
    ("SEA_AREA", "FILL", "台湾海峡连接了______海和______海。",
     "东|南|东海|南海", "台湾海峡位于台湾岛与中国大陆之间，北接东海，南接南海。", "EASY"),
    ("SEA_AREA", "FILL", "琼州海峡位于______省和______省（区）之间。",
     "广东|海南", "琼州海峡位于雷州半岛（广东省）和海南岛（海南省）之间。", "EASY"),
    ("SEA_AREA", "FILL", "马六甲海峡连接了______洋和______洋。",
     "印度|太平|Pacific|Indian", "马六甲海峡连接印度洋和太平洋，全长约1080公里。", "EASY"),
    ("SEA_AREA", "FILL", "霍尔木兹海峡是______湾与______海之间的咽喉要道。",
     "波斯|阿曼", "霍尔木兹海峡位于波斯湾与阿曼湾之间，是世界上最重要的石油运输咽喉。", "MEDIUM"),
    ("SEA_AREA", "FILL", "苏伊士运河连接了______海和______海。",
     "地中|红|Mediterranean|Red", "苏伊士运河连接地中海和红海，1869年通航。", "EASY"),
    ("SEA_AREA", "FILL", "巴拿马运河连接了______洋和______洋。",
     "大西|太平|Atlantic|Pacific", "巴拿马运河连接大西洋和太平洋，1914年通航。", "EASY"),
    ("SEA_AREA", "FILL", "我国最大的半岛是______半岛。",
     "山东|辽东|雷州", "山东半岛是我国最大的半岛，面积约39000平方公里。", "EASY"),
    ("SEA_AREA", "FILL", "我国最大的岛屿是______岛。",
     "台湾", "台湾岛面积约3.6万平方公里，是我国第一大岛。", "EASY"),
    ("SEA_AREA", "FILL", "「世界船运十字路口」指的是______海峡。",
     "马六甲|Malacca", "马六甲海峡是沟通太平洋与印度洋的咽喉要道，每年通过船只约10万艘。", "MEDIUM"),
    ("SEA_AREA", "FILL", "南海四大群岛包括东沙群岛、西沙群岛、中沙群岛和______群岛。",
     "南沙", "南海四大群岛是中国在南海的重要领土组成部分。", "EASY"),
    ("SEA_AREA", "FILL", "好望角位于______洲南端，是印度洋与大西洋之间的重要航道节点。",
     "非|Africa", "好望角位于非洲南端，1488年由葡萄牙探险家迪亚士发现。", "MEDIUM"),
    ("SEA_AREA", "FILL", "世界最大的大洋是______洋。",
     "太平|Pacific", "太平洋面积约1.65亿平方公里，占地球表面积的约32%。", "EASY"),
    ("SEA_AREA", "FILL", "世界最深的海沟是______海沟，位于太平洋西部。",
     "马里亚纳|Mariana", "马里亚纳海沟最深处挑战者深渊深达约11034米，是地球表面的最低点。", "MEDIUM"),
]

log_path = "D:/study and work/ruanjianshixun/ShipInsight_AI-main1.1/scripts/insert_fill_log.txt"
with open(log_path, "w", encoding="utf-8") as f:
    f.write("开始插入填空题...\n")

try:
    conn = mysql.connector.connect(
        host="localhost",
        user="root",
        password="123456",
        database="gsmv",
        charset="utf8mb4"
    )
    cursor = conn.cursor()
    
    sql = """INSERT INTO quiz_question 
             (category, type, title, options, answer, explanation, difficulty, status, created_at, updated_at) 
             VALUES (%s, %s, %s, %s, %s, %s, %s, 1, NOW(), NOW())"""
    
    count = 0
    errors = 0
    with open(log_path, "a", encoding="utf-8") as log:
        for q in questions:
            category, qtype, title, answer, explanation, difficulty = q
            options = "[]"
            try:
                cursor.execute(sql, (category, qtype, title, options, answer, explanation, difficulty))
                count += 1
                log.write(f"  成功: [{category}][{qtype}] {title[:30]}...\n")
            except Exception as e:
                errors += 1
                log.write(f"  失败: {title[:30]}... 错误: {e}\n")
    
    conn.commit()
    cursor.close()
    conn.close()
    
    with open(log_path, "a", encoding="utf-8") as log:
        log.write(f"\n共插入 {count} 道填空题，{errors} 道失败！\n")
    
    print(f"完成！成功 {count} 道，失败 {errors} 道")
    print(f"详细日志: {log_path}")
    
except Exception as e:
    with open(log_path, "a", encoding="utf-8") as log:
        log.write(f"数据库连接失败: {e}\n")
    print(f"数据库连接失败: {e}")
    sys.exit(1)
