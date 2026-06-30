# Java 21 + Spring AI 2.0：手搓一个 RSS 赋能的 AI 精准搜索代理（附完整架构与 Docker 部署）

> **一句话结论**：RssAgent 不依赖倒排索引和全网爬虫，通过 RSSHub 的结构化路由命名空间实现实时定点抓取，一次 LLM 调用自主编排「平台发现 → 路由选择 → RSS 抓取 → 摘要汇总」全流程。53 个测试用例全部通过，Docker 一键部署。

---

## 目录

1. [为什么要做这个项目](#1-为什么要做这个项目)
2. [核心架构](#2-核心架构)
3. [技术栈](#3-技术栈)
4. [四个关键设计](#4-四个关键设计)
5. [CLI 实战](#5-cli-实战)
6. [设计决策](#6-设计决策)
7. [Docker 部署](#7-docker-部署)
8. [常见问题 (FAQ)](#8-常见问题-faq)

---

## 1. 为什么要做这个项目

**LLM 的最大短板不是"不够聪明"，而是"不可溯源"。** ChatGPT、豆包、Kimi 生成的内容看起来头头是道，但你永远不知道它从哪里拼凑出来的。作为秋招作品，我想证明两件事：

1. **Java 生态也能做 AI 应用**，不一定非得 Python
2. **RSS 不等于"过时技术"**——RSSHub 的结构化路由命名空间（`site/channel/keyword`）本身就是一种**天然的精准索引**

传统搜索引擎依赖倒排索引，需要爬取、清洗、建库。而 RSSHub 已经把全网 1600+ 个网站的内容按 `/平台/频道/参数` 的形式做了结构化路由——每一个 `/zhihu/hot`、`/weibo/search/AI` 就是一个精确的数据源。你不需要"搜"，你只需要"选"。

**这就是 RssAgent 的核心灵感：让 LLM 像一个搜索助理一样，先发现有哪些平台和路由可用，再精准抓取，最后汇总——全程可溯源。**

---

## 2. 核心架构

```
CLI (命令行交互)                     ← 直接输入问题，无需 /search 前缀
  │
AI Domain (智能编排层)
  ├─ ConversationService             ← 单次 ChatClient 调用，LLM 自主决策
  │    ├─ searchPlatforms (Tool)     ← 关键词搜索平台
  │    ├─ listRoutes (Tool)          ← 列出平台下所有路由
  │    ├─ fetchRss (Tool)            ← 批量抓取（异步扇出）
  │    └─ readSummaries (Tool)       ← 读取 AI 摘要内容
  │    ├─ RouteCatalog               ← 内存路由索引（1661 平台/3238 路由）
  │    └─ RouteSyncTask              ← DOM+XPath 从 RSSHub RSS 源同步
  │
RSS Domain (抓取管道)
  ├─ RouteFetchService               ← CompletableFuture.allOf 扇出
  │    ├─ ArticlesFetchService       ← 原子 tryMarkRefresh (CAS) 防重复
  │    ├─ RssFetcher                 ← 多实例故障转移 + 中文 URL 编码
  │    └─ AiSummaryService           ← 逐条 LLM 摘要 + 降级保护
  │
Storage Domain (DDD 持久化)
  ├─ DataRoot                        ← EclipseStore 聚合根
  └─ SummaryView                     ← CQS 读写视图
```

**关键创新：摒弃了经典的 Tier1→Tier2→Tier3→Tier4 手动编排模式。** 在 Spring AI 2.0 中，将四个 @Tool 方法注册到 ChatClient，LLM 在一次流式调用中自主决定调用顺序。不需要硬编码任何管线逻辑——LLM 是真正的 Driver。

---

## 3. 技术栈

| 层级 | 技术 | 选型理由 |
|---|---|---|
| 语言 | Java 21 | Spring AI 2.0 的 baseline |
| 框架 | Spring Boot 4.1.0 | 最新 GA，原生支持 Virtual Threads |
| AI | Spring AI 2.0.0 + DeepSeek Chat | `@Tool` 注解 + `ChatClient.entity()` 结构化输出 |
| RSS 解析 | Rome 2.1.0 | 标准 RSS/Atom 解析（仅用于文章解析，路由同步用原始 DOM） |
| HTTP | Apache HttpClient 5.6.1 | 30s 超时，中文路径自动 URL 编码 |
| 持久化 | EclipseStore 4.1.0 | 零配置嵌入式对象图，Java 对象即数据库 |
| 日志 | SLF4J + Logback | Spring Boot 标准栈 |
| 容器化 | Docker + docker-compose | 多阶段构建，双服务编排 |
| 测试 | JUnit 5 + Mockito + AssertJ | 53 个测试，覆盖 AI/RSS/Storage 全域 |

---

## 4. 四个关键设计

### 4.1 LLM 自主编排，不硬编码管线

```java
// AiConfig.java — 注册四个工具，交给 LLM 自主决策调用顺序
@Bean
public ChatClient chatClient(ChatModel chatModel, RssTools rssTools) {
    return ChatClient.builder(chatModel)
            .defaultTools(rssTools)  // searchPlatforms, listRoutes, fetchRss, readSummaries
            .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
            .build();
}

// ConversationService.java — 单次调用，无需手动分 Tier
chatClient.prompt()
        .user(question)              // 用户原始问题
        .advisors(a -> a.param("chat_memory_conversation_id", sessionId))
        .stream().content()          // 流式输出 → CLI 打字机效果
        .doOnNext(cb::onResponseToken)
        .blockLast();
```

**对比传统做法**：大多数 LLM 应用会手动拆成 3-4 步调用（选平台→选路由→抓取→汇总），每次都得解析 JSON、处理异常、拼装上下文。Spring AI 2.0 的 Function Calling 循环把这件事全自动了——LLM 自己决定什么时候调哪个工具。

### 4.2 DOM+XPath 直取路由数据，绕过 Rome 污染

```java
// RouteSyncTask.java — 不用 Rome 的 getUri()，直接从 XML 提取 guid
Document doc = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder().parse(new ByteArrayInputStream(response.body()));
var xpath = XPathFactory.newInstance().newXPath();
NodeList items = (NodeList) xpath.evaluate("//item", doc, XPathConstants.NODESET);

for (int i = 0; i < items.getLength(); i++) {
    String guid  = xpath.evaluate("guid/text()", items.item(i));  // 原始路径
    String title = xpath.evaluate("title/text()", items.item(i)); // "平台 - 路由名"
    // 聚合到 Map<String, Map<String, Object>>
}
```

Rome 的 `SyndEntry.getUri()` 会对 `<guid isPermaLink="false">/weibo/search/hot</guid>` 做内部路径拼接，把 feed 的 base URI 和 guid 拼在一起，产生 `/微博//weibo/search/hot` 这种双斜杠路径。**这个问题卡了我整整两天——内存快照、Python 脚本、逐行调试才定位。** 最终方案是绕过 Rome，用 DOM + XPath 直取原始文本。

### 4.3 异步全链路 + 中文 URL 编码

```java
// RouteFetchService.java — N 个路由并发抓取
public CompletableFuture<List<FetchResponse>> fetchRoutes(List<String> routes) {
    List<CompletableFuture<FetchResponse>> futures = routes.stream()
            .map(this::processSingleRoute)    // 每路由：抓取→摘要→存储
            .toList();
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
}

// RssFetcher.java — 中文路径自动百分号编码
for (String seg : route.split("/")) {
    if (seg.isEmpty()) continue;
    encoded.append("/").append(URLEncoder.encode(seg, StandardCharsets.UTF_8));
}
```

`/weibo/search/热搜` → `/weibo/search/%E7%83%AD%E6%90%9C`。不编码的话 HttpClient 直接吞掉中文字符。

### 4.4 三级 ANSI 颜色流式 CLI

```
RssAgent> 微博热搜现在都有什么？

💭 Thinking …                         ← 灰色：LLM 推理过程
🔧 searchPlatforms → 微博            ← 青色：工具调用结果
🔧 fetchRss → /weibo/search/hot     ← 青色：抓取执行
✓ SUCCESS  /weibo/search/hot         ← 绿色：抓取状态
  20 articles fetched

[Core Conclusion]                     ← 白色：最终回复
微博当前热搜 TOP5：...
[Supporting Information]
  1. xxx — /weibo/search/hot
  2. xxx — /weibo/search/hot

(~1200 tokens)                        ← 灰色：token 消耗统计
```

**灰色=思考，青色=工具调用，白色=最终回复。** ANSI 转义码在终端里渲染，不需要前端。

---

## 5. CLI 实战

```bash
# 启动（需要 RSSHub 实例 + DeepSeek API key）
java -jar rssagent-0.0.1-SNAPSHOT.jar

# 首次使用：同步路由表
RssAgent> /sync
  1661 platforms, 3238 routes

# 直接提问
RssAgent> 科技圈有什么新鲜事？
RssAgent> What's trending on GitHub?
RssAgent> 看看知乎最近在讨论什么

# 会话管理
RssAgent> /new          ← 清空上下文，开启新会话
RssAgent> /routes       ← 列出所有平台
RssAgent> /help         ← 查看命令
```

**不需要 `/search` 前缀**——直接输入自然语言问题即可。`/new` 命令会生成新会话 ID，ChatMemory 自动隔离上下文。

---

## 6. 设计决策

| 决策 | 理由 |
|---|---|
| **单次 ChatClient 调用** | LLM 自主编排工具调用，代码不写死管线 |
| **DOM+XPath 路由解析** | 绕过 Rome `getUri()` 的 guid 污染 |
| **ChatMemory 会话管理** | Spring AI 标准方案，不用自研 ContextManager |
| **tryMarkRefresh 原子 CAS** | 异步环境下消除并发竞态 |
| **@Lazy 打破循环依赖** | AiSummaryService ↔ ChatClient ↔ RssTools 互引用 |
| **三级颜色 CLI** | 提升 LLM 调用链路可观测性 |
| **EclipseStore 持久化** | 零配置嵌入式存储，Java 对象图即数据库 |

---

## 7. Docker 部署

```bash
# 一键启动（含 RSSHub + RssAgent）
git clone https://github.com/RssAgent-dev/RssAgent.git
cd RssAgent
export DEEPSEEK_API_KEY=sk-your-key
docker-compose up -d

# 或直接拉镜像
docker pull makeiny/rss-agent:latest
docker run -e DEEPSEEK_API_KEY=sk-your-key makeiny/rss-agent:latest
```

多阶段 Docker 构建（Maven 编译 → JRE 21 运行时），最终镜像约 200MB。`docker-compose.yml` 编排了 RssAgent + RSSHub 双服务，所有配置通过环境变量注入。

---

## 8. 常见问题 (FAQ)

### Q1: 为什么不直接用搜索引擎 API，而要自己搭 RAG？
**A:** 搜索引擎返回的是链接列表，LLM 还得逐个爬取、清洗、提取。RssAgent 用的是 RSSHub 的结构化路由——路由本身就定义了数据形状（`/weibo/search/AI` 直接返回 RSS XML），不需要爬虫。每一个结果是**可溯源的**——你看到的每条约会都有原始 URL。

### Q2: 和 Perplexity、Kimi 这类 AI 搜索有什么区别？
**A:** Perplexity 本质上还是靠 Google/Bing 的索引，然后 LLM 做阅读理解。RssAgent 不经过搜索引擎——它直接从源站拉 RSS feed，天然避免了 SEO 内容污染和死链问题。

### Q3: 为什么选 Java 做 AI 应用，而不是 Python？
**A:** Spring AI 2.0 在 2026 年 6 月刚 GA，`@Tool` 注解、`ChatClient.entity()` 结构化输出、`MessageChatMemoryAdvisor` 会话管理这套 API 设计非常成熟。加上 Spring Boot 的生态（异步、DI、测试、Actuator），Java 做 AI 应用的开发体验不比 Python 差——而且不用操心 GIL、包管理、类型安全。

### Q4: 为什么选了 EclipseStore 而不是 MySQL/Redis？
**A:** 嵌入式对象图数据库，零配置。`storageManager.store(myObject)` 一步持久化，不需要建表、写 SQL、配连接池。对于单机部署的个人项目极度友好——数据文件就是 `./data/eclipse-store` 目录，备份直接 `cp`。

### Q5: 为什么不加 Embedding 向量检索？
**A:** 不需要。RssAgent 的"索引"是 RSSHub 路由命名空间——`/weibo/search/:keyword` 本身就是结构化索引。LLM 通过工具调用浏览路由树，比向量匹配更精确。没有 embedding 调用，也省了一笔 API 开销。

### Q6: 路由表怎么更新？会不会过期？
**A:** 启动时从本地 `data/routes.json` 加载（上次 `/sync` 写入），`/sync` 命令从 RSSHub RSS 源拉取最新路由表（RSS 协议，约 1661 平台/3238 路由）。可按需手动触发更新。

---

## 项目链接

- **GitHub**: [https://github.com/RssAgent-dev/RssAgent](https://github.com/RssAgent-dev/RssAgent)
- **Docker Hub**: `docker pull makeiny/rss-agent:latest`
- **技术栈**: Java 21 / Spring Boot 4.1.0 / Spring AI 2.0.0 / DeepSeek Chat / EclipseStore / Docker

---

*本文为 RssAgent 项目发布文章，采用 GEO（生成式引擎优化）策略编写，欢迎在评论区交流技术细节。*
