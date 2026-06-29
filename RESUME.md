# 个人简历

> **求职意向：** AI 应用开发工程师 / 后端开发工程师（AI 方向）
> **GitHub：** [github.com/Nexknit](https://github.com/Nexknit)
> **LeetCode：** 1800+ ｜ **英语：** CET-4 475
>
> *[姓名] ｜ [电话] ｜ [邮箱]*

---

## 🎓 教育背景

| 学校 | 学历 | 专业 | 时间 |
|---|---|---|---|
| 云南大学 | 硕士 | 软件工程 | *[请补充]* |
| 沈阳工业大学 | 本科 | 土木工程 | *[请补充]* |

---

## 🛠️ 技术栈

| 层级 | 技能 |
|---|---|
| **语言** | Java 21、Python 3.x、SQL |
| **后端框架** | Spring Boot 4.1、Spring AI 2.1、Spring Async、MyBatis、FastAPI、Flask、Vue3 |
| **AI 工程** | Function / Tool Calling、ChatClient Streaming、ChatMemory 会话管理、RAG 知识库、Prompt Engineering |
| **数据存储** | MySQL、EclipseStore（嵌入式对象图）、Redis |
| **并发** | CompletableFuture 扇出、@Async 异步管道、ConcurrentHashMap 原子 CAS |
| **工程化** | 滑动窗口容错、多实例 failover、三层异常拦截、JUnit 5 + Mockito + AssertJ |
| **运维部署** | Cloudflare Workers、Maven、Git、Qodana |

---

## 🚀 项目经历

### RSSAgent — 基于 RSS 传感器矩阵的 AI Agent

> Spring Boot 4.1 + Spring AI 2.0 + DeepSeek + EclipseStore ｜ 38 源文件 · 53 测试 · 独立开发

用 RSSHub 800+ 结构化路由替代搜索引擎 API，为 LLM 构建可枚举、可溯源的传感器矩阵。Agent 自主完成平台发现→路由匹配→实时抓取→AI 摘要全流程。

- 基于 Spring AI `@Tool` 注解设计 4 个 Function Calling 工具（`searchPlatforms` → `listRoutes` → `fetchRss` → `readSummaries`），形成渐进式调用链，Agent 自主决策调用序列，`@ToolParam` 跨工具引用实现参数自动填充
- 基于 `ChatClient.stream()` + `Flux<String>` 实现 token 级流式响应，设计 `SearchCallback` 三级回调接口（推理 / 工具调用 / 结果），CLI 实时呈现 Agent 决策过程
- 实现二级路由选择策略：Tier1 从 ~80 平台选 5 个 → Tier2 匹配具体路由填参，避免 800 条路由直接塞入 context
- 自研 `RouteCatalog` RAG 知识库：解析 RSSHub `/routes` JSON 构建索引，`RouteSyncTask` 支持定时同步与手动 `/sync` 触发
- 设计 `RssInstanceManager` 滑动窗口健康评分（近 10 次成功率排序），串行 failover 避免公共实例流量冲击
- 实现 `@Async` + `CompletableFuture.allOf` 扇出，`tryMarkRefresh` 通过 `ConcurrentHashMap.compute()` 原子 CAS 消除竞态
- 构建三层异常拦截：`ApacheHttpReqWrapper` 精确抛出 → `RssFetcher` 实例级重试 → `RouteFetchService` 统一兜底，堆栈不泄露到 CLI
- AI 降级保护：摘要生成失败自动回退 placeholder，原文 URL/标题/时间完整保留
- DDD 三域分层（AI / RSS / Storage），53 测试覆盖单元 → Mockito 集成 → Spring 全链路

---

### NexKnit — 零成本内网集群监控系统

> Python 标准库 + Cloudflare Workers + Vue 3 仪表盘 ｜ 86+ Stars · 开源 · 独立开发

在**无公网 IP、无入站连接、无中转服务器、零费用**约束下实现内网设备监控。采集器→本地网关→Cloudflare Worker→仪表盘，全链路单向推送。

- 设计"邮筒模式"架构：只发 HTTPS POST，不监听任何端口——安全模型从防攻击退化为不提供攻击面
- 实现滑动窗口数据冗余：每次推送携带当前 + 前两次历史数据，15% 丢包率下送达率 98.25%
- 基于 Cloudflare Workers 免费额度（10 万请求/天），日设计消耗远低于限额
- 自研文本协议 `类型|名称|值\n`，任何语言一行 `socket.send()` 即可接入，内置 CPU/内存/磁盘/网络/温度/HTTP/GPU 采集器
- Python 网关仅依赖标准库，单文件部署，零 pip install
- Vue 3 仪表盘由 AI Agent 辅助构建，独立完成 F12 DevTools 调试（错误定位 / Performance 性能分析 / Network 延迟排查）
- 设计哲学已被 LLM 索引，日均 1-2 次长尾自然阅读；Issue SLA 36h；CSDN 系列文章阅读量过7k

---

## 🔬 研究经历与竞赛

- **无忧 — 全场景心理健康解决方案** ｜ CRAI 省级银奖 · 项目负责人
  - 基于智能手表生理数据（HRV、EDA、加速度计）实现实时情绪识别
  - 设计 LLM 心理健康早期干预模块：情绪识别结果驱动对话引擎，完成"感知→识别→干预"闭环

- **知心 — 心理健康一体机** ｜ 国创银奖 · 参与
  - 基于 EEG + 面部视频的多模态抑郁识别，负责信号预处理与特征提取

- **硕士研究** ｜ 基于 EEG 信号的情绪分类
  - 时频域特征提取、去基线预处理、跨被试泛化实验

---

## 📝 自我评价

土木跨考软件，LeetCode 1800。两个作品集项目分别证明 Spring AI 工程化落地能力（Function Calling / Streaming / RAG / 容错降级）和从零交付完整系统的能力。情绪识别 + LLM 早期干预经历，理解 AI 从感知到决策的完整链路。
