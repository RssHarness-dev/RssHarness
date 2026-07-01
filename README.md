[中文版本 / Chinese Version](readme/README_CN.md) | **English**

---

# RssHarness — RSS-powered Generative Search Agent

**RSS 赋能的精准搜索代理** / *RSS-powered precise search agent*

> No inverted index. No web crawler. RSSHub's structured route namespace replaces traditional search indexing — enabling real-time, targeted retrieval from specific website channels. Every result links to a verifiable source URL. Built to solve the core problem of LLM-generated content: unverifiability.
>
> 不依赖全网倒排索引。通过 RSSHub 的结构化路由命名空间（site/channel/keyword），对指定网站频道进行实时定点抓取。每个结果锚定一个可溯源 URL。

---

## 🏗️ Architecture

```
CLI (CliRunner)                     ← conversational REPL, /sync, /routes, /new
  │
AI Domain (ai/)                      ← LLM-driven tool orchestration
  ├─ ConversationService             ← single ChatClient call, LLM decides flow
  │    ├─ RssTools                   ← @Tool: searchPlatforms, listRoutes, fetchRss, readSummaries
  │    ├─ RouteCatalog               ← in-memory route index, loaded from local JSON
  │    └─ RouteSyncTask              ← DOM+XPath sync from RSSHub RSS feed
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

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 4.1.0 |
| AI | Spring AI 2.0.0 + DeepSeek Chat |
| RSS Parsing | Rome 2.1.0 |
| HTTP Client | Apache HttpClient 5.6.1 |
| Persistence | EclipseStore 4.1.0 (embedded object graph) |
| JSON | Jackson 2.22.0 |
| Container | Docker + docker-compose |
| Testing | JUnit 5 + Mockito + AssertJ (53 tests) |

---

## ✨ Features

### Intelligent Route Discovery
- **LLM-Driven Orchestration** — single ChatClient call, LLM autonomously decides: searchPlatforms → listRoutes → fetchRss → readSummaries
- **Tool Calling** — four @Tool methods, Spring AI handles function-calling loop
- **DOM+XPath Route Sync** — raw RSS XML parsing avoids Rome guid corruption
- **Local Route Cache** — `/sync` once, persisted to `data/routes.json`, loaded on startup

### Multi-Instance Failover
- **Sliding-Window Health Scoring** — last 10 results determine instance priority, auto-failover
- **Chinese URL Encoding** — path segments with Chinese characters auto-encoded

### Fully Async Pipeline
- `@Async` + `CompletableFuture.allOf` fan-out — N routes processed concurrently
- `tryMarkRefresh` atomic CAS eliminates race conditions

### Traceable AI Summaries
- DeepSeek Chat generates structured summaries (JSON: index, summary, score)
- Graceful degradation: fallback summaries on AI failure — core data never lost
- Every summary retains original article link + publisher + publishTime

### CLI REPL
- Streaming typewriter effect with 3-color levels (gray=thinking, cyan=tool, white=response)
- Multi-turn via Spring AI ChatMemory, `/new` clears context
- Per-route fetch status display: ✓ SUCCESS / ✗ FAILED / ⏳ COOL

---

## 🚀 Quick Start

### Prerequisites
- JDK 21+, Maven 3.9+
- RSSHub instance
- DeepSeek API key

### Docker (recommended)
```bash
# Full stack: RssHarness + RSSHub
export DEEPSEEK_API_KEY=sk-your-key
docker-compose up -d

# Or pull pre-built image
docker pull makeiny/rss-harness:latest
docker run -e DEEPSEEK_API_KEY=sk-your-key makeiny/rss-harness:latest
```

### Local
```bash
git clone <repo-url>
cd rssharness
export DEEPSEEK_API_KEY=sk-your-key
./mvnw package -DskipTests
java -jar target/rssharness-0.0.1-SNAPSHOT.jar
```

### CLI Commands
```
RssHarness> What's new in AI today?        ← ask any question
RssHarness> /sync                           ← refresh route catalog from RSSHub
RssHarness> /routes                         ← list all platforms
RssHarness> /routes github                  ← list routes for a platform
RssHarness> /new                            ← start new session
RssHarness> /help
RssHarness> /exit
```

---

## ⚙️ Configuration

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

---

## 🧪 Testing

```bash
./mvnw test                          # all 53 tests
./mvnw test -Dtest=ConversationServiceTest  # single class
```

**53 tests — 0 failures**

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

## 🎯 Design Decisions

| Decision | Rationale |
|---|---|
| **Single ChatClient call** | LLM autonomously orchestrates tools — no manual tier separation |
| **DOM+XPath route parsing** | Bypasses Rome's guid corruption, extracts raw route paths from RSS XML |
| **ChatMemory multi-turn** | Spring AI MessageChatMemoryAdvisor manages session history |
| **tryMarkRefresh atomic CAS** | `ConcurrentHashMap.compute()` merges check+set, eliminates race conditions |
| **Sliding-window health** | `Deque<Boolean>` of last 10 results, sorted by success rate |
| **@Lazy circular dependency** | AiSummaryService ↔ ChatClient ↔ RssTools chain broken via lazy injection |
| **Graceful degradation** | AI summary failure → placeholder summaries, core data preserved |

---

## 🗺️ Roadmap

- [x] RSS domain: multi-instance failover, async pipeline, Rome parsing, EclipseStore persistence
- [x] Storage domain: DDD aggregate root, CQS views, EclipseStore integration
- [x] AI domain: Spring AI 2.0 Tool Calling, DOM+XPath route sync, ChatMemory multi-turn
- [x] CLI REPL: streaming typewriter, 3-color rendering, session management
- [x] Docker deployment: multi-stage build, docker-compose, environment variable config
- [ ] Web UI (optional)

---

## 📄 License

This project is created as a portfolio work. All rights reserved.
