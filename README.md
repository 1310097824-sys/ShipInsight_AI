# ShipInsight AI v1.2

ShipInsight AI 是一套面向 AIS 船舶数据的本地化航运智能管理系统，提供船舶档案管理、海上态势地图、AIS 数据导入与查询、AI 智能分析、RAG 知识库、知识问答和统计报表等完整功能。系统采用 Vue 3 + Spring Boot 前后端分离架构，MySQL 保存低频业务主数据，ClickHouse 保存 AIS 明细和高频轨迹数据，Qdrant/Ollama 承载 RAG 向量检索。

当前版本：`v1.2`

## v1.2 更新重点

- **全系统 AIS 主题重命名**：后端类名（`Species*`→`VesselProfile*`、`Observation*`→`AisRecordManual*`、`Ecosystem*`→`ShippingZone*`）、API 端点（`/api/v1/species`→`/vessel-profiles`、`/observations`→`/ais-records-manual`、`/ecosystems`→`/shipping-zones`）、RAG 常量（`SOURCE_SPECIES`→`SOURCE_VESSEL_PROFILE` 等）、前端类型与视图标签全面重命名，彻底脱离海洋生物主题。
- **数据库表重命名（V28）**：`ecosystem`→`shipping_zone`、`observation`→`ais_record_manual`、`observation_species`→`ais_record_manual_vessel`，废弃的 `species`/`taxon` 表已删除。
- **统计报表 AIS 主题适配**：报表端点重命名（`/protection-level`→`/risk-level`、`/iucn-status`→`/operational-status`、`/observation-trend`→`/ais-record-trend` 等），DTO 字段映射为航运领域术语，报表导出同步更新。
- **AIS 角色与权限重构（V27）**：权限和角色体系适配航运业务场景。
- **AIS 高级统计**：新增导入者排行、风险摘要、船舶草稿生成、数据集日期统计等接口。
- **清理废弃代码**：移除旧的 `SpeciesAiService`、`ObservationAiService`、`VesselProfileService/Controller/Mapper` 等不再使用的组件。
- **Lombok 编译修复**：pom.xml 显式指定 `lombok.version=1.18.34`，确保注解处理器正常工作。
- **编译验证通过**：后端 `mvn compile` BUILD SUCCESS，前端 `vue-tsc` 零错误。

