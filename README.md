# ShipInsight AI v1.3

ShipInsight AI 是一套面向 AIS 船舶交通数据的本地化智能管理系统。系统围绕船舶档案、AIS 记录、海上交通图、航线地图、智能分析、RAG 知识库、知识问答、统计报表、用户权限和审计日志，提供从数据接入、态势查看、风险分析到权限治理的一体化工作台。

当前版本：`v1.3`

## v1.3 更新重点

- 新增角色管理模块，仅 `ADMIN` 系统管理员角色可见和可访问，支持角色分页查询、创建、编辑、删除和权限分配。
- 修复普通用户进入系统后反复弹出“没有权限访问该资源”的问题，前端菜单、路由守卫和页面数据加载按用户权限降级展示。
- 优化用户权限体系，确保管理员账号拥有 `ADMIN` 角色和 `USER_ADMIN` 权限，角色管理与用户管理入口保持一致。
- 优化系统主界面、船舶档案、AIS 记录、航线地图、态势总览等页面视觉风格和交互体验。
- 新增 AIS 示例数据迁移脚本，便于空库初始化后直接体验船舶档案、人工 AIS 记录、航运区域和地图模块。
- 重写 Flyway 一键迁移脚本，支持三类数据库状态：
  - 空 MySQL 且没有 `gsmv` 库：自动创建数据库并从 `V1` 开始建表。
  - 已有 `gsmv` 且有 Flyway 历史：只补跑未执行的新迁移。
  - 已有表但没有 Flyway 历史：自动清空 schema 后按迁移脚本重建。
- 优化一键启动脚本的 API Key 注入逻辑，支持启动时填写百炼、DeepSeek、百度地图等密钥，也兼容系统环境变量。

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 态势总览 | 展示船舶、AIS、航运区域、风险、天气和关键统计指标。 |
| 船舶档案 | 管理船名、MMSI、IMO、呼号、船型、船旗、运营方、风险等级、运行状态和版本历史。 |
| AIS 记录 | 基于 ClickHouse 保存 AIS 明细，支持导入、分页查询、地图快照、轨迹查询和批量操作。 |
| 海上交通图 | MarineTraffic 风格地图，展示最新船位、船舶详情、MMSI/船名搜索和单船历史轨迹。 |
| 航线地图 | 基于 Leaflet 展示 AIS 点位、航线轨迹和空间分布。 |
| 人工 AIS 记录 | 手动录入和维护 AIS 观察记录，支持关联船舶档案和版本回溯。 |
| 航运区域 | 管理港口、航道、锚地、近海水域等区域信息。 |
| 智能分析 | 集成大模型、RAG、业务数据和对话历史，提供自然语言分析能力。 |
| 分析报告 | 生成、查看和导出 AI 辅助分析报告。 |
| 异常复核 | 对低置信度或疑似异常的 AI 结果进行人工复核和工单流转。 |
| AIS 知识库 | 管理知识来源、文档、分块、索引任务和检索测试。 |
| 知识问答 | 支持题库练习、随机练习、自动判分、答题记录和 AI 出题入库。 |
| 统计报表 | 输出风险、运营状态、船型、AIS 趋势和航运区域统计，支持 Excel/PDF 导出。 |
| 用户管理 | 管理用户、注册审核、账号状态、角色分配和个人资料。 |
| 角色管理 | 仅系统管理员可见，维护角色和权限绑定。 |
| 审计日志 | 记录关键操作，支持系统运行和权限审计。 |

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3.5、TypeScript 6、Vite 8、Pinia 3、Vue Router 5 |
| UI 与可视化 | Element Plus 2.13、Leaflet 1.9、ECharts 6 |
| 后端 | Java 17、Spring Boot 4.0.5、Spring Security、Spring Actuator |
| 数据访问 | MyBatis 4.0、Flyway |
| 主数据库 | MySQL 8，用于用户、权限、船舶档案、航运区域、题库、RAG 元数据和审计数据 |
| AIS 明细库 | ClickHouse HTTP 接口，用于 AIS 明细、轨迹点和高频聚合查询 |
| 向量库 | Qdrant + Ollama `bge-m3`，默认 1024 维 |
| AI 服务 | 阿里云百炼兼容 OpenAI 格式，也支持 DeepSeek 配置 |
| 天气服务 | 百度地图天气 API |
| 文档导出 | Apache PDFBox、Apache POI |
| 压缩处理 | zstd-jni |

## 项目结构

