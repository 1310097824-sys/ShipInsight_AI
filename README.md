# ShipInsight AI v1.1.1

ShipInsight AI 是一套面向 AIS 船舶数据、船舶档案、海上态势地图、风险复核和智能分析的本地化航运系统。系统采用 Vue 3 + Spring Boot 架构，MySQL 保存低频业务主数据，ClickHouse 保存 AIS 明细和高频轨迹数据，Qdrant/Ollama 承载 RAG 知识库与向量检索。

当前版本：`v1.1.1`

## v1.1.1 更新重点

- 新增**知识问答模块**，支持船舶、天气、海域三类知识题目，包含单选、多选、判断、填空四种题型。
- 前端新增"知识问答"菜单入口，包含首页（分类练习、随机练习）、答题页、结果页、管理页（增删改查题目）。
- 后端新增 `com.gsmv.quiz` 包，提供题目分页查询、按分类/题型筛选、提交答案自动判分、答题记录保存等接口。
- 数据库新增 `quiz_questions` 和 `quiz_records` 两张表，并通过 Flyway 迁移脚本（`V18`–`V20`）自动建表与初始化数据。
- 初始题库包含 239 道题目（船舶 104 道、天气 65 道、海域 70 道），涵盖 SINGLE、MULTI、JUDGE、FILL 四种题型。
- 修复填空题多空作答样式，输入框改为下划线风格，与题目文本视觉统一。
- 将"航线地图"菜单路径从 `/eco-map` 更名为 `/route-map`，去除旧项目痕迹。

## v1.1 更新重点

- 船舶档案从旧的海洋生物 `species` 模型中拆出，新增独立 `vessel` 模块、船型表、船舶主档表、船舶图片和版本回溯能力。
- AIS 记录页支持按本地文件导入前 N 条或全量导入，导入过程中显示实时进度条。
- AIS 查询支持关键词、时间范围、数据集日期筛选、分页、详情、批量修改和批量删除。
- 新增 MarineTraffic 风格海上交通图，采用灰色极简海图风格，并按 `[-360°世界] [0°世界] [+360°世界]` 展示三份世界地图。
- 地图默认展示数据集最新日期的数据，并按船舶保留一个最新点；悬浮显示船名/位置，点击显示详情。
- 地图支持按船只编号/MMSI 搜索选中船舶，点击"显示轨迹"后加载该船全部 AIS 轨迹点和轨迹线。
- 新增数据集日期筛选功能，可查看指定日期的最新船位快照。
- 移除前端"航运网络"菜单入口，旧 `/ecosystems` 路由重定向到态势总览。
- README 和 Git 忽略规则已按 v1.1 重新整理，本地数据目录不进入仓库。

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 态势总览 | 展示港口、航线、热区、AIS 动态和关键统计指标。 |
| 船舶档案 | 管理船名、MMSI、IMO、呼号、船型、船旗、运营方、风险等级、航行状态、常用区域、航线范围和图片资料。 |
| 航线地图 | 基于 Leaflet 展示观察点、航线和空间分布。 |
| 海上交通图 | MarineTraffic 风格地图，展示最新船位点、船舶详情、MMSI 搜索和单船历史轨迹。 |
| AIS 记录 | 基于 ClickHouse 保存 AIS 明细，支持本地文件导入、全量导入、进度查看、条件查询、批量修改和批量删除。 |
| 异常复核 | 对低置信度或疑似异常的 AI 分析结果进行人工复核。 |
| 智能分析 | 集成 DeepSeek、RAG、业务数据和对话历史，提供自然语言分析能力。 |
| 分析报告 | 生成、查看和导出 AI 辅助分析报告。 |
| 知识问答 | 提供船舶、天气、海域知识的题目练习，支持单选、多选、判断、填空四种题型，自动判分并保存记录。 |
| AIS 知识库 | 管理 RAG 文档、外部知识、分块、向量化状态和检索测试。 |
| 统计报表 | 输出 AIS、船型、航线、观察活动等统计视图。 |
| 用户权限 | 登录、注册审核、角色权限、个人中心和审计日志。 |

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 4、Spring Security、Spring Actuator |
| 数据访问 | MyBatis、Flyway |
| 主数据数据库 | MySQL 8，保存用户、权限、船舶档案、知识题库、业务配置等低频结构化数据 |
| AIS 明细数据库 | ClickHouse HTTP 接口，保存 AIS 明细、批量导入数据和高频轨迹记录 |
| 向量知识库 | Qdrant、Ollama `bge-m3` |
| AI 服务 | DeepSeek Chat、DashScope/百炼视觉模型、IUCN API |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Vue Router |
| UI 与可视化 | Element Plus、Leaflet、ECharts |

