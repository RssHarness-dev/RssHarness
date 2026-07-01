[English Version](../README.md) | **中文版本**

---

# RssHarness — RSS 赋能的精准搜索代理

**RSS 赋能的精准搜索代理** / *RSS-powered precise search agent*

> 不依赖全网倒排索引。通过 RSSHub 的结构化路由命名空间（site/channel/keyword），对指定网站频道进行实时定点抓取。每个结果锚定一个可溯源 URL——解决 LLM 生成内容不可查证的固有问题。
>
> *No inverted index. No web crawler. RSSHub's structured route namespace replaces traditional search indexing — enabling real-time, targeted retrieval from specific website channels. Every result links to a verifiable source URL.*

---

## 🏗️ 架构 / Architecture

```
CLI (CliRunner)                     ← conversational REPL, /sync, /routes, /new
  │
AI Domain (ai/)                      ← LLM-driven tool orchestration
  ├─ ConversationService             ← single ChatClient call, LLM decides flow
  │    ├─ RssTools                   ← @Tool: searchPlatforms, listRoutes, fetchRss, readSummaries
  │    ├─ RouteCatalog               ← in-memory route index, loaded from local JSON
  │    └─ RouteSyncTask              ← DOM+XPath sync from RSSHub
  │
RSS Domain (rss/)                    ← MVC pipeline
  ├─ RssController                   ← internal bean entry
  ├─ RouteFetchService               ← async allOf fan-out
  │    ├─ ArticlesFetchService       ← @Async + atomic tryMarkRefresh (CAS)
  │    ├─ RssFetcher                 ← multi-instance failover + URL encoding
  │    │    └─ RssInstanceManager    ← sliding-window health scoring
  │    ├─ AiSummaryService           ← per-article LLM summary (@Lazy)
  │    └─ SummaryStorageService      ← adapter → storage domain
  │
Storage Domain (storage/)            ← DDD
  ├─ DataRoot                        ← EclipseStore aggregate root
  ├─ DataViewFactory                 ← fromRoute() factory
  └─ SummaryView                     ← CQS read/write view
```

---

## 🛠️ 技术栈 / Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 4.1.0 |
| AI | Spring AI 2.0.0 + DeepSeek Chat |
| RSS Parsing | Rome 2.1.0 |
| HTTP Client | Apache HttpClient 5.6.1 |
| Persistence | EclipseStore 4.1.0 (embedded object graph) |
| JSON | Jackson 2.22.0 |
| Logging | SLF4J + Logback |
| Testing | JUnit 5 + Mockito + AssertJ |

---

## ✨ 核心特性 / Features

### 智能路由发现 / Intelligent Route Discovery
- **LLM 自主编排** — 一次 ChatClient 调用，LLM 决定 searchPlatforms → listRoutes → fetchRss → readSummaries 全流程
- **Tool Calling** — 四个 @Tool 方法，Spring AI 自动处理 function calling 循环
- **DOM+XPath 路由同步** — 直接从 RSS XML 提取 guid，避免 Rome UUID 处理污染
- **本地路由缓存** — `/sync` 一次，持久化到 `data/routes.json`，启动即加载

*Single ChatClient call — LLM autonomously drives the entire pipeline via @Tool methods. Route catalog synced via raw XML parsing, persisted locally.*

### 多实例容错 / Multi-Instance Failover
- **滑动窗口排序** — 最近 10 次成功率决定实例优先级，主实例失败自动切换下一个
- **中文 URL 编码** — 路径中的中文参数自动百分号编码

*Sliding-window health scoring. Chinese path segments auto-encoded for HTTP.*

### 异步全链路 / Fully Async Pipeline
- `@Async` + `CompletableFuture.allOf` 扇出模式，N 个路由并发处理
- `tryMarkRefresh` 原子 CAS，消除并发竞态

*N routes processed concurrently via allOf fan-out. Atomic tryMarkRefresh eliminates race conditions.*

### 可溯源 AI 摘要 / Traceable AI Summaries
- DeepSeek Chat 生成结构化摘要（JSON: index, title, summary, score）
- 降级保护：AI 调用失败自动回退到占位摘要，不丢失抓取数据
- 每条约会保留原文 link + publisher + publishTime

*Fallback protection ensures fetched data is never lost. Every summary retains the original article URL for traceability.*

### CLI 交互 / CLI REPL
- 流式打字机效果 — LLM 输出逐 token 实时显示，三级颜色（灰=思考，青=工具，白=回复）
- 多轮对话 — ChatMemory 自动保存会话历史，`/new` 清空上下文
- 进度反馈 — 每个路由抓取结果独立显示，成功/失败/冷却分别标识
- `/<command>` 风格命令：`/sync` `/routes` `/new` `/help` `/exit`