```text
ShipInsight_AI/
|-- README.md
|-- .gitignore
|-- scripts/                         # 根目录辅助脚本
|-- system/
|   |-- pom.xml                       # 后端 Maven 配置
|   |-- start-gsmv.cmd                # 一键启动脚本
|   |-- stop-gsmv.cmd                 # 停止脚本
|   |-- scripts/
|   |   |-- setup-flyway.cmd          # Flyway 一键建库/迁移脚本
|   |   |-- start-clickhouse.cmd      # ClickHouse 启动脚本
|   |   |-- start-gsmv.ps1            # 一键启动核心逻辑
|   |   `-- gsmv-api-key-prompt.ps1   # API Key 输入窗口
|   |-- src/main/java/com/gsmv/
|   |   |-- ais/                      # AIS 导入、查询、地图、轨迹和统计
|   |   |-- ai/                       # AI 助手、RAG、AI 报告、复核工单
|   |   |-- auth/                     # 登录、注册、验证码、认证
|   |   |-- audit/                    # 审计日志
|   |   |-- bootstrap/                # 启动初始化与迁移辅助
|   |   |-- config/                   # 安全、JWT、存储等配置
|   |   |-- ecosystem/                # 航运区域
|   |   |-- observation/              # 人工 AIS 记录
|   |   |-- quiz/                     # 知识问答、AI 出题、天气解读
|   |   |-- report/                   # 统计报表与导出
|   |   |-- user/                     # 用户、角色和权限
|   |   |-- vessel/                   # 船舶档案
|   |   |-- versioning/               # 实体版本历史
|   |   `-- media/                    # 媒体文件
|   |-- src/main/resources/
|   |   |-- application.yml
|   |   `-- db/migration/             # Flyway 迁移脚本，当前到 V30
|   |-- src/test/java/com/gsmv/       # 后端测试
|   `-- frontend/                     # Vue 前端
|       |-- package.json
|       |-- vite.config.ts
|       `-- src/
|           |-- api/
|           |-- components/
|           |-- layouts/
|           |-- router/
|           |-- stores/
|           |-- types/
|           |-- utils/
|           `-- views/
|-- data/                             # 本地数据，默认不入库
|-- clean*/                           # 清洗输出，默认不入库
|-- handle_DATA*/                     # 本地处理数据，默认不入库
`-- clickhouse-data/                  # ClickHouse 本地数据卷，默认不入库
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

## 数据库初始化

### 推荐方式：一键 Flyway 脚本

```powershell
cd D:\ShipInsight_AI\system
.\scripts\setup-flyway.cmd
```

该脚本会自动检测 Java、MySQL 和 Maven Wrapper，并执行 Flyway 迁移。默认数据库配置：

| 配置 | 默认值 |
| --- | --- |
| Host | `localhost` |
| Port | `3306` |
| Database | `gsmv` |
| Username | `root` |
| Password | `123456` |

脚本对不同数据库状态的处理：

| 数据库状态 | 处理方式 |
| --- | --- |
| MySQL 已启动，但没有 `gsmv` 库 | 使用 `createDatabaseIfNotExist=true` 自动建库并执行全部迁移。 |
| `gsmv` 已存在，且有 `flyway_schema_history` | 执行 `flyway:migrate`，只补跑新版本迁移。 |
| `gsmv` 已有业务表，但没有 Flyway 历史 | 自动执行 `flyway:clean flyway:migrate`，清空 schema 后重建。 |

注意：第三种情况会清空 `gsmv` 中已有对象，这是为了把无历史的旧库重建为标准 Flyway 管理库。

### 手动建库

也可以先手动创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS gsmv
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

然后启动后端，系统会自动执行 `system/src/main/resources/db/migration` 下的迁移脚本。

## 数据服务

### MySQL

默认配置位于 `system/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gsmv?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456
```

### ClickHouse

默认 ClickHouse 配置：

```yaml
gsmv:
  clickhouse:
    url: http://localhost:8123
    database: shipinsight
    username: default
    password: 123456
```

启动方式：

```powershell
cd D:\ShipInsight_AI\system
.\scripts\start-clickhouse.cmd
```

连接检查：

```powershell
Invoke-RestMethod "http://localhost:8123/?query=SELECT%201"
```

### Qdrant 和 Ollama

```powershell
docker run -d --name gsmv-qdrant -p 6333:6333 qdrant/qdrant
ollama pull bge-m3
ollama serve
```

默认 RAG 配置：

| 配置项 | 默认值 |
| --- | --- |
| Qdrant URL | `http://localhost:6333` |
| Collection | `gsmv_rag_chunks` |
| Embedding 模型 | `bge-m3` |
| Embedding 维度 | `1024` |

## API Key 配置

建议通过环境变量注入密钥，不要提交真实密钥：

```powershell
setx BAILIAN_API_KEY "your-bailian-key"
setx DASHSCOPE_API_KEY "your-dashscope-key"
setx DEEPSEEK_API_KEY "your-deepseek-key"
setx BAIDU_MAP_AK "your-baidu-map-ak"
```

