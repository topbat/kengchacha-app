# 坑查查（KENG CHACHA）· 生活防坑与常识科普平台 — MVP 实现

> **有坑没坑，先查查。** 本仓库是依据《坑查查-产品需求文档PRD.md》《坑查查-技术实现方案.md》《坑查查-原型设计.html》落地的**可运行 MVP**（V2.0-α 垂直切片）。

## 一、已实现功能（MVP）

| 模块 | 能力 | 接口 |
|------|------|------|
| 避坑头条 | 五维标签筛选 + 关键词搜索 + 分页；2 小时滚动更新倒计时 | `GET /api/content/feed`、`/api/content/{id}`、`/api/content/tags`、`/api/refresh/countdown` |
| AI 避坑助手 | 规则版 RAG（案例库召回）+ 固定五段式作答 + 官方渠道与免责声明；`LlmClient` 接口预留云模型 | `POST /api/assistant/chat` |
| 防坑自测 | 出题（不下发答案）→ 评分 → 维度短板 → 画像/高危场景/建议 | `GET /api/quiz/start`、`POST /api/quiz/submit` |
| 踩坑上报 UGC | 提交（含 AI 审核占位）→ 列表（仅审核通过）→ 点赞（学到了/点亮） | `GET/POST /api/ugc/stories`、`POST /api/ugc/stories/{id}/like` |
| 成长/徽章 | 我的成长概览 + 徽章解锁状态 | `GET /api/growth/me` |
| **风险检测工具箱** | 合同体检（风险条款标红）/ 链接验毒（仿冒域名·IP直连·诱导词）/ 拍照识坑（诈骗话术分类）/ 收款核验（公对私·户名不符）+ 私密检测记录（仅脱敏预览） | `POST /api/toolbox/contract`·`/link`·`/image`·`/payee`、`GET /api/toolbox/records` |
| **家人守护** | 为家人订阅风险领域 → 扫描最新高危内容 → 生成适老化口播预警，绑定/解绑/已读 | `GET /api/guardian/overview`、`POST /api/guardian/relations`·`/push-all`·`/relations/{id}/push`、`POST /api/guardian/alerts/{id}/read` |
| **文生图海报** | 结构化要素 → 服务端 SVG 模板渲染（标题/套路/损失/口诀 + 程序化插画背景 + 仿二维码），多端排版一致，前端可下载 SVG/PNG | `POST /api/share/poster` |
| **语音 ASR/TTS** | 在线优先走浏览器 Web Speech（语音搜索/转写上报/朗读播报）；离线/兜底经 `VoiceClient`，TTS 返回确定性 WAV + SSML | `POST /api/voice/asr`·`/tts` |
| **向量检索 / 个性化推荐** | `EmbeddingClient` 离线向量化（字+bigram 哈希 TF）→ 内存向量索引余弦召回：相似案例 + 为你推荐（兴趣标签×浏览足迹，可解释） | `GET /api/recommend/similar/{id}`、`POST /api/recommend/for-you` |
| **PC 端** | 同一套 React 应用响应式：≥980px 切桌面布局（左侧导航栏 + 宽屏双列信息流），<980px 维持 H5 底部 TabBar | 复用全部上述接口 |

> 上述六个模块（PRD 已规划）本轮已落地为**可运行垂直切片**，沿用「离线规则/确定性 + 接口隔离」范式：`LlmClient`/`EmbeddingClient`/`VoiceClient` 三类抽象 + `@ConditionalOnProperty` 切换，开发期零密钥可跑，切生产仅换实现（云大模型 / BGE-M3E / 讯飞·阿里云语音 / pgvector·Milvus），上层不变。

## 二、技术栈（开发期轻量化，见技术方案 §2.6）

- **后端**：Java 21（虚拟线程）+ Spring Boot 3.2 + Spring Data JPA + **H2 内存库（PostgreSQL 兼容模式）**，Maven 构建；按 `content/quiz/ugc/assistant/growth/refresh` 分模块（模块化单体，即未来微服务边界）。
- **前端**：Vite 6 + React 18 + TypeScript + react-router；`/api` 代理到后端。
- **数据初始化**：`schema.sql` 建表 + `data.sql` 种子（12 条避坑内容含五维标签、12 道题、徽章、示例 UGC）。
- 切生产：依赖均经接口隔离，按 Profile 切 PostgreSQL+pgvector / Redis / ES / 云大模型（LlmClient）。