*Streaming typewriter with 3-color levels. Multi-turn via Spring AI ChatMemory. Per-route fetch status display.*

---

## 🚀 快速开始 / Quick Start

### 前置条件 / Prerequisites
- JDK 21+
- Maven 3.9+
- RSSHub instance (default: `http://127.0.0.1:1200`)
- DeepSeek API key

### 构建 / Build
```bash
git clone <repo-url>
cd rssharness

# Set DeepSeek API key (or edit application.properties)
export DEEPSEEK_API_KEY=sk-your-key-here

# Build and test
./mvnw clean package
```

### Docker 部署 / Docker
```bash
# Build and run with docker-compose (includes RSSHub)
export DEEPSEEK_API_KEY=sk-your-key
docker-compose up -d

# Or pull pre-built image
docker pull makeiny/rss-harness:latest
docker run -e DEEPSEEK_API_KEY=sk-your-key makeiny/rss-harness:latest
```

### 本地运行 / Local Run
```bash
./mvnw package -DskipTests
java -jar target/rssharness-0.0.1-SNAPSHOT.jar
```

### CLI 命令 / CLI Commands
```
RssHarness> 最近AI领域有什么新进展？       ← ask any question
RssHarness> What's new in WebGPU?
RssHarness> /sync                          ← refresh route catalog
RssHarness> /routes                        ← list platforms
RssHarness> /routes github                 ← list routes for a platform
RssHarness> /new                           ← start new session
RssHarness> /help
RssHarness> /exit
```

---

## ⚙️ 配置 / Configuration

### 生产配置 `application.properties` / Production

| Property | Default | Description |
|---|---|---|
| `spring.ai.deepseek.api-key` | `$DEEPSEEK_API_KEY` or fallback | DeepSeek API key |
| `rssharness.cli.enabled` | `true` | Enable CLI REPL |
| `rssharness.routes-url` | `http://127.0.0.1:1200/rsshub/routes/zh` | RSSHub routes RSS endpoint |
| `rssharness.routes-file` | `./data/routes.json` | Local route cache file |
| `rss.fetch-interval` | `60` | Cooldown between fetches (seconds) |
| `rss.config-path` | `config/rss-sources.json` | Route→URL mappings |
| `rss.instance-path` | `config/rss-instances.json` | RSSHub instances |
| `org.eclipse.store.storage-directory` | `./data/eclipse-store` | Embedded DB path |

### 测试配置 `application-test.properties` / Test Profile

测试 profile 自动通过以下机制隔离生产环境：
*Test profile isolates from production via:*

| Override | Value | Purpose |
|---|---|---|
| `rssharness.cli.enabled` | `false` | 阻止 CLI 无限循环 / prevents hang |
| `spring.ai.deepseek.api-key` | `test-key-placeholder` | 阻止真实 API 调用、mock 生效 / mock takes over |
| `rss.fetch-interval` | `0` | 无冷却期、全链路验证 / no cooldown, full pipeline |
| Mock ChatModel | `TestAiConfig` (via `spring.factories`) | 返回可解析的摘要 JSON / returns parseable JSON |

---

## 📂 项目结构 / Project Structure