## 数据库选型

系统采用混合数据库架构：

| 数据类型 | 存储 | 原因 |
| --- | --- | --- |
| 船舶档案、用户、权限、审计、配置、知识题库 | MySQL | 适合增删改查、唯一约束、事务、权限和版本回溯。 |
| AIS 明细、轨迹点、按时间范围统计 | ClickHouse | 适合大批量写入、列式存储、时间范围查询和聚合分析。 |
| RAG 文档分块向量 | Qdrant | 适合向量相似度检索，不用于结构化船舶主档。 |

当前版本不引入 MongoDB 或 PostgreSQL，避免增加额外服务复杂度。

## 项目结构

```text
ShipInsight_AI/
├─ README.md
├─ .gitignore
├─ scripts/                         # 辅助脚本（题库生成、数据清洗等）
├─ system/
│  ├─ src/main/java/com/gsmv/
│  │  ├─ ais/                       # AIS + ClickHouse 接入、导入、查询、地图点位
│  │  ├─ ai/                        # 智能分析、RAG、复核、报告
│  │  ├─ auth/                      # 登录、注册、JWT
│  │  ├─ audit/                     # 审计日志
│  │  ├─ ecosystem/                 # 旧航运网络后端兼容模块
│  │  ├─ observation/               # 观察业务
│  │  ├─ quiz/                      # v1.1.1 知识问答模块
│  │  ├─ report/                    # 统计报表
│  │  ├─ species/                   # 旧 species 兼容代码，不再作为船舶档案页面入口
│  │  ├─ vessel/                    # v1.1 船舶档案模块
│  │  ├─ user/                      # 用户与权限
│  │  └─ versioning/                # 数据版本回溯
│  ├─ src/main/resources/
│  │  ├─ application.yml
│  │  └─ db/migration/              # Flyway 迁移脚本（V1–V20）
│  ├─ frontend/                     # Vue 前端
│  ├─ start-gsmv.cmd               # 一键启动脚本
│  ├─ stop-gsmv.cmd                # 一键停止脚本
│  └─ pom.xml
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
    url: jdbc:mysql://localhost:3306/gsmv?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456
```

后端启动时会通过 Flyway 自动执行 `system/src/main/resources/db/migration` 下的迁移脚本。v1.1.1 新增 `V18__quiz_knowledge_base.sql`、`V19__quiz_seed_data.sql`、`V20__quiz_seed_data_extra.sql`，用于创建知识题库表、答题记录表和初始化题目数据。

### ClickHouse

AIS 记录默认写入 ClickHouse 的 `shipinsight.ais_records` 表，配置项如下：

```yaml
gsmv:
  clickhouse:
    url: ${CLICKHOUSE_URL:http://localhost:8123}
    database: ${CLICKHOUSE_DATABASE:shipinsight}
    username: ${CLICKHOUSE_USERNAME:default}
    password: ${CLICKHOUSE_PASSWORD:123456}
```

推荐把 ClickHouse 数据卷放在 D 盘：

```powershell
docker run -d --name shipinsight-clickhouse `
  -p 8123:8123 -p 9000:9000 `
  -e CLICKHOUSE_PASSWORD=123456 `
  -v D:\ShipInsight_AI\clickhouse-data:/var/lib/clickhouse `
  clickhouse/clickhouse-server:25.8-alpine
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

```text
Qdrant URL: http://localhost:6333
Collection: gsmv_rag_chunks
Embedding model: bge-m3
Embedding dimension: 1024
```

## AI 与密钥配置

建议通过环境变量注入 Key，不要把真实密钥提交到仓库。

```powershell
setx BAILIAN_API_KEY "your-bailian-key"
setx DASHSCOPE_API_KEY "your-bailian-key"
setx DEEPSEEK_API_KEY "your-deepseek-key"
setx IUCN_API_TOKEN "your-iucn-token"
```

## 启动系统

### 一键启动

```powershell
cd D:\ShipInsight_AI\system
.\start-gsmv.cmd
```

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

## 知识问答模块使用说明

1. 打开前端 `http://localhost:5173`，登录后点击左侧"知识问答"菜单。
2. 首页可选择"分类练习"（按船舶/天气/海域分类）或"随机练习"。
3. 答题页支持单选、多选、判断、填空四种题型，填空题多个空位会显示对应数量的输入框。
4. 提交答案后自动判分，展示正确答案和答题记录。
5. 管理员可进入"题目管理"页面，新增、编辑、删除题目，支持按分类和题型筛选。

