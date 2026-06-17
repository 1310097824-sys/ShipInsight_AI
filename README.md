# ShipInsight AI

ShipInsight AI 是一个面向 AIS 船舶记录、航运态势、风险复核和智能分析的全栈系统。系统后端基于 Spring Boot，前端基于 Vue 3 和 Element Plus，业务数据库使用 MySQL，AIS 明细记录使用 ClickHouse 存储，支持本地 AIS 数据文件导入、分页查询、条件检索、详情查看，以及按选择结果或查询结果进行批量修改和删除。

当前版本：`v1.0`

## 功能概览

| 模块 | 说明 |
| --- | --- |
| 态势总览 | 展示港口、航线、热区、AIS 动态和关键统计指标。 |
| 船舶档案 | 管理船舶/目标档案、分类信息、媒体资料和历史版本。 |
| 航线地图 | 基于 Leaflet 展示观测点、航线和航运网络空间分布。 |
| 航运网络 | 维护港口、航线、生态/区域网络等基础资料。 |
| AIS 记录 | 基于 ClickHouse 保存 AIS 明细，支持本地文件导入、关键词/时间查询、分页、详情、批量修改和批量删除。 |
| 异常复核 | 对低置信度或疑似异常的 AI 分析结果进行人工复核。 |
| 智能分析 | 集成 DeepSeek、RAG、系统业务数据和对话历史，提供自然语言分析能力。 |
| 分析报告 | 生成、查看和导出 AI 辅助分析报告。 |
| AIS 知识库 | 管理 RAG 文档、外部知识、分块、向量化状态和检索测试。 |
| 统计报表 | 输出 AIS、船型、航线、观测活动等统计视图。 |
| 用户权限 | 登录、注册审核、角色权限、个人中心和审计日志。 |

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 4、Spring Security、Spring Actuator |
| 数据访问 | MyBatis、Flyway |
| 业务数据库 | MySQL 8 |
| AIS 明细库 | ClickHouse HTTP 接口 |
| 向量知识库 | Qdrant、Ollama `bge-m3` |
| AI 服务 | DeepSeek Chat、DashScope/百炼视觉模型、IUCN API |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Vue Router |
| UI 与可视化 | Element Plus、Leaflet、ECharts |
| 文档处理 | Apache PDFBox、Apache POI |

## 项目结构

```text
ShipInsight_AI/
├─ README.md
├─ .gitignore
├─ scripts/                  # 根目录辅助脚本
├─ system/
│  ├─ src/main/java/com/gsmv/
│  │  ├─ ais/                # AIS + ClickHouse 接入
│  │  ├─ ai/                 # 智能分析、RAG、复核、报告
│  │  ├─ auth/               # 登录、注册、JWT
│  │  ├─ audit/              # 审计日志
│  │  ├─ ecosystem/          # 航运网络/区域资料
│  │  ├─ observation/        # 观测业务
│  │  ├─ report/             # 统计报表
│  │  ├─ species/            # 船舶/目标档案
│  │  ├─ user/               # 用户与权限
│  │  └─ versioning/         # 数据版本回溯
│  ├─ src/main/resources/
│  │  ├─ application.yml
│  │  └─ db/migration/       # Flyway 迁移脚本
│  ├─ frontend/              # Vue 前端
│  ├─ scripts/               # 启动、实验和运维脚本
│  ├─ start-gsmv.cmd
│  ├─ stop-gsmv.cmd
│  └─ pom.xml
├─ data/                     # 本地数据目录，不上传
├─ one mon/                  # 本地 AIS 数据目录，不上传
├─ train/                    # 本地训练数据目录，不上传
├─ clean/                    # 本地清洗数据目录，不上传
└─ clickhouse-data/          # 本地 ClickHouse 数据卷，不上传
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

后端启动时会通过 Flyway 自动执行 `system/src/main/resources/db/migration` 下的迁移脚本。

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
  clickhouse/clickhouse-server:latest
```

检查连接：

```powershell
Invoke-RestMethod "http://localhost:8123/?query=SELECT%201"
```

## AI 与 RAG 配置

建议通过环境变量注入 Key，不要把真实密钥提交到仓库。

```powershell
setx BAILIAN_API_KEY "your-bailian-key"
setx DASHSCOPE_API_KEY "your-bailian-key"
setx DEEPSEEK_API_KEY "your-deepseek-key"
setx IUCN_API_TOKEN "your-iucn-token"
```

本地 embedding 模型：

```powershell
ollama pull bge-m3
ollama serve
```

Qdrant：

```powershell
docker run -d --name gsmv-qdrant -p 6333:6333 qdrant/qdrant
```

默认 RAG 配置：

```text
Qdrant URL: http://localhost:6333
Collection: gsmv_rag_chunks
Embedding model: bge-m3
Embedding dimension: 1024
```

## 启动系统

### 一键启动

```powershell
cd D:\ShipInsight_AI\system
.\start-gsmv.cmd
```

一键脚本会启动后端、前端，并尝试准备必要的本地服务。停止系统：

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

## AIS 记录使用说明

1. 打开前端 `http://localhost:5173`，登录后进入 `AIS 记录` 页面。
2. 选择本地 AIS 数据文件导入，系统会按页面设置的条数限制读取样例记录，默认用于快速导入前 10 条。
3. 支持按关键词、观测时间范围查询 ClickHouse 中的 AIS 记录。
4. 表格支持分页、勾选、详情查看。
5. 批量操作支持：
   - 对已勾选记录批量修改备注等字段。
   - 对当前查询结果批量修改。
   - 对已勾选记录批量删除。
   - 对当前查询结果批量删除。

后端 API：

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/ais-records` | `OBS_READ` | 查询 AIS 记录 |
| `POST` | `/api/v1/ais-records/import` | `OBS_WRITE` | 导入本地 AIS 文件 |
| `PATCH` | `/api/v1/ais-records/batch` | `OBS_WRITE` | 批量修改 AIS 记录 |
| `DELETE` | `/api/v1/ais-records/batch` | `OBS_WRITE` | 批量删除 AIS 记录 |

## 构建与检查

前端构建：

```powershell
cd D:\ShipInsight_AI\system\frontend
npm.cmd run build
```

后端测试：

```powershell
cd D:\ShipInsight_AI\system
.\mvnw.cmd test
```

后端编译：

```powershell
cd D:\ShipInsight_AI\system
.\mvnw.cmd -DskipTests compile
```

## 版本记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| `v1.0` | 2026-06-17 | 初始化 ShipInsight AI 本地仓库；补充 AIS + ClickHouse 记录导入、查询、批量修改和删除能力；整理 README 与忽略规则。 |

## 仓库约定

以下目录是本地数据、训练数据、清洗结果或数据库运行数据，不进入 Git：

- `data/`
- `one mon/`
- `train/`
- `clean/`
- `clickhouse-data/`

提交前建议运行：

```powershell
git status --short
git check-ignore -v "data" "one mon" "train" "clean" "clickhouse-data"
```
