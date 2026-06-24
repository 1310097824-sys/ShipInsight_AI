# ShipInsight AI v1.1.2

ShipInsight AI 是一套面向 AIS 船舶数据、船舶档案、海上态势地图、风险复核和智能分析的本地化航运系统。系统采用 Vue 3 + Spring Boot 架构，MySQL 保存低频业务主数据，ClickHouse 保存 AIS 明细和高频轨迹数据，Qdrant/Ollama 承载 RAG 知识库与向量检索。

当前版本：`v1.1.2`

## v1.1.2 更新重点

- 新增 **AI 知识助手**，集成阿里云百炼（Qwen）/ DeepSeek，锁定船舶、天气、海域三类知识，支持 AI 出题并自动入库（查重），支持对话历史记录。
- 新增**态势总览天气模块**，默认显示广东湛江今日天气及 AI 出海建议，每秒刷新当前时间，提示用户可到 AI 知识助手查询其他地区天气。
- 集成**百度地图天气 API**，AI 助手在检测到天气关键词时自动获取实时天气数据注入 prompt。
- 启动脚本 (`start-gsmv.cmd`) 新增 `BAIDU_MAP_AK` 输入框，支持每次启动时配置百度地图 AK。
- 知识问答页面样式统一为 `page-shell` / `page-hero` / `panel-card` 风格，与其他模块视觉一致。
- 修复 Element Plus `el-checkbox` / `el-radio` 弃用警告（`label` → `value`）。
- 修复学生/公众角色无法看到知识问答菜单的权限问题。

## v1.1.1 更新重点

- 新增**知识问答模块**，支持船舶、天气、海域三类知识题目，包含单选、多选、判断、填空四种题型。
- 修复填空题多空作答样式，输入框改为下划线风格，与题目文本视觉统一。
- 将"航线地图"菜单路径从 `/eco-map` 更名为 `/route-map`，去除旧项目痕迹。

## v1.1 更新重点

- 船舶档案从旧的海洋生物 `species` 模型中拆出，新增独立 `vessel` 模块、船型表、船舶主档表、船舶图片和版本回溯能力。
- AIS 记录页支持按本地文件导入前 N 条或全量导入，导入过程中显示实时进度条。
- 新增 MarineTraffic 风格海上交通图，支持按船舶 MMSI 搜索和单船历史轨迹。
- 新增数据集日期筛选功能，可查看指定日期的最新船位快照。

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 态势总览 | 展示港口、航线、热区、AIS 动态和关键统计指标；右侧栏包含综合态势指数和今日天气出海建议。 |
| 船舶档案 | 管理船名、MMSI、IMO、呼号、船型、船旗、运营方、风险等级、航行状态、常用区域、航线范围和图片资料。 |
| 航线地图 | 基于 Leaflet 展示观察点、航线和空间分布。 |
| 海上交通图 | MarineTraffic 风格地图，展示最新船位点、船舶详情、MMSI 搜索和单船历史轨迹。 |
| AIS 记录 | 基于 ClickHouse 保存 AIS 明细，支持本地文件导入、全量导入、进度查看、条件查询、批量修改和批量删除。 |
| 异常复核 | 对低置信度或疑似异常的 AI 分析结果进行人工复核。 |
| 智能分析 | 集成 DeepSeek、RAG、业务数据和对话历史，提供自然语言分析能力。 |
| 分析报告 | 生成、查看和导出 AI 辅助分析报告。 |
| 知识问答 | 提供船舶、天气、海域知识的题目练习，支持单选、多选、判断、填空四种题型，自动判分并保存记录。 |
| AI 知识助手 | 基于大模型的航海知识问答与出题助手，支持 AI 出题入库、实时天气查询、对话历史记录。 |
| AIS 知识库 | 管理 RAG 文档、外部知识、分块、向量化状态和检索测试。 |
| 统计报表 | 输出 AIS、船型、航线、观察活动等统计视图。 |
| 用户权限 | 登录、注册审核、角色权限、个人中心和审计日志。 |

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 4、Spring Security、Spring Actuator |
| 数据访问 | MyBatis（注解风格）、Flyway |
| 主数据数据库 | MySQL 8，保存用户、权限、船舶档案、RAG 元数据、业务配置等低频结构化数据 |
| AIS 明细数据库 | ClickHouse HTTP 接口，保存 AIS 明细、批量导入数据和高频轨迹记录 |
| 向量知识库 | Qdrant、Ollama `bge-m3` |
| AI 服务 | 阿里云百炼（Qwen-Plus，兼容 OpenAI 格式）/ DeepSeek Chat、IUCN API |
| 天气服务 | 百度地图天气 API（`https://api.map.baidu.com/weather/v1/`） |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Vue Router |
| UI 与可视化 | Element Plus、Leaflet、ECharts |
| 文档处理 | Apache PDFBox、Apache POI |

