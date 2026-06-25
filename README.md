# RSSGSE — RSS-powered Generative Search Engine

**RSS 赋能的精准搜索引擎** / *RSS-powered precise search engine*

> 不依赖全网倒排索引。通过 RSSHub 的结构化路由命名空间（site/channel/keyword），对指定网站频道进行实时定点抓取。每个结果锚定一个可溯源 URL——解决 LLM 生成内容不可查证的固有问题。
>
> *No inverted index. No web crawler. RSSHub's structured route namespace replaces traditional search indexing — enabling real-time, targeted retrieval from specific website channels. Every result links to a verifiable source URL.*

---

## 🏗️ 架构 / Architecture

```
CLI (CliRunner)                     ← /search, /routes, /help, /exit
  │
AI Domain (ai/)                      ← intelligent routing + conversation
  ├─ ConversationService             ← Tier1 (platforms) → Tier2 (routes) → fetch
  │    ├─ RouteCatalog               ← RSSHub /routes JSON → flat index
  │    ├─ RouteSyncTask              ← @Scheduled daily sync
  │    ├─ RssTools                   ← @Tool Function Calling endpoint
  │    ├─ SessionCache               ← per-query ephemeral cache
  │    └─ ContextManager             ← multi-turn + token budget trimming
  │
RSS Domain (rss/)                    ← MVC pipeline
  ├─ RssController                   ← internal bean entry
  ├─ RouteFetchService               ← async allOf fan-out
  │    ├─ ArticlesFetchService       ← @Async + atomic tryMarkRefresh (CAS)
  │    │    ├─ RssFetcher            ← multi-instance failover
  │    │    │    └─ RssInstanceManager ← sliding-window health scoring (last 10)
  │    │    │         └─ ApacheHttpReqWrapper ← 3s timeout
  │    │    └─ RssXmlParserV1        ← Rome RSS/Atom parser
  │    ├─ AiSummaryService           ← ChatClient AI summary (@Lazy)
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
- **二级 Function Calling** — Tier1：LLM 从 ~80 个平台中选最相关的 5 个；Tier2：对每个平台选择具体路由并填充参数（如 `/weibo/keyword/AI`）
- **RAG 路由知识库** — RSSHub `/routes` JSON 定时同步、扁平化、缓存
- **会话缓存** — 一次查询临时存储，生命期结束即弃

*Two-tier function calling — Tier1: LLM picks top 5 platforms from ~80; Tier2: selects routes and fills params per platform.*

### 多实例容错 / Multi-Instance Failover
- **滑动窗口排序** — 最近 10 次成功率决定实例优先级，主实例失败自动切换下一个
- **3 秒 HTTP 超时** — 串行 failover 不对公共 RSSHub 实例造成流量冲击

*Sliding-window health scoring (last 10 results) sorts instances by success rate. Serial failover avoids traffic spikes to public RSSHub instances.*

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
- 流式打字机效果 — LLM 输出逐 token 实时显示
- `/search` `/routes` `/help` `/exit` 命令

*Streaming typewriter effect: LLM tokens rendered in real-time via Flux.*

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
cd rssgse

# Set DeepSeek API key (or edit application.properties)
export DEEPSEEK_API_KEY=sk-your-key-here

# Build and test
./mvnw clean package
```

### 运行 / Run
```bash
# Launch CLI interactive mode
java -jar target/rssgse-0.0.1-SNAPSHOT.jar
```

### CLI 命令 / CLI Commands
```
rssgse> /search 最近AI领域有什么新进展
rssgse> /search What's new in WebGPU
rssgse> /routes                    # list all platforms
rssgse> /routes github             # list routes for a platform
rssgse> /help
rssgse> /exit
```

---

## ⚙️ 配置 / Configuration

### 生产配置 `application.properties` / Production