```
rssharness/
├── pom.xml
├── README.md
├── PROGRESS.md
├── qodana.yaml
├── data/                              # EclipseStore data (runtime)
├── config/                            # RSS source & instance config files
├── src/
│   ├── main/java/com/fanexmp/rssharness/
│   │   ├── RssHarnessApplication.java    # @SpringBootApplication
│   │   ├── CliRunner.java             # Interactive REPL (streaming)
│   │   ├── dto/                       # Shared DTOs
│   │   │   ├── FetchResponse.java
│   │   │   ├── FetchStatus.java       # SUCCESS/INTERVAL/FAILED/SAVE_FAILED
│   │   │   └── Summary.java
│   │   ├── ai/                        # AI Domain
│   │   │   ├── AiConfig.java          # ChatClient + advisors
│   │   │   ├── dto/RouteEntry.java    # Route metadata record
│   │   │   ├── rag/
│   │   │   │   ├── RouteCatalog.java   # Route knowledge base
│   │   │   │   ├── RouteSyncTask.java  # Scheduled sync
│   │   │   │   └── SessionCache.java   # Per-query ephemeral
│   │   │   ├── tool/RssTools.java     # @Tool Function Calling
│   │   │   └── conversation/
│   │   │       ├── ContextManager.java        # Multi-turn + token budget
│   │   │       ├── ConversationService.java   # Tier1→Tier2 orchestrator
│   │   │       └── SearchCallback.java        # Streaming callback
│   │   ├── rss/                       # RSS Domain (MVC)
│   │   │   ├── RssController.java
│   │   │   ├── dto/                   # Article, Articles, RssInstance, RssSource
│   │   │   ├── exception/            # HttpReqException, RssConfigRepoException
│   │   │   ├── fetcher/              # RssFetcher, config repo, HTTP client
│   │   │   ├── parser/               # RssXmlParser interface + Rome impl
│   │   │   └── services/             # Async pipeline services
│   │   └── storage/                  # Storage Domain (DDD)
│   │       ├── DataRoot.java         # Aggregate root
│   │       ├── StorageConfig.java    # EmbeddedStorageManager bean
│   │       └── dataview/             # SummaryView + factory
│   ├── main/resources/
│   │   └── application.properties
│   └── test/                         # 53 tests (17 classes)
│       ├── java/com/fanexmp/rssharness/
│       │   ├── TestAiConfig.java     # Mock ChatModel
│       │   ├── ai/                   # RouteCatalog, ContextManager, ConversationService tests
│       │   ├── rss/                  # Controller, DTO, fetcher, parser, service tests
│       │   └── storage/              # SummaryView, StorageIntegration tests
│       └── resources/
│           ├── application-test.properties  # Central test config
│           ├── META-INF/spring.factories    # TestConfiguration registration
│           └── xml/                 # Test fixture XMLs
```

---

## 🧪 测试 / Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=RouteFetchServiceTest

# Run with test profile explicitly
./mvnw test -Dspring.profiles.active=test
```

**53 测试 / 53 tests — 0 failures**

| Domain | Test Class | Count | Type |
|---|---|---|---|
| DTO | `ArticlesTest`, `ArticleTest` | 6 | Unit |
| Parser | `RssXmlParserTest` | 2 | Unit |
| Config Repo | `RssConfigRepositoryTest` | 8 | Spring Integration |
| Instance Mgmt | `RssInstanceManagerTest` | 4 | Spring Integration |
| HTTP Fetch | `RssFetcherTest`, `RssFetcherIntegrationTest` | 4 | Unit + Integration |
| Services | `ArticlesFetchServiceTest`, `RouteFetchServiceTest` | 6 | Unit (Mockito) |
| Controller | `RssControllerIntegrationTest` | 4 | Full Pipeline |
| Storage | `SummaryViewTest`, `StorageIntegrationTest` | 6 | Unit + Integration |
| AI Routing | `RouteCatalogTest` | 5 | Unit |
| AI Conversation | `ContextManagerTest`, `ConversationServiceTest` | 7 | Unit (Mockito) |
| Application | `RssHarnessApplicationTests` | 1 | Spring Integration |

---

## 🎯 设计决策 / Design Decisions

| 决策 / Decision | 说明 / Rationale |
|---|---|
| **单次 ChatClient 调用** / Single-call | LLM 自主编排 searchPlatforms → listRoutes → fetchRss → readSummaries，不手动分 Tier |
| **DOM+XPath 路由解析** / Raw XML parsing | 绕过 Rome 的 guid 处理污染，直接从 RSS XML 提取路由路径 |
| **ChatMemory 多轮对话** / Multi-turn | Spring AI MessageChatMemoryAdvisor 自动管理会话历史 |
| **tryMarkRefresh 原子 CAS** | `ConcurrentHashMap.compute()` 合并 check+set，消除 @Async 环境下的竞态 |
| **滑动窗口排序** / Sliding window | `Deque<Boolean>` 最近 10 次，按成功率排序。避免取模导致的权重失真 |
| **@Lazy 打破循环** | AiSummaryService → ChatClient → RssTools → RssController → RouteFetchService 形成环 |
| **降级保护** / Graceful degradation | AI 摘要失败 → placeholder 摘要保留 URL+标题，核心数据不丢失 |

---

## 🗺️ 路线图 / Roadmap

- [x] RSS 域：多实例容错、异步管道、Rome 解析、EclipseStore 持久化
- [x] 存储域：DDD 聚合根、CQS 视图、EclipseStore 集成
- [x] AI 域：Spring AI 2.0 Tool Calling、路由目录 DOM+XPath 同步、ChatMemory 多轮对话
- [x] CLI REPL：流式打字机效果、三级颜色渲染、会话管理
- [x] Docker 部署：多阶段构建、docker-compose 编排、环境变量配置
- [ ] Web UI（可选）

---

## 📄 License

This project is created as a portfolio work. All rights reserved.