## 数据库选型

系统采用混合数据库架构：

| 数据类型 | 存储 | 原因 |
| --- | --- | --- |
| 船舶档案、用户、权限、审计、配置 | MySQL | 适合增删改查、唯一约束、事务、权限和版本回溯。 |
| AIS 明细、轨迹点、按时间范围统计 | ClickHouse | 适合大批量写入、列式存储、时间范围查询和聚合分析。 |
| RAG 文档分块向量 | Qdrant | 适合向量相似度检索，不用于结构化船舶主档。 |

## 项目结构

```text
ShipInsight_AI/
├─ README.md
├─ .gitignore
├─ system/
│  ├─ src/main/java/com/gsmv/
│  │  ├─ ais/                       # AIS + ClickHouse 接入、导入、查询、地图点位
│  │  ├─ ai/                        # 智能分析、RAG、复核、报告
│  │  ├─ auth/                      # 登录、注册、JWT
│  │  ├─ audit/                     # 审计日志
│  │  ├─ observation/               # 观察业务
│  │  ├─ quiz/                      # 知识问答 + AI 知识助手 + 天气服务
│  │  ├─ report/                    # 统计报表
│  │  ├─ vessel/                    # 船舶档案模块
│  │  ├─ user/                      # 用户与权限
│  │  └─ versioning/                # 数据版本回溯
│  ├─ src/main/resources/
│  │  ├─ application.yml
│  │  └─ db/migration/              # Flyway 迁移脚本
│  ├─ frontend/                     # Vue 前端
│  ├─ scripts/                      # 启动、实验和运维脚本
│  ├─ start-gsmv.cmd                # 一键启动入口
│  ├─ stop-gsmv.cmd
│  └─ pom.xml
├─ data/                            # 本地数据目录，不上传
├─ one mon/                         # 本地 AIS 原始数据目录，不上传
├─ train/                           # 本地训练数据目录，不上传
├─ clean/                           # 本地清洗数据目录，不上传
└─ clickhouse-data/                 # 本地 ClickHouse 数据卷，不上传
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

后端启动时会通过 Flyway 自动执行 `system/src/main/resources/db/migration` 下的迁移脚本，完成建表与初始数据写入。

> **注意**：部分表（如 `quiz_ai_chat_message`）因 Flyway 检查策略在已有数据库中未自动执行，需在首次启动前手动运行 `system/scripts/run_quiz_ai_sql.py`：
>
> ```powershell
> python system/scripts/run_quiz_ai_sql.py
> ```

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
# AI 对话（二选一，百炼兼容 OpenAI 格式，推荐）
setx BAILIAN_API_KEY "your-bailian-key"
# 或
setx DEEPSEEK_API_KEY "your-deepseek-key"

# 天气功能（态势总览天气模块 + AI 助手实时天气）
setx BAIDU_MAP_AK "your-baidu-map-ak"   /M   # /M 写入系统级，不加则写用户级

# 可选
setx IUCN_API_TOKEN "your-iucn-token"
```

也可以在启动时通过对话框输入，**密钥仅在本次启动生效，不会保存到磁盘**。

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