一键启动脚本也会弹出 API Key 输入窗口。窗口中输入的密钥只在本次启动进程内生效，不会写入磁盘。

| 环境变量 | 说明 |
| --- | --- |
| `BAILIAN_API_KEY` | 阿里云百炼 API Key，兼容 OpenAI 格式 |
| `DASHSCOPE_API_KEY` | 阿里云 DashScope API Key，百炼兼容字段 |
| `DEEPSEEK_API_KEY` | DeepSeek API Key |
| `DEEPSEEK_BASE_URL` | DeepSeek 或兼容 OpenAI 接口地址 |
| `DEEPSEEK_CHAT_MODEL` | 对话模型名称 |
| `BAIDU_MAP_AK` | 百度地图 AK，用于天气查询 |
| `QDRANT_URL` | Qdrant 服务地址 |
| `OLLAMA_BASE_URL` | Ollama 服务地址 |
| `CLICKHOUSE_URL` | ClickHouse HTTP 地址 |

## 启动系统

### 一键启动

```powershell
cd D:\ShipInsight_AI\system
.\start-gsmv.cmd
```

默认访问地址：

| 服务 | 地址 |
| --- | --- |
| 前端 | `http://localhost:5173` |
| 后端 | `http://localhost:8080` |
| ClickHouse HTTP | `http://localhost:8123` |
| Qdrant | `http://localhost:6333` |

默认管理员账号：

| 账号 | 密码 |
| --- | --- |
| `admin` | `123456` |

停止系统：

```powershell
cd D:\ShipInsight_AI\system
.\stop-gsmv.cmd
```

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

## 权限说明

系统使用角色和权限组合控制菜单、路由和接口访问。

| 角色/权限 | 说明 |
| --- | --- |
| `ADMIN` | 系统管理员角色，默认拥有所有页面访问能力，并且唯一可见角色管理模块。 |
| `USER_ADMIN` | 用户管理权限，可访问用户管理相关能力。 |
| `VESSEL_READ` | 查看船舶档案。 |
| `OBS_READ` | 查看 AIS 记录、航线地图和海上交通图。 |
| `REPORT_READ` | 查看态势总览、分析报告和统计报表。 |
| `RAG_READ` | 查看 AIS 知识库。 |
| `QUIZ_READ` / `QUIZ_WRITE` | 查看或维护知识问答题库。 |
| `AUDIT_READ` | 查看审计日志。 |

前端会根据当前用户的角色和权限动态隐藏不可访问菜单；后端接口仍会进行权限校验。

## 构建与验证

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

Flyway 状态检查：

```powershell
cd D:\ShipInsight_AI\system
.\mvnw.cmd flyway:info -DskipTests
```

## 核心 API 概览

### 船舶档案

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/vessels` | 分页查询船舶 |
| `GET` | `/api/v1/vessels/{id}` | 查看船舶详情 |
| `POST` | `/api/v1/vessels` | 新增船舶 |
| `PUT` | `/api/v1/vessels/{id}` | 修改船舶 |
| `DELETE` | `/api/v1/vessels/{id}` | 归档船舶 |
| `GET` | `/api/v1/vessels/types` | 查询船型选项 |
| `GET` | `/api/v1/vessels/{id}/versions` | 查询版本历史 |
| `POST` | `/api/v1/vessels/{id}/versions/{versionId}/rollback` | 回滚到指定版本 |

### AIS 记录

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/ais-records` | 分页查询 AIS 明细 |
| `GET` | `/api/v1/ais-records/map` | 查询地图最新船位 |
| `GET` | `/api/v1/ais-records/dataset-dates` | 查询可选数据集日期 |
| `GET` | `/api/v1/ais-records/{mmsi}/track` | 查询单船轨迹 |
| `POST` | `/api/v1/ais-records/import` | 导入 AIS 文件 |
| `GET` | `/api/v1/ais-records/import/progress/{taskId}` | 查询导入进度 |
| `PATCH` | `/api/v1/ais-records/batch` | 批量更新 AIS 记录 |
| `DELETE` | `/api/v1/ais-records/batch` | 批量删除 AIS 记录 |
| `GET` | `/api/v1/ais-records/risk-summary` | 查询风险摘要 |
| `GET` | `/api/v1/ais-records/importer-ranking` | 查询导入者排行 |

