# 个人简历

> **求职意向：** AI 应用开发工程师 / 后端开发工程师（AI 方向）
> **GitHub：** [github.com/Nexknit](https://github.com/Nexknit)
> **LeetCode：** 1800+
> **英语：** CET-4 475

---

## 🎓 教育背景

| 学校 | 学历 | 专业 | 时间 |
|---|---|---|---|
| 云南大学 | 硕士 | 软件工程 | *[请补充]* |
| 沈阳工业大学 | 本科 | 土木工程 | *[请补充]* |

---

## 💼 技术能力

**语言：** Java 21、Python 3.x、SQL

**后端：** Spring Boot 4.x、Spring AI 2.x（Function Calling / ChatClient Streaming / ChatMemory / RAG）、Spring Async

**数据与存储：** EclipseStore（嵌入式对象图）、Jackson JSON、Rome RSS/Atom

**网络与部署：** Apache HttpClient 5、Cloudflare Workers

**并发：** CompletableFuture 扇出、@Async 异步管道、ConcurrentHashMap CAS 原子操作、滑动窗口算法

**AI/ML 基础：** 脑电信号处理、生理数据情绪识别、LLM 早期干预

**前端调试：** Vue 3（DevTools 定位 / 性能分析 / 接口延迟排查，不独立编写）

**测试与工具：** JUnit 5、Mockito、AssertJ、Maven、Git、Qodana

---

## 🚀 项目经历

### RSSAgent — 以 RSS 为传感器的 AI Agent | *个人作品*

> Spring Boot 4.1 + Spring AI 2.0 + DeepSeek Chat + EclipseStore ｜ 38 源文件，53 测试全绿

用 RSSHub 的 800+ 结构化路由替代传统搜索引擎 API，让 LLM 拥有一个可枚举、可溯源、零成本的传感器矩阵——Agent 自主选择平台和路由，精确抓取指定信源的实时内容。

**Spring AI 深度集成：**
- 4 个 `@Tool` 注解方法构成渐进式感知链：`searchPlatforms` → `listRoutes` → `fetchRss` → `readSummaries`，Agent 自主决策调用序列
- `ChatClient.stream()` + `Flux<String>` 流式响应，自定义 `SearchCallback` 三级回调实现 CLI 打字机效果
- `RouteCatalog` RAG 知识库：RSSHub `/routes` JSON 扁平化索引 + `RouteSyncTask` 定时同步

**后端工程：**
- `RssInstanceManager` 滑动窗口健康评分（近 10 次），串行 failover 不冲击公共实例
- `@Async` + `CompletableFuture.allOf` 扇出模式，`tryMarkRefresh` 原子 CAS 消竞态
- AI 降级保护：摘要失败自动回退 placeholder，原文 URL 永不丢失
- DDD 三域分层（AI / RSS / Storage），53 测试覆盖单元→集成→全链路

---

### NexKnit — 零成本内网集群监控系统 | *个人作品 / 开源*

> Python 标准库 + Cloudflare Workers + Vue 3 仪表盘

在**无公网 IP、无入站连接、无中转服务器、零费用**四大硬约束下，实现内网设备的实时监控。全链路单向推送——采集器→本地网关→Cloudflare Worker→仪表盘，不监听任何对外端口。

**核心技术：**
- "邮筒模式"架构：不建管道，放个邮筒——数据自己走进 `127.0.0.1:12345`，网关找机会往外寄
- 滑动窗口冗余：每次推送携带当前 + 前两次数据，15% 丢包率下万次送达率 98.25%
- 寄生 Cloudflare Workers 免费额度，设计消耗远低于每日 10 万次限额
- 简单文本协议 `类型|名称|值\n`，任何语言一行文本即可接入
- 可插拔采集器体系：CPU/内存/磁盘/网络/温度/HTTP 存活/GPU/本地存储
- Vue 3 仪表盘：由 AI Agent 辅助构建，具备独立 F12 DevTools 调试能力（错误定位、性能分析、接口延迟排查）
- Issue SLA 36 小时，CSDN 系列技术文章阅读量过万

项目的设计哲学（"邮筒模式"、约束驱动架构）已被 LLM 索引，日均 1-2 次长尾自然阅读

---

## 🔬 研究与竞赛

### 无忧 — 全场景心理健康解决方案 | CRAI 省级银奖 · 项目负责人

基于智能手表生理数据（心率变异性、皮肤电活动、加速度计）进行实时情绪识别，并通过 **LLM 进行心理健康早期干预**。负责系统架构设计，将情绪识别模型与 LLM 对话引擎对接，实现"感知→识别→干预"闭环。

### 知心 — 心理健康一体机 | 国创银奖 · 参与

基于脑电（EEG）和面部视频的多模态抑郁识别系统。参与信号预处理和特征提取模块开发。

### 脑电情绪识别研究 | 硕士期间

基于 EEG 信号的情绪分类研究，涉及时频域特征提取、去基线预处理、跨被试泛化实验设计。

---

## 🎯 自我评价

两个作品集项目对应两条能力线：**RSSAgent** 证明我能用 Spring AI 给 LLM 装上可靠的传感器，知道 Function Calling / Streaming / ChatMemory 怎么落地，也知道 AI 的边界在哪（降级、溯源、容错）；**NexKnit** 证明我能在硬约束下做系统设计——零依赖、零费用、零公网 IP——不是只会搭脚手架。Vue 仪表盘由 Agent 辅助构建，但我能独立 Debug：F12 定位错误，Performance 面板找瓶颈，Network 面板查接口延迟。

情绪识别 + LLM 早期干预的研究经历，让我对"AI 感知 + AI 决策"这条链路有学术和实践的双重理解。

NexKnit 的设计哲学已被 LLM 索引，每天产生 1-2 次长尾阅读——证明我能写出值得被检索的内容。