## AI 知识助手使用说明

### AK 获取方法

打开 `https://lbsyun.baidu.com/` 注册登录后，点击「控制台」→「应用管理」→「我的应用」→「创建应用」。**本系统为服务端调用**，创建应用时：
- 应用类型选择「服务端」
- 拿到 AK（API Key）后，通过以下任一方式配置：

### 使用步骤

1. 用户在注册百度地图账号后获取自己的 `BAIDU_MAP_AK`，启动 `start-gsmv.cmd` 填入，或自行设置环境变量。
2. 登录后点击左侧「知识问答」菜单，再点击「AI 知识助手」进入聊天页面。
3. 助手知识范围锁定在**船舶、天气、海域**三类，会拒绝无关话题。
4. **知识问答**：直接输入航海相关问题，AI 给出专业解答。
5. **天气查询**：输入含「天气 / 气温 / 风力」等关键词并包含城市名时，AI 自动获取百度实时天气数据后回答。
6. **AI 出题**：在「AI 出题入库」面板选择分类 / 题型 / 难度 / 数量，点击「生成并入库」，AI 出题后自动查重入库，已有题目跳过。
7. 对话历史保存在数据库，刷新页面后可继续查看，支持一键清空。

AI 知识助手后端 API：

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/api/v1/quiz/ai/chat` | 登录用户 | AI 对话（同步）。 |
| `POST` | `/api/v1/quiz/ai/chat/stream` | 登录用户 | AI 对话（SSE 流式）。 |
| `GET` | `/api/v1/quiz/ai/messages` | 登录用户 | 查询对话历史。 |
| `DELETE` | `/api/v1/quiz/ai/messages` | 登录用户 | 清空对话历史。 |
| `POST` | `/api/v1/quiz/ai/generate` | 登录用户 | AI 出题并入库。 |
| `GET` | `/api/v1/quiz/ai/weather/interpret` | 无需登录 | 获取指定城市天气及出海建议（供态势总览调用）。 |
| `GET` | `/api/v1/quiz/ai/weather/test` | 无需登录 | 天气链路调试接口。 |

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

## 船舶档案

v1.1 将船舶档案独立为 `vessel` 模块。核心字段包括：

- 船名、MMSI、IMO、呼号、船型、船旗、运营方、所有方。
- 长宽、吃水、总吨、载重吨。
- 风险等级、航行状态、常用区域、航线范围。
- 备注、资料来源、启用/归档状态、创建时间、更新时间。
- 船舶图片和资料文件，业务类型为 `VESSEL_IMAGE`。

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

以下目录是本地数据、训练数据、清洗结果或数据库运行数据，不进入 Git：

- `data/`、`one mon/`、`train/`、`clean/`、`clean1/`、`clean2/`、`clickhouse-data/`

其他本地运行文件和构建产物同样不提交：

- `.idea/`、`.vscode/`
- `system/target/`、`system/frontend/node_modules/`、`system/frontend/dist/`
- `system/.gsmv-runtime/`
- `system/uploads/**`
- `*.log`、`.env`、`.env.*`

提交前建议检查：

```powershell
git status --short
```

## 版本记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| `v1.1.2` | 2026-06-24 | 新增 AI 知识助手（大模型出题/问答/实时天气）；态势总览新增天气模块；集成百度地图天气 API；启动脚本支持 BAIDU_MAP_AK 配置；统一知识问答页面样式；修复学生/公众权限。 |
| `v1.1.1` | 2026-06-22 | 新增知识问答模块（239 道题目，四种题型）；修复填空题样式；更名航线地图路径。 |
| `v1.1` | 2026-06-20 | 重构船舶档案模块；新增 MarineTraffic 风格地图、单船轨迹、数据集日期筛选、AIS 全量导入和导入进度；移除前端航运网络入口。 |
| `v1.0` | 2026-06-17 | 初始化 ShipInsight AI 本地仓库；补充 AIS + ClickHouse 记录导入、查询、批量修改和删除能力。 |