### v1.1.x 更新回顾

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| `v1.1.2` | 2026-06-24 | 新增 AI 知识助手（大模型出题/问答/实时天气）；态势总览新增天气模块；集成百度地图天气 API；启动脚本支持 BAIDU_MAP_AK 配置；统一知识问答页面样式；修复学生/公众权限。 |
| `v1.1.1` | 2026-06-22 | 新增知识问答模块（239 道题目，四种题型）；修复填空题样式；更名航线地图路径。 |
| `v1.1` | 2026-06-20 | 重构船舶档案模块；新增 MarineTraffic 风格地图、单船轨迹、数据集日期筛选、AIS 全量导入和导入进度；移除前端航运网络入口。 |

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 态势总览 | 展示港口、航线、热区、AIS 动态和关键统计指标；右侧栏包含综合态势指数和今日天气出海建议。 |
| 船舶档案 | 管理船名、MMSI、IMO、呼号、船型、船旗、运营方、风险等级、航行状态、常用区域、航线范围和图片资料，支持版本回溯。 |
| 航运区域 | 管理航运区域（如港口、航道、锚地）的地理信息。 |
| 航线地图 | 基于 Leaflet 展示观察点、航线和空间分布。 |
| 海上交通图 | MarineTraffic 风格地图，展示最新船位点、船舶详情、MMSI 搜索和单船历史轨迹。 |
| AIS 记录 | 基于 ClickHouse 保存 AIS 明细，支持本地文件导入、全量导入、进度查看、条件查询、批量修改和批量删除。 |
| 手动 AIS 记录 | 手动录入和管理 AIS 观察记录，支持关联船舶档案和版本历史。 |
| 异常复核 | 对低置信度或疑似异常的 AI 分析结果进行人工复核，支持工单流转。 |
| 智能分析 | 集成 DeepSeek、RAG、业务数据和对话历史，提供自然语言船舶态势分析能力。 |
| 分析报告 | 生成、查看和导出 AI 辅助分析报告（PDF）。 |
| AIS 知识库 | 管理 RAG 文档、外部知识、分块、向量化状态和检索测试。 |
| 知识问答 | 提供船舶、天气、海域知识的题目练习，支持单选、多选、判断、填空四种题型，自动判分并保存记录。 |
| AI 知识助手 | 基于大模型的航海知识问答与出题助手，支持 AI 出题入库、实时天气查询、对话历史记录。 |
| 统计报表 | 输出风险分布、运营状态、船型分布、AIS 趋势、航运区域统计等统计视图，支持 Excel/PDF 导出。 |
| 用户权限 | 登录、注册审核、角色权限、个人中心、验证码和审计日志。 |

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 4.0.5、Spring Security、Spring Actuator |
| 数据访问 | MyBatis 4.0.0、Flyway |
| 主数据库 | MySQL 8，保存用户、权限、船舶档案、航运区域、RAG 元数据、题库等低频结构化数据 |
| AIS 明细数据库 | ClickHouse HTTP 接口，保存 AIS 明细、批量导入数据和高频轨迹记录 |
| 向量知识库 | Qdrant、Ollama `bge-m3`（1024 维） |
| AI 服务 | 阿里云百炼（Qwen-Plus，兼容 OpenAI 格式）/ DeepSeek Chat |
| 天气服务 | 百度地图天气 API（`https://api.map.baidu.com/weather/v1/`） |
| 前端 | Vue 3.5、TypeScript 6.0、Vite 8.0、Pinia 3.0、Vue Router 5.0 |
| UI 与可视化 | Element Plus 2.13、Leaflet 1.9、ECharts 6.0 |
| 文档处理 | Apache PDFBox 2.0.32、Apache POI 5.4.1 |
| 数据压缩 | Zstandard (zstd-jni 1.5.6) |

## 数据库选型

系统采用混合数据库架构：

| 数据类型 | 存储 | 原因 |
| --- | --- | --- |
| 船舶档案、用户、权限、审计、配置、题库 | MySQL | 适合增删改查、唯一约束、事务、权限和版本回溯。 |
| AIS 明细、轨迹点、按时间范围统计 | ClickHouse | 适合大批量写入、列式存储、时间范围查询和聚合分析。 |
| RAG 文档分块向量 | Qdrant | 适合向量相似度检索，不用于结构化船舶主档。 |

## 项目结构

```text
ShipInsight_AI/
├── README.md
├── .gitignore
├── scripts/                            # 根目录辅助脚本
├── handle_DATA/                        # 数据处理工具
├── handle_DATA_clean/                  # 数据清洗工具
├── system/
│   ├── src/main/java/com/gsmv/
│   │   ├── GsmvApplication.java        # 主应用入口
│   │   ├── ais/                        # AIS + ClickHouse 接入、导入、查询、地图
│   │   ├── ai/                         # 智能分析助手、RAG 知识库、复核工单、分析报告
│   │   ├── auth/                       # 登录、注册、JWT、验证码
│   │   ├── audit/                      # 审计日志
│   │   ├── bootstrap/                  # 启动初始化（RAG Schema、Flyway 配置）
│   │   ├── config/                     # 安全配置
│   │   ├── ecosystem/                  # 航运区域管理（ShippingZone）
│   │   ├── observation/                # 手动 AIS 记录管理（AisRecordManual）
│   │   ├── quiz/                       # 知识问答 + AI 知识助手 + 天气服务
│   │   ├── report/                     # 统计报表
│   │   ├── species/                    # 船型分类（VesselProfile/VesselTypeCategory）
│   │   ├── user/                       # 用户与权限
│   │   ├── vessel/                     # 船舶档案模块
│   │   ├── versioning/                 # 数据版本回溯
│   │   └── media/                      # 媒体文件管理
│   ├── src/main/resources/
│   │   ├── application.yml             # 主配置文件
│   │   └── db/migration/               # Flyway 迁移脚本（V1-V28）
│   ├── src/test/java/com/gsmv/         # 测试代码
│   ├── frontend/                       # Vue 前端
│   │   ├── src/
│   │   │   ├── api/                    # API 调用层
│   │   │   ├── types/                  # TypeScript 类型定义
│   │   │   ├── views/                  # 页面视图
│   │   │   ├── layouts/                # 布局组件
│   │   │   ├── router/                 # 路由配置
│   │   │   └── utils/                  # 工具函数
│   │   ├── vite.config.ts              # Vite 构建配置
│   │   └── package.json
│   ├── scripts/                        # 启动、运维和实验脚本
│   ├── start-gsmv.cmd                  # 系统一键启动
│   ├── stop-gsmv.cmd                   # 系统停止
│   └── pom.xml                         # Maven 构建配置
├── data/                               # 本地数据目录（Git 忽略）
├── clickhouse-data/                    # 本地 ClickHouse 数据卷（Git 忽略）
└── .workbuddy/                         # WorkBuddy 项目数据
```