### AI 与 RAG

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/ai/assistant/chat` | AI 助手对话 |
| `POST` | `/api/v1/ai/assistant/chat/stream` | SSE 流式对话 |
| `GET` | `/api/v1/ai/assistant/messages` | 查询助手历史消息 |
| `GET` | `/api/v1/ai/rag/sources` | 查询 RAG 来源 |
| `POST` | `/api/v1/ai/rag/documents` | 新增或导入文档 |
| `POST` | `/api/v1/ai/rag/search` | 检索测试 |
| `GET` | `/api/v1/ai/reports` | 查询 AI 报告 |
| `POST` | `/api/v1/ai/reports/generate` | 生成 AI 报告 |
| `GET` | `/api/v1/ai/review-tickets` | 查询复核工单 |

### 用户、角色和审计

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/auth/login` | 登录 |
| `POST` | `/api/v1/auth/register` | 注册 |
| `GET` | `/api/v1/users` | 查询用户 |
| `GET` | `/api/v1/roles` | 查询角色，仅管理员 |
| `GET` | `/api/v1/roles/permissions` | 查询权限选项，仅管理员 |
| `POST` | `/api/v1/roles` | 创建角色，仅管理员 |
| `PUT` | `/api/v1/roles/{id}` | 更新角色，仅管理员 |
| `DELETE` | `/api/v1/roles/{id}` | 删除角色，仅管理员 |
| `GET` | `/api/v1/audits` | 查询审计日志 |

### 知识问答和报表

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/quiz/questions` | 查询题目 |
| `POST` | `/api/v1/quiz/questions` | 新增题目 |
| `POST` | `/api/v1/quiz/submit` | 提交答案并判分 |
| `GET` | `/api/v1/quiz/records` | 查询答题记录 |
| `POST` | `/api/v1/quiz/ai/chat` | AI 知识助手对话 |
| `POST` | `/api/v1/quiz/ai/generate` | AI 出题入库 |
| `GET` | `/api/v1/reports/summary` | 仪表盘摘要 |
| `GET` | `/api/v1/reports/export/excel` | 导出 Excel |
| `GET` | `/api/v1/reports/export/pdf` | 导出 PDF |

## AIS 数据使用说明

1. 启动 MySQL、ClickHouse 和后端服务。
2. 打开前端 `http://localhost:5173`，使用管理员账号登录。
3. 进入 AIS 记录页面，导入 `.csv`、`.gz`、`.tgz`、`.zst` 等格式的数据文件。
4. 导入时可选择只导入前 N 条，也可以全量导入，并在页面查看实时进度。
5. 导入后可按关键字、时间范围、数据集日期查询记录，也可进入海上交通图查看最新船位和轨迹。
6. 删除 ClickHouse 数据使用轻量删除和后台合并机制，查询结果会及时变化，磁盘空间释放可能延迟。

## GPX 转 AIS CSV

仓库提供 GPX 轨迹转 AIS CSV 的辅助脚本，便于把移动端或 GPS 轨迹快速导入系统：

```powershell
python D:\ShipInsight_AI\scripts\gpx_to_ais_csv.py D:\ShipInsight_AI\handle_DATA\20260624.gpx `
  --mmsi 413000001 `
  --vessel-name DemoVessel
```

前端也内置了 GPX 转换工具函数，可在 AIS 导入流程中生成可导入 CSV。

## 仓库约定

以下目录属于本地数据、训练数据、清洗结果或运行数据，不进入 Git：

- `data/`
- `one mon/`
- `train/`
- `clean/`、`clean1/`、`clean2/`
- `handle_DATA/`、`handle_DATA_clean/`
- `clickhouse-data/`
- `.workbuddy/`

以下文件和目录也不应提交：

- `.codex_deps/`、`.idea/`、`.vscode/`
- `system/target/`
- `system/frontend/node_modules/`
- `system/frontend/dist/`
- `system/.gsmv-runtime/`
- `system/uploads/**`，保留 `system/uploads/.gitkeep`
- `*.log`、`.env`、`.env.*`

提交前建议检查：

```powershell
git status --short
```

## 版本记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| `v1.3` | 2026-06-29 | 新增角色管理模块、修复普通用户权限弹窗、优化系统界面和权限路由、补充 AIS 示例数据、增强 Flyway 一键建库/迁移脚本，并重写 README。 |
| `v1.2` | 2026-06-26 | AIS 主题重构、V28 表迁移、AIS 高级统计、海上交通图升级、AI 报告适配、权限体系调整和 README 重写。 |
| `v1.1.2` | 2026-06-24 | 新增 AI 知识助手、天气模块、百度地图天气 API、AI 出题和知识问答样式优化。 |
| `v1.1.1` | 2026-06-22 | 新增知识问答模块、题库练习、填空题样式修复和航线地图路径调整。 |
| `v1.1` | 2026-06-20 | 新增船舶档案、海上交通图、单船轨迹、AIS 导入和导入进度。 |
| `v1.0` | 2026-06-17 | 初始化 ShipInsight AI 本地仓库，提供 AIS + ClickHouse 导入、查询、批量修改和删除能力。 |
