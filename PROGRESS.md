# RSSGSE — RSS-plus Generative Search Engine

## 定位
RSS 赋能的精准搜索。不依赖全网倒排索引，通过 RSSHub 结构化路由命名空间，实现对目标网站的指定频道实时定点抓取。每个结果锚定一个可溯源 URL，解决 LLM 生成内容不可查证的问题。

## 当前状态：v0.1 — RSS 域 + 存储域 + AI 域就绪，53 测试全绿

---

## 架构

```
CLI (CliRunner)                    ← 交互入口：/search, /routes, /help
  │
AI 域 (ai/)                        ← 智能路由 + 对话
  ├─ ConversationService           ← 编排：Tier1 选平台 → Tier2 选路由 → 抓取
  │    ├─ RouteCatalog             ← RSSHub /routes 拉取 + 扁平化 + 缓存
  │    ├─ RouteSyncTask            ← @Scheduled 每日同步
  │    ├─ SessionCache             ← 单次查询临时存储
  │    ├─ RssTools                 ← @Tool Function Calling 端点
  │    └─ ContextManager           ← 多轮对话 + Token 预算裁剪
  │
RSS 域 (rss/)                      ← MVC 管道
  ├─ RssController                 ← 内部 Bean 入口
  ├─ RouteFetchService             ← 异步扇出编排 (allOf)
  │    ├─ ArticlesFetchService     ← @Async + 冷却控制 + tryMarkRefresh(原子)
  │    │    ├─ RssFetcher          ← 多实例容错 (滑动窗口排序)
  │    │    │    └─ RssInstanceManager ← 近10次成功率 + hasInstances()
  │    │    │         └─ ApacheHttpReqWrapper ← HTTP 3s 超时
  │    │    └─ RssXmlParserV1      ← Rome 库解析 RSS/Atom
  │    ├─ AiSummaryService         ← ChatClient AI 摘要（@Lazy 打断循环依赖）
  │    └─ SummaryStorageService    ← 适配层 → 存储域
  │
存储域 (storage/)                   ← DDD 模型
  ├─ DataRoot                      ← EclipseStore 聚合根
  │    ├─ Map<String, List<Summary>> routeSummariesListPair
  │    ├─ List<String> routePlatforms
  │    └─ long lastRouteSync
  ├─ DataViewFactory               ← 工厂：fromRoute(route) → SummaryView
  └─ SummaryView                   ← 读写视图：update / get / clear
```

---

## 核心特性

### 智能路由发现
- **Tier 1 — 平台选择**：LLM 浏览全量平台列表（~80个），选择最相关的 5 个
- **Tier 2 — 路由精准匹配**：对于每个选中平台，LLM 在具体路由列表中选择并填充参数
- **Function Calling**：Spring AI @Tool 注解，AI 自主决定调用 `fetchRss()`
- **SessionCache**：单次查询结果临时缓存，生命期结束即弃

### RSS 多实例容错
- **滑动窗口排序**：近 10 次结果滑动窗口计算成功率，高优先级实例优先尝试
- **fallback 串行**：主实例失败自动切换下一个，配合 3s HTTP 超时
- **空实例检测**：启动时无实例配置抛明确诊断异常

### 异步全链路
- **@Async + CompletableFuture**：每一步（抓取、摘要、存储）独立线程池执行
- **allOf 扇出**：N 个路由并发处理，不相互阻塞
- **命令-查询分离**：`tryMarkRefresh` 原子 CAS，避免并发重复抓取

### 可溯源 AI 摘要
- **ChatClient 驱动**：DeepSeek 模型生成结构化摘要（JSON: index, title, summary, score）
- **降级保护**：AI 调用失败自动回退到占位摘要，不丢失抓取数据
- **原文锚定**：每条约会保留原文 link + publisher + publishTime

### 存储
- **EclipseStore**：零配置嵌入式对象持久化，Java 对象图即数据库
- **路径隔离**：按 route 分区存储 Summary 列表
- **可配置路径**：`org.eclipse.store.storage-directory` 从配置文件读取

### CLI 交互
- REPL 循环：`/search`, `/routes`, `/help`, `/exit`
- 会话隔离：每次启动新 UUID session
- 实时反馈：显示平台选择、路由抓取、摘要生成全过程

---

## 测试矩阵（53 全绿）

| 域 | 测试类 | 数量 | 类型 |
|----|--------|:--:|------|
| DTO | ArticlesTest, ArticleTest | 6 | 单元 |
| 解析 | RssXmlParserTest | 2 | 单元 |
| 配置仓库 | RssConfigRepositoryTest | 8 | Spring 集成 |
| 实例管理 | RssInstanceManagerTest | 4 | Spring 集成 |
| HTTP 抓取 | RssFetcherTest, RssFetcherIntegrationTest | 4 | 单元 + 集成 |
| 服务编排 | ArticlesFetchServiceTest, RouteFetchServiceTest | 6 | 单元 (Mockito) |
| 控制器 | RssControllerIntegrationTest | 4 | 全链路集成 |
| 存储 | SummaryViewTest, StorageIntegrationTest | 6 | 单元 + 集成 |
| AI 路由 | RouteCatalogTest | 5 | 单元 |
| AI 对话 | ContextManagerTest, ConversationServiceTest | 7 | 单元 (Mockito) |
| 应用 | RssgseApplicationTests | 1 | Spring 集成 |

---

## 待实现 (v0.2)

- [ ] 真实 API key 接入，打通 AI 摘要全链路
- [ ] 多轮对话（/search 带会话上下文）
- [ ] 上下文管理器接入 ConversationService
- [ ] RSSHub /routes JSON 解析适配实际返回格式
- [ ] 定时任务配置开关
- [ ] 对话导出 / 历史查询