## 环境要求

| 依赖 | 建议版本 |
| --- | --- |
| JDK | 17 或更高 |
| Node.js | 20 或更高 |
| npm | 10 或更高 |
| MySQL | 8.0 或更高 |
| Docker Desktop | 用于运行 ClickHouse 和 Qdrant |
| Ollama | 可选，用于本地 embedding 模型 |

## 数据库准备

### MySQL

```sql
CREATE DATABASE IF NOT EXISTS gsmv
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

默认连接配置位于 `system/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gsmv
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
```

**Flyway 迁移**：后端启动时自动执行 `system/src/main/resources/db/migration` 下的迁移脚本（当前共 28 个版本：V1-V28）。也可以单独执行迁移：

```powershell
cd D:\ShipInsight_AI\system
.\scripts\setup-flyway.cmd
```

### ClickHouse

AIS 记录默认写入 ClickHouse 的 `shipinsight.ais_records` 表：

```yaml
gsmv:
  clickhouse:
    url: http://localhost:8123
    database: shipinsight
    username: default
    password: 123456
```

启动 ClickHouse（数据卷建议放 D 盘）：

```powershell
docker run -d --name shipinsight-clickhouse `
  -p 8123:8123 -p 9000:9000 `
  -e CLICKHOUSE_PASSWORD=123456 `
  -v D:\ShipInsight_AI\clickhouse-data:/var/lib/clickhouse `
  clickhouse/clickhouse-server:25.8-alpine
```

或使用项目脚本：

```powershell
cd D:\ShipInsight_AI\system
.\scripts\start-clickhouse.cmd
```

检查连接：

```powershell
Invoke-RestMethod "http://localhost:8123/?query=SELECT%201"
```

### Qdrant 与 Ollama

```powershell
docker run -d --name gsmv-qdrant -p 6333:6333 qdrant/qdrant
ollama pull bge-m3
ollama serve
```

默认 RAG 配置：

| 配置项 | 值 |
| --- | --- |
| Qdrant URL | `http://localhost:6333` |
| Collection | `gsmv_rag_chunks` |
| Embedding 模型 | `bge-m3` |
| Embedding 维度 | 1024 |

## AI 与密钥配置

建议通过环境变量注入 Key，不要把真实密钥提交到仓库。

```powershell
# AI 对话（二选一，百炼兼容 OpenAI 格式，推荐）
setx BAILIAN_API_KEY "your-bailian-key"
# 或
setx DEEPSEEK_API_KEY "your-deepseek-key"

# 天气功能（态势总览天气模块 + AI 助手实时天气）
setx BAIDU_MAP_AK "your-baidu-map-ak" /M   # /M 写入系统级，不加则写用户级
```

也可以在启动时通过对话框输入，**密钥仅在本次启动生效，不会保存到磁盘**。

