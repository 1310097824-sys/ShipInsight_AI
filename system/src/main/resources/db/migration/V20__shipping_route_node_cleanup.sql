UPDATE ecosystem
SET name = '珠江口主航道',
    type = 'ESTUARY',
    description = '珠江口进出港主航道节点，用于 AIS 点位筛选、航线地图展示和态势复核。'
WHERE name = '珠江口海湾';

UPDATE ecosystem
SET name = '琼州海峡主通道',
    type = 'REEF',
    description = '琼州海峡核心过境通道，用于跟踪跨海峡船舶流量、低速目标和时段拥挤变化。'
WHERE name = '珊瑚礁保育区';

UPDATE ecosystem
SET name = '湛江港进出航区',
    type = 'OFFSHORE',
    description = '以湛江港近海为核心的航运节点，覆盖港口入口、近岸锚地与外海主通道，用于 AIS 记录、航线地图和态势总览。'
WHERE name = '湛江近海';

UPDATE ecosystem
SET name = '北部湾外海航线',
    type = 'DEEP_SEA',
    description = '北部湾外海主航线节点，用于观察远洋过境与沿海支线的 AIS 活跃变化。'
WHERE name = '澳大利亚大堡礁外海';

UPDATE ecosystem
SET name = '雷州半岛东侧近海航路',
    type = 'OFFSHORE',
    description = '雷州半岛东侧近海航路节点，用于观察近岸补给、施工保障和出海航线分布。'
WHERE name = '印度尼西亚拉贾安帕特珊瑚礁';

UPDATE ecosystem
SET name = '硇洲岛东南作业水域',
    type = 'BAY',
    description = '硇洲岛东南侧重点作业与过境水域，用于跟踪局部 AIS 聚集、低速目标和通行态势。'
WHERE name = '菲律宾图巴塔哈珊瑚礁';

UPDATE ecosystem
SET name = '湛江港外锚地',
    type = 'MANGROVE',
    description = '湛江港外锚地节点，用于观察待泊、锚泊和近静止目标分布。'
WHERE name = '马尔代夫北马累环礁海草床';

UPDATE ecosystem
SET name = '徐闻港待泊区',
    type = 'MANGROVE',
    description = '徐闻港近岸待泊区节点，用于观察轮渡、支线船舶与待泊船队的活动变化。'
WHERE name = '泰国攀牙湾红树林';

UPDATE ecosystem
SET name = '琼州海峡西口',
    type = 'SEAGRASS',
    description = '琼州海峡西口港航衔接节点，用于查看西向进出港与海峡过境船流。'
WHERE name = '埃及红海珊瑚礁';

UPDATE ecosystem
SET name = '琼州海峡东口',
    type = 'SEAGRASS',
    description = '琼州海峡东口港航衔接节点，用于查看东向进出港与跨海峡船流。'
WHERE name = '肯尼亚拉穆红树林';

UPDATE ecosystem
SET name = '北部湾南向外海通道',
    type = 'DEEP_SEA',
    description = '面向南向外海的远洋过境通道节点，用于观察远海来去船流和速度分化。'
WHERE name = '美国夏威夷外海';

UPDATE ecosystem
SET name = '北部湾东向外海通道',
    type = 'DEEP_SEA',
    description = '面向东向外海的远洋过境通道节点，用于观察主航线外扩与交会流量。'
WHERE name = '日本冲绳近海珊瑚礁';

UPDATE ecosystem
SET name = '南三岛补给航线',
    type = 'DEEP_SEA',
    description = '南三岛方向的补给与支线航路节点，用于观察近海补给航线活动。'
WHERE name = '新西兰凯库拉外海';

DELETE FROM ai_research_report
WHERE title LIKE '%海洋生物%'
   OR title LIKE '%核心观察区域简报%';