| Property | Default | Description |
|---|---|---|
| `spring.ai.deepseek.api-key` | `$DEEPSEEK_API_KEY` or fallback | DeepSeek API key |
| `rssgse.cli.enabled` | `true` | Enable CLI REPL |
| `rssgse.routes-url` | `http://127.0.0.1:1200/routes` | RSSHub routes JSON endpoint |
| `rss.fetch-interval` | `1800` | Cooldown between fetches (seconds) |
| `rss.config-path` | `config/rss-sources.json` | Route→URL mappings |
| `rss.instance-path` | `config/rss-instances.json` | RSSHub instances |
| `org.eclipse.store.storage-directory` | `./data/eclipse-store` | Embedded DB path |

### 测试配置 `application-test.properties` / Test Profile

测试 profile 自动通过以下机制隔离生产环境：
*Test profile isolates from production via:*

| Override | Value | Purpose |
|---|---|---|
| `rssgse.cli.enabled` | `false` | 阻止 CLI 无限循环 / prevents hang |
| `spring.ai.deepseek.api-key` | `test-key-placeholder` | 阻止真实 API 调用、mock 生效 / mock takes over |
| `rss.fetch-interval` | `0` | 无冷却期、全链路验证 / no cooldown, full pipeline |
| Mock ChatModel | `TestAiConfig` (via `spring.factories`) | 返回可解析的摘要 JSON / returns parseable JSON |

---

## 📂 项目结构 / Project Structure

```
rssgse/
├── pom.xml
├── README.md
├── PROGRESS.md
├── qodana.yaml
├── data/                              # EclipseStore data (runtime)
├── config/                            # RSS source & instance config files
├── src/
│   ├── main/java/com/fanexmp/rssgse/
│   │   ├── RssgseApplication.java     # @SpringBootApplication
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
│       ├── java/com/fanexmp/rssgse/
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
| Application | `RssgseApplicationTests` | 1 | Spring Integration |

---

## 🎯 设计决策 / Design Decisions

| 决策 / Decision | 说明 / Rationale |
|---|---|
| **二级 Function Calling** / Two-tier | 避免一次发送全量路由：Tier1 平台 (~80, ~3K tokens) → Tier2 路由 (每平台 ~10 条) |
| **SessionCache 一次性** / Disposable | 每次查询新建、查完即弃；路由表可能更新，不复用旧缓存 |
| **tryMarkRefresh 原子 CAS** | `ConcurrentHashMap.compute()` 合并 check+set，消除 @Async 环境下的竞态 |
| **滑动窗口排序** / Sliding window | `Deque<Boolean>` 最近 10 次，按成功率排序。避免取模导致的权重失真 |
| **串行 failover** / Serial failover | 不对公共 RSSHub 实例产生并发流量冲击，3s 超时保证可用性 |
| **@Lazy 打破循环** | AiSummaryService → ChatClient → RssTools → RssController → RouteFetchService 形成环 |
| **DDD vs MVC 边界** | RSS 域保持 MVC 管道模式，存储域 DDD。SummaryStorageService 是薄适配层 |
| **降级保护** / Graceful degradation | AI 摘要失败 → placeholder 摘要保留 URL+标题，核心数据不丢失 |

---

## 🗺️ 路线图 / Roadmap

- [x] RSS 域：多实例容错、异步管道、Rome 解析、EclipseStore 持久化
- [x] 存储域：DDD 聚合根、CQS 视图、EclipseStore 集成
- [x] AI 域：Spring AI 2.0、二级 Function Calling、路由目录同步、CLI REPL
- [x] 流式打字机效果：`SearchCallback` + `Flux<String>` token 级实时输出
- [ ] 多轮对话：ContextManager 接入 ConversationService，跨轮次上下文保留
- [ ] RSSHub `/routes` 实际 JSON 格式适配与端到端验证
- [ ] 对话导出 / 历史查询
- [ ] Web UI（可选）

---

## 📄 License

This project is created as a portfolio work. All rights reserved.