| 配置项 | 环境变量 | 说明 |
| --- | --- | --- |
| 百炼 API Key | `BAILIAN_API_KEY` | 阿里云百炼大模型 |
| DeepSeek API Key | `DEEPSEEK_API_KEY` | DeepSeek Chat |
| 百度地图 AK | `BAIDU_MAP_AK` | 地图底图服务与天气查询 |

## 启动系统

### 一键启动

```powershell
cd D:\ShipInsight_AI\system
.\start-gsmv.cmd
```

启动后会弹出 API Key 输入对话框，可填写：

| 字段 | 说明 |
| --- | --- |
| Bailian API Key | 阿里云百炼密钥，用于 AI 对话和出题 |
| DeepSeek API Key | DeepSeek 密钥（与百炼二选一） |
| BAIDU_MAP_AK | 百度地图密钥，用于天气查询 |

留空字段将回退到系统环境变量中对应的值，全部留空则 AI/天气功能不可用（其他功能正常）。

停止系统：

```powershell
cd D:\ShipInsight_AI\system
.\stop-gsmv.cmd
```

默认访问地址：

| 服务 | 地址 |
| --- | --- |
| 前端 | http://localhost:5173 |
| 后端 | http://localhost:8080 |
| ClickHouse HTTP | http://localhost:8123 |
| Qdrant | http://localhost:6333 |

默认管理员账号：

| 账号 | 密码 |
| --- | --- |
| `admin` | `123456` |

### 手动启动

后端：

```powershell
cd D:\ShipInsight_AI\system
.\mvnw.cmd spring-boot:run
```

前端：

```powershell
cd D:\ShipInsight_AI\system\frontend
npm install
npm run dev
```

## 关键 API 概览

### AIS 记录

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/ais-records` | `OBS_READ` | 分页查询 AIS 记录 |
| `GET` | `/api/v1/ais-records/map` | `OBS_READ` | 地图最新船位快照 |
| `GET` | `/api/v1/ais-records/dataset-dates` | `OBS_READ` | 数据集可选日期 |
| `GET` | `/api/v1/ais-records/{mmsi}/track` | `OBS_READ` | 单船轨迹 |
| `POST` | `/api/v1/ais-records/import` | `OBS_WRITE` | 导入 AIS 文件 |
| `GET` | `/api/v1/ais-records/import/progress/{taskId}` | `OBS_WRITE` | 查询导入进度 |
| `PATCH` | `/api/v1/ais-records/batch` | `OBS_WRITE` | 批量修改 |
| `DELETE` | `/api/v1/ais-records/batch` | `OBS_WRITE` | 批量删除 |

### 船舶档案

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/vessels` | `VESSEL_READ` | 分页查询 |
| `GET` | `/api/v1/vessels/{id}` | `VESSEL_READ` | 船舶详情 |
| `POST` | `/api/v1/vessels` | `VESSEL_WRITE` | 新增船舶 |
| `PUT` | `/api/v1/vessels/{id}` | `VESSEL_WRITE` | 修改船舶 |
| `DELETE` | `/api/v1/vessels/{id}` | `VESSEL_WRITE` | 归档船舶 |
| `GET` | `/api/v1/vessels/types` | `VESSEL_READ` | 船型选项 |
| `GET` | `/api/v1/vessels/{id}/versions` | `VESSEL_READ` | 版本历史 |
| `POST` | `/api/v1/vessels/{id}/versions/{versionId}/rollback` | `VESSEL_WRITE` | 版本回滚 |

### 智能分析

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/ai/assistant/chat` | 助手对话（非流式） |
| `POST` | `/api/v1/ai/assistant/chat/stream` | 助手对话（SSE 流式） |
| `GET` | `/api/v1/ai/assistant/messages` | 获取聊天历史 |

### 知识问答

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/quiz/questions` | 登录用户 | 分页查询题目 |
| `GET` | `/api/v1/quiz/questions/{id}` | 登录用户 | 查看题目详情 |
| `POST` | `/api/v1/quiz/questions` | 管理员 | 新增题目 |
| `PUT` | `/api/v1/quiz/questions/{id}` | 管理员 | 修改题目 |
| `DELETE` | `/api/v1/quiz/questions/{id}` | 管理员 | 删除题目 |
| `POST` | `/api/v1/quiz/submit` | 登录用户 | 提交答案并判分 |
| `GET` | `/api/v1/quiz/records` | 登录用户 | 查询答题记录 |