## 三、目录结构

```
kepu-app/
├── backend/                 Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/kengchacha/
│       │   ├── common/      统一响应 / 全局异常 / CORS
│       │   ├── content/     避坑头条（含五维标签）
│       │   ├── refresh/     2 小时更新倒计时
│       │   ├── quiz/        防坑自测与画像
│       │   ├── ugc/         踩坑上报
│       │   ├── assistant/   AI 避坑助手（LlmClient 抽象 + Mock）
│       │   ├── growth/      成长与徽章
│       │   ├── toolbox/     风险检测工具箱（4 检测器 + 私密记录）
│       │   ├── guardian/    家人守护（关系/订阅/预警推送）
│       │   ├── media/       文生图海报（服务端 SVG 渲染）
│       │   ├── voice/       语音 ASR/TTS（VoiceClient 抽象 + Mock）
│       │   └── recommend/   向量检索/个性化推荐（EmbeddingClient + 向量索引）
│       └── resources/       application.yml / schema.sql / data.sql
├── frontend/                Vite + React H5 + PC 端响应式
│   └── src/{api.ts, App.tsx, styles.css, voice.ts, recent.ts,
│            components/PosterModal.tsx, pages/{Feed,Toolbox,Guardian,Assistant,Quiz,Report,Me}.tsx}
├── 坑查查-产品需求文档PRD.md
├── 坑查查-技术实现方案.md
├── 坑查查-原型设计.html
├── 坑查查-LOGO.html / 坑查查-logo.svg
└── README.md（本文件）
```

## 四、快速开始

### 1）启动后端（端口 8080）
```bash
# 方式一：直接跑（需联网首次拉依赖）
mvn -f backend/pom.xml spring-boot:run

# 方式二：打包后运行
mvn -f backend/pom.xml -DskipTests package
java -jar backend/target/kengchacha-backend-2.0.0.jar
```
- 健康自检：浏览器或 curl 访问 `http://localhost:8080/api/refresh/countdown`
- H2 控制台：`http://localhost:8080/h2-console`（JDBC URL：`jdbc:h2:mem:kengchacha`，用户 `sa`，空密码）

### 2）启动前端（端口 5173）
```bash
cd frontend
npm install
npm run dev      # 打开 http://localhost:5173
```
> 前端通过 Vite 代理把 `/api/*` 转发到 `http://localhost:8080`，**请先启动后端**。

### 3）生产构建（可选）
```bash
npm --prefix frontend run build   # 产物在 frontend/dist
```

## 五、快速体验路径

1. **头条**：顶部🎤语音搜索或点"领域→网络诈骗""危害→高危"五维筛选；顶部"✨为你推荐"按浏览/兴趣语义召回（点一条会自适应刷新）；卡片可"🔊听一听 / 🔗相似坑 / 🖼海报"。
2. **工具箱**：选「链接验毒」填示例 `taobao-anquan.cn` 看仿冒命中；「拍照识坑」可🎤口述截图文字；底部"🧾检测记录（私密）"仅存脱敏预览。
3. **AI 助手**：点示例"我妈被拉进一个荐股群…"，看五段式结构化作答 + 关联案例 + 96110 提示。
4. **自测**：快测 10 题 → 提交 → 看防坑画像、六维条形图、高危场景与建议。
5. **上报**：点"🎤 语音转写"在线识别口述经过 → "✍️ AI 成稿"整理 → 提交 → 踩坑广场点赞。
6. **我的 → 家人守护**：添加家人并订阅风险领域 → "🛡️一键守护扫描"按订阅生成适老化预警 → "🔊朗读播报"；可开"🔍大字模式"。
7. **PC 端**：浏览器窗口拉宽到 ≥980px，自动切换为左侧导航 + 宽屏双列布局。

> 语音（ASR/TTS）在线优先走浏览器 Web Speech API（Chrome/Edge 支持最佳）；不支持时回退后端离线实现。

## 六、说明
- 所有内容为**科普改写示意**，不点名定性具体企业；AI 输出标注"不构成法律/官方结论"。
- Windows PowerShell 直接 `Invoke-RestMethod` 看接口可能显示中文乱码（PS5.1 解码问题），浏览器/`curl.exe` 正常。