知识问答后端 API：

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/quiz/questions` | 登录用户 | 分页查询题目。 |
| `GET` | `/api/v1/quiz/questions/{id}` | 登录用户 | 查看题目详情。 |
| `POST` | `/api/v1/quiz/questions` | 管理员 | 新增题目。 |
| `PUT` | `/api/v1/quiz/questions/{id}` | 管理员 | 修改题目。 |
| `DELETE` | `/api/v1/quiz/questions/{id}` | 管理员 | 删除题目。 |
| `POST` | `/api/v1/quiz/submit` | 登录用户 | 提交答案并判分。 |
| `GET` | `/api/v1/quiz/records` | 登录用户 | 查询答题记录。 |

## AIS 记录使用说明

1. 打开前端 `http://localhost:5173`，登录后进入 `AIS 记录` 页面。
2. 选择本地 AIS 数据文件导入，支持 `.csv`、`.gz`、`.tgz`、`.zst` 等本地数据包格式。
3. 可以选择导入前 N 条，也可以点击全量导入。全量导入会读取文件内全部 AIS 记录，页面显示一条实时进度条。
4. 支持按关键词、观察时间范围、数据集日期查询 ClickHouse 中的 AIS 记录。
5. 表格支持分页、勾选、详情查看、按勾选结果批量修改/删除，以及按当前查询结果批量修改/删除。

AIS 后端 API：

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/ais-records` | `OBS_READ` | 查询 AIS 记录。 |
| `GET` | `/api/v1/ais-records/map` | `OBS_READ` | 查询地图最新船位快照。 |
| `GET` | `/api/v1/ais-records/dataset-dates` | `OBS_READ` | 查询数据集中可选日期。 |
| `GET` | `/api/v1/ais-records/{mmsi}/track` | `OBS_READ` | 查询单船全部轨迹点。 |
| `POST` | `/api/v1/ais-records/import` | `OBS_WRITE` | 导入本地 AIS 文件。 |
| `GET` | `/api/v1/ais-records/import/progress/{taskId}` | `OBS_WRITE` | 查询导入进度。 |
| `PATCH` | `/api/v1/ais-records/batch` | `OBS_WRITE` | 批量修改 AIS 记录。 |
| `DELETE` | `/api/v1/ais-records/batch` | `OBS_WRITE` | 批量删除 AIS 记录。 |

## 构建与检查

前端构建：

```powershell
cd D:\ShipInsight_AI\system\frontend
npm.cmd run build
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

以下目录是本地数据、训练数据、清洗结果或数据库运行数据，不进入 Git，也不会上传到 GitHub：

- `data/`
- `one mon/`
- `train/`
- `clean/`
- `clean1/`
- `clean2/`
- `clickhouse-data/`

其他本地运行文件和构建产物同样不提交：

- `.idea/`
- `.vscode/`
- `system/target/`
- `system/frontend/node_modules/`
- `system/frontend/dist/`
- `system/.gsmv-runtime/`
- `*.log`
- `.env`、`.env.*`
- `.workbuddy/`

## 版本记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| `v1.1.1` | 2026-06-22 | 新增知识问答模块（239道题目，四种题型）；修复填空题样式；更名航线地图路径。 |
| `v1.1` | 2026-06-20 | 重构船舶档案模块；新增 MarineTraffic 风格地图、单船轨迹、数据集日期筛选、AIS 全量导入和导入进度；移除前端航运网络入口。 |
| `v1.0` | 2026-06-17 | 初始化 ShipInsight AI 本地仓库；补充 AIS + ClickHouse 记录导入、查询、批量修改和删除能力。 |