### AI 知识助手

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/api/v1/quiz/ai/chat` | 登录用户 | AI 对话（同步） |
| `POST` | `/api/v1/quiz/ai/chat/stream` | 登录用户 | AI 对话（SSE 流式） |
| `GET` | `/api/v1/quiz/ai/messages` | 登录用户 | 查询对话历史 |
| `DELETE` | `/api/v1/quiz/ai/messages` | 登录用户 | 清空对话历史 |
| `POST` | `/api/v1/quiz/ai/generate` | 登录用户 | AI 出题并入库 |
| `GET` | `/api/v1/quiz/ai/weather/interpret` | 无需登录 | 获取指定城市天气及出海建议 |
| `GET` | `/api/v1/quiz/ai/weather/test` | 无需登录 | 天气链路调试接口 |

### 统计报表

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/reports/summary` | 仪表盘摘要 |
| `GET` | `/api/v1/reports/risk-level` | 风险分布 |
| `GET` | `/api/v1/reports/operational-status` | 运营状态分布 |
| `GET` | `/api/v1/reports/vessel-type-distribution` | 船型分布 |
| `GET` | `/api/v1/reports/ais-record-trend` | AIS 记录趋势 |
| `GET` | `/api/v1/reports/shipping-zone-stats` | 航运区域统计 |
| `GET` | `/api/v1/reports/export/excel` | 导出 Excel |
| `GET` | `/api/v1/reports/export/pdf` | 导出 PDF |

### 其他模块

| 模块 | 基础路径 |
| --- | --- |
| 航运区域 | `/api/v1/shipping-zones` |
| 手动 AIS 记录 | `/api/v1/ais-records-manual` |
| 用户管理 | `/api/v1/users` |
| 认证 | `/api/v1/auth` |
| AI 复核工单 | `/api/v1/ai/review-tickets` |
| RAG 知识库 | `/api/v1/ai/rag` |
| AI 分析报告 | `/api/v1/ai/reports` |
| 审计日志 | `/api/v1/audits` |
| 媒体文件 | `/api/v1/media` |

## AIS 记录使用说明

1. 打开前端 `http://localhost:5173`，登录后进入「AIS 记录」页面。
2. 选择本地 AIS 数据文件导入，支持 `.csv`、`.gz`、`.tgz`、`.zst` 等格式。
3. 可以选择导入前 N 条，也可以点击全量导入，页面显示实时进度条。
4. 支持按关键词、时间范围、数据集日期查询 ClickHouse 中的 AIS 记录。
5. 表格支持分页、勾选、详情查看、批量修改/删除。
6. ClickHouse 删除采用轻量删除/后台合并机制，删除后查询结果会变化，但磁盘空间需等待后台合并释放。

## 知识问答模块使用说明

1. 打开前端 `http://localhost:5173`，登录后点击左侧「知识问答」菜单。
2. 首页可选择「分类练习」（按船舶/天气/海域分类）或「随机练习」。
3. 答题页支持单选、多选、判断、填空四种题型，填空题多个空位会显示对应数量的输入框。
4. 提交答案后自动判分，展示正确答案和答题记录。
5. 管理员可进入「题目管理」页面，新增、编辑、删除题目，支持按分类和题型筛选。

## AI 知识助手使用说明

1. 用户在注册百度地图账号后获取自己的 `BAIDU_MAP_AK`，启动 `start-gsmv.cmd` 填入，或自行设置环境变量。
2. 登录后点击左侧「知识问答」菜单，再点击「AI 知识助手」进入聊天页面。
3. 助手知识范围锁定在**船舶、天气、海域**三类，会拒绝无关话题。
4. **知识问答**：直接输入航海相关问题，AI 给出专业解答。
5. **天气查询**：输入含「天气 / 气温 / 风力」等关键词并包含城市名时，AI 自动获取百度实时天气数据后回答。
6. **AI 出题**：在「AI 出题入库」面板选择分类 / 题型 / 难度 / 数量，点击「生成并入库」，AI 出题后自动查重入库，已有题目跳过。
7. 对话历史保存在数据库，刷新页面后可继续查看，支持一键清空。

## 海上交通图

入口位于 `/marine-traffic`，设计遵循 MarineTraffic 风格：

- 底图采用浅灰陆地、浅蓝/白色海洋的极简海图风格。
- 默认展示数据集最新日期的船舶最新点，一艘船只保留一个最新点。
- 鼠标悬浮 AIS 点时展示船名、MMSI、经纬度和观测时间。
- 点击 AIS 点后打开详情面板，可查看船舶状态、航速、航向、风险和备注等字段。
- 通过 MMSI/船只编号搜索可选中船舶，再点击「显示轨迹」加载该船在全部日期中的 AIS 轨迹点和轨迹线。
- 支持按数据集日期切换地图快照。

## 构建与检查

前端构建：

```powershell
cd D:\ShipInsight_AI\system\frontend
npm run build
```

后端编译：

```powershell
cd D:\ShipInsight_AI\system
.\mvnw.cmd -DskipTests compile
```

后端测试：

```powershell
cd D:\ShipInsight_AI\system
.\mvnw.cmd test
```

## 仓库约定

以下目录是本地数据、训练数据、清洗结果或数据库运行数据，不进入 Git：

- `data/`、`one mon/`、`train/`、`clean/`、`clean1/`、`clean2/`
- `clickhouse-data/`
- `handle_DATA/`、`handle_DATA_clean/`

其他本地运行文件和构建产物同样不提交：

- `.codex_deps/`、`.idea/`、`.vscode/`
- `system/target/`、`system/frontend/node_modules/`、`system/frontend/dist/`
- `system/.gsmv-runtime/`、`system/uploads/**`
- `*.log`、`.env`、`.env.*`（保留 `!.env.example`）

提交前建议检查：

```powershell
git status --short
```

## Flyway 迁移版本记录

| 版本 | 说明 |
| --- | --- |
| V1 | 初始建表（用户、角色、权限、分类、物种、生态系统、观察记录、媒体文件、审计日志） |
| V2 | 种子数据（角色、权限、初始物种和生态系统） |
| V3-V10 | 生态系统扩展、物种增强、观察行为、全球数据种子、分布回填 |
| V11-V13 | AI 复核工单、实体版本化、AI 报告工作流 |
| V14-V16 | RAG 知识库、知识中心、AI 助手聊天历史 |
| V17 | 船舶档案模块（vessel_type、vessel 表） |
| V18-V20 | 航运助手历史重置、AIS 指标、航线节点清理 |
| V21-V26 | 知识问答模块（题库、考试、AI 问答） |
| V27 | AIS 角色与权限重构 |
| V28 | 全系统 AIS 主题表重命名 |

## 版本记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| `v1.2` | 2026-06-26 | 全系统 AIS 主题重命名；数据库表重命名（V28）；统计报表 AIS 适配；AIS 角色权限重构；清理废弃代码；Lombok 编译修复。 |
| `v1.1.2` | 2026-06-24 | 新增 AI 知识助手（大模型出题/问答/实时天气）；态势总览新增天气模块；集成百度地图天气 API；启动脚本支持 BAIDU_MAP_AK 配置；统一知识问答页面样式；修复学生/公众权限。 |
| `v1.1.1` | 2026-06-22 | 新增知识问答模块（239 道题目，四种题型）；修复填空题样式；更名航线地图路径。 |
| `v1.1` | 2026-06-20 | 重构船舶档案模块；新增 MarineTraffic 风格地图、单船轨迹、数据集日期筛选、AIS 全量导入和导入进度；移除前端航运网络入口。 |
| `v1.0` | 2026-06-17 | 初始化 ShipInsight AI 本地仓库；AIS + ClickHouse 记录导入、查询、批量修改和删除能力。 |
