<p align="center">
  <img src="docs/logo.svg" width="116" alt="坑查查 logo">
</p>

<h1 align="center">坑查查 · KENG CHACHA</h1>

<p align="center">
  <b>有坑没坑，先查查。</b><br>
  生活防坑与常识科普平台 —— <b>看得清 · 查得到 · 测得准 · 护得住</b>
</p>

<p align="center">
  <a href="https://github.com/topbat/kengchacha-app/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/topbat/kengchacha-app/actions/workflows/ci.yml/badge.svg"></a>
  <a href="https://github.com/topbat/kengchacha-app/releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/topbat/kengchacha-app?display_name=tag&color=0E9E8E"></a>
  <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-22C55E.svg"></a>
  <a href="#-参与贡献"><img alt="PRs Welcome" src="https://img.shields.io/badge/PRs-welcome-16D6B4.svg"></a>
  <img alt="Backend" src="https://img.shields.io/badge/Java%2021-Spring%20Boot%203.2-6DB33F?logo=springboot&logoColor=white">
  <img alt="Frontend" src="https://img.shields.io/badge/React%2018-Vite%206-646CFF?logo=vite&logoColor=white">
</p>

<p align="center">
  <a href="https://github.com/topbat/kengchacha-app/releases/download/v1.0.0/kengchacha-promo.mp4">▶ 观看 5 分钟产品宣传片</a>
  &nbsp;·&nbsp; <a href="docs/DEPLOYMENT.md">🚀 部署指南</a>
  &nbsp;·&nbsp; <a href="docs/DEVELOPMENT.md">🛠 二次开发</a>
  &nbsp;·&nbsp; <a href="#-快速开始">⚡ 快速开始</a>
</p>

<p align="center">
  <img src="docs/screenshots/h5-feed.png" width="190" alt="避坑头条">
  <img src="docs/screenshots/h5-toolbox.png" width="190" alt="风险检测工具箱">
  <img src="docs/screenshots/h5-assistant.png" width="190" alt="AI 避坑助手">
  <img src="docs/screenshots/h5-guardian.png" width="190" alt="家人守护">
</p>

---

## 📖 这是什么

**坑查查** 把「反诈 / 防坑 / 消费维权」常识，做成 **可查、可测、可分享、可守护** 的科普平台，覆盖从**主动学习**到**实时检测**再到**家人守护**的完整链路。本仓库是依据《坑查查-产品需求文档PRD.md》《坑查查-技术实现方案.md》《坑查查-原型设计.html》落地的**可运行 MVP（v1.0）**。

> 🟢 一套 React 代码同时适配 **手机 H5 + 电脑 PC**；后端 **Java 21 / Spring Boot**；开发期 **零密钥可跑**（离线规则/确定性 + 接口隔离，切生产仅换实现）。

<table>
<tr>
<td align="center" width="16.6%">📰<br><b>看</b><br><sub>资讯</sub></td>
<td align="center" width="16.6%">🧰<br><b>查</b><br><sub>风险</sub></td>
<td align="center" width="16.6%">💬<br><b>问</b><br><sub>助手</sub></td>
<td align="center" width="16.6%">🧭<br><b>测</b><br><sub>能力</sub></td>
<td align="center" width="16.6%">✍️<br><b>报</b><br><sub>经历</sub></td>
<td align="center" width="16.6%">👵<br><b>护</b><br><sub>家人</sub></td>
</tr>
</table>

> 内容均为**科普改写示意**，不点名定性具体企业；AI 输出标注"不构成法律/官方结论"。

---

## 🎬 产品宣传片

一支约 **5 分钟** 的产品发布短片（真机截图 + 品牌动效，1920×1080）：

- ▶ **观看 / 下载视频**：[releases/v1.0.0 → kengchacha-promo.mp4](https://github.com/topbat/kengchacha-app/releases/download/v1.0.0/kengchacha-promo.mp4)
- 🌐 **在线动态版（可全屏播放/录屏）**：[`promo/index.html`](promo/)　·　分镜与旁白脚本：[`docs/宣传片脚本-Storyboard.md`](docs/宣传片脚本-Storyboard.md)

---

## 🖥 桌面端（PC）

同一套 React 应用响应式适配：窗口 **≥980px** 自动切换为「左侧导航 + 宽屏双列信息流」的桌面布局。

<p align="center"><img src="docs/screenshots/pc-feed.png" width="860" alt="坑查查 PC 端"></p>

---

## 🧩 产品模块与功能介绍

> 移动端（H5）为主形态；下列每个模块均为**可运行垂直切片**，截图取自真实运行实例。

<table>
<tr>
<td width="260" valign="top"><img src="docs/screenshots/h5-feed.png" width="240" alt="避坑头条"></td>
<td valign="top">

### 1 · 📰 避坑头条
**智能科普信息流。** 支持「领域 / 人群 / 危害 / 地域 / 时效」**五维标签**筛选与关键词搜索；顶部「✨为你推荐」基于浏览足迹与兴趣标签做**语义召回（可解释）**；每张卡片可「🔊听一听 / 🔗相似坑 / 🖼生成海报」。

`GET /api/content/feed` · `/tags` · `GET /api/recommend/similar/{id}` · `POST /api/recommend/for-you`
</td>
</tr>

<tr>
<td width="260" valign="top"><img src="docs/screenshots/h5-toolbox.png" width="240" alt="风险检测工具箱"></td>
<td valign="top">

### 2 · 🧰 风险检测工具箱
**把"拿不准的东西"直接丢进来出结论。** 四类检测器 + 私密检测记录（仅脱敏预览）：
- **合同体检** —— 风险条款标红（定金不退 / 自动续费 / 空白条款 / 培训贷 / 高额违约金）
- **链接验毒** —— 仿冒域名、IP 直连、诱导词识别
- **拍照识坑** —— 诈骗话术分类（支持 🎤 口述截图文字）
- **收款核验** —— 公对私、户名不符等异常提示

`POST /api/toolbox/{contract,link,image,payee}` · `GET /api/toolbox/records`
</td>
</tr>

<tr>
<td width="260" valign="top"><img src="docs/screenshots/h5-assistant.png" width="240" alt="AI 避坑助手"></td>
<td valign="top">

### 3 · 💬 AI 避坑助手
**规则版 RAG + 固定五段式作答**：风险判定 → 套路拆解 → 你该怎么办 → 官方渠道（96110/110/12315）→ 关联案例，并附免责声明。`LlmClient` 接口已预留云大模型接入。

`POST /api/assistant/chat`
</td>
</tr>

<tr>
<td width="260" valign="top"><img src="docs/screenshots/h5-quiz.png" width="240" alt="防坑能力自测"></td>
<td valign="top">

### 4 · 🧭 防坑能力自测
**出题（不下发答案）→ 评分 → 维度短板 → 防坑画像 / 高危场景 / 行动建议。** 覆盖**心理 / 法律 / 消费 / 金融 / 职场 / 网络**六大维度，支持快测 10 题 / 标准 30 题 / 深度 50 题。

`GET /api/quiz/start` · `POST /api/quiz/submit`
</td>
</tr>

<tr>
<td width="260" valign="top"><img src="docs/screenshots/h5-report.png" width="240" alt="踩坑上报"></td>
<td valign="top">

### 5 · ✍️ 踩坑上报（UGC）
**"不会打字？"** 点 🎤 语音转写说出经过 → 「AI 成稿」自动整理结构 → 提交（含 AI 审核占位）→ 「踩坑广场」展示审核通过案例，可点赞「学到了 / 点亮」。

`GET/POST /api/ugc/stories` · `POST /api/ugc/stories/{id}/like`
</td>
</tr>

<tr>
<td width="260" valign="top"><img src="docs/screenshots/h5-guardian.png" width="240" alt="家人守护"></td>
<td valign="top">

### 6 · 👵 家人守护
**为长辈而生。** 为家人订阅关心的风险领域 → 「🛡️一键守护扫描」按订阅生成**适老化口播预警** → 「🔊朗读播报」，支持「🔍大字模式」、绑定 / 解绑 / 已读。

`GET /api/guardian/overview` · `POST /api/guardian/relations` · `/push-all`
</td>
</tr>

<tr>
<td width="260" valign="top"><img src="docs/screenshots/h5-me.png" width="240" alt="成长与徽章"></td>
<td valign="top">

### 7 · 👤 成长与徽章
防坑贡献值、身份等级、徽章墙（避坑萌新 / 人间清醒 / 反诈布道者 / 打假斗士…）；聚合「我的上报、检测记录、收藏、家人守护、适老化语音、隐私设置」入口。

`GET /api/growth/me`
</td>
</tr>
</table>

#### 🖼🔊🧠 横切能力
- **文生图海报**：结构化要素 → 服务端 SVG 模板渲染，多端排版一致，可下载 SVG/PNG。`POST /api/share/poster`
- **语音 ASR/TTS**：在线优先走浏览器 Web Speech，离线/兜底经 `VoiceClient`。`POST /api/voice/asr` · `/tts`
- **向量检索 / 个性化推荐**：`EmbeddingClient` 离线向量化 → 内存向量索引余弦召回（相似案例 + 为你推荐）。`GET /api/recommend/*`

> 三类能力沿用「**离线规则/确定性 + 接口隔离**」范式：`LlmClient` / `EmbeddingClient` / `VoiceClient` 抽象 + `@ConditionalOnProperty` 切换 —— 开发期零密钥可跑，切生产仅换实现（云大模型 / BGE·M3E / 讯飞·阿里云语音 / pgvector·Milvus），上层不变。

---

## 🔌 模块 × 接口速查表

| 模块 | 核心能力 | 主要接口 |
|------|------|------|
| 避坑头条 | 五维标签筛选 + 搜索 + 分页；2 小时更新倒计时 | `GET /api/content/feed`、`/{id}`、`/tags`、`/api/refresh/countdown` |
| 风险检测工具箱 | 合同 / 链接 / 拍照 / 收款 四检测器 + 私密记录 | `POST /api/toolbox/{contract,link,image,payee}`、`GET /api/toolbox/records` |
| AI 避坑助手 | 规则 RAG + 五段式作答 + 官方渠道 | `POST /api/assistant/chat` |
| 防坑自测 | 出题 → 评分 → 六维画像 | `GET /api/quiz/start`、`POST /api/quiz/submit` |
| 踩坑上报 UGC | 语音转写 + AI 成稿 + 审核 + 广场点赞 | `GET/POST /api/ugc/stories`、`POST /api/ugc/stories/{id}/like` |
| 家人守护 | 订阅 → 扫描 → 适老化预警 | `GET /api/guardian/overview`、`POST /api/guardian/relations`·`/push-all` |
| 成长 / 徽章 | 成长概览 + 徽章解锁 | `GET /api/growth/me` |
| 向量检索 / 推荐 | 相似案例 + 为你推荐（可解释） | `GET /api/recommend/similar/{id}`、`POST /api/recommend/for-you` |
| 文生图海报 | 服务端 SVG 渲染，可下载 | `POST /api/share/poster` |
| 语音 ASR / TTS | Web Speech 优先 + 离线兜底 | `POST /api/voice/asr`·`/tts` |
| PC 端 | ≥980px 响应式桌面布局 | 复用全部上述接口 |

---

## 🧱 技术栈

| 层 | 选型 |
|------|------|
| **后端** | Java 21（虚拟线程）· Spring Boot 3.2 · Spring Data JPA · **H2 内存库（PostgreSQL 兼容模式）** · Maven；模块化单体（即未来微服务边界） |
| **前端** | Vite 6 · React 18 · TypeScript · react-router；`/api` 代理后端；H5 + PC 响应式 |
| **数据** | `schema.sql` 建表 + `data.sql` 种子（避坑内容含五维标签、自测题库、徽章、示例 UGC） |
| **切生产** | 依赖均经接口隔离，按 Profile 切 PostgreSQL+pgvector / Redis / ES / 云大模型 |
| **CI** | GitHub Actions：后端 `mvn verify`（编译·测试·打包归档 jar）+ 前端 `tsc --noEmit` + `vite build`（归档 dist） |

---

## ⚡ 快速开始

```bash
# 1) 后端（:8080）
mvn -f backend/pom.xml spring-boot:run
curl http://localhost:8080/api/refresh/countdown      # 健康自检

# 2) 前端（:5173）— 另开一个终端
cd frontend && npm install && npm run dev              # 打开 http://localhost:5173
```
> 前端通过 Vite 代理把 `/api/*` 转发到 `http://localhost:8080`，**请先启动后端**。
> 生产构建：`npm --prefix frontend run build`（产物在 `frontend/dist`）。完整部署见 [部署指南](docs/DEPLOYMENT.md)。

<details>
<summary><b>📂 目录结构（点击展开）</b></summary>

```
kengchacha-app/
├── backend/                 Spring Boot 后端（模块化单体）
│   └── src/main/java/com/kengchacha/
│       ├── common/   content/   refresh/   quiz/   ugc/   assistant/
│       ├── growth/   toolbox/   guardian/  media/  voice/ recommend/
│   └── src/main/resources/  application.yml / schema.sql / data.sql
├── frontend/                Vite + React H5 + PC 端响应式
│   └── src/{api.ts, App.tsx, styles.css, voice.ts, components/, pages/}
├── docs/                    截图 / 部署指南 / 二开指南 / 宣传片脚本 / logo
├── promo/                   产品宣传片（HTML 动态片 + 说明）
├── 坑查查-产品需求文档PRD.md / 坑查查-技术实现方案.md / 坑查查-原型设计.html
├── LICENSE                  MIT
└── README.md
```
</details>

<details>
<summary><b>🧭 快速体验路径（点击展开）</b></summary>

1. **头条**：顶部🎤语音搜索或点"领域→网络诈骗""危害→高危"五维筛选；"✨为你推荐"按浏览/兴趣语义召回；卡片可"🔊听一听 / 🔗相似坑 / 🖼海报"。
2. **工具箱**：选「合同体检」点"填入示例"→"开始检测"看风险条款标红；「链接验毒」填 `taobao-anquan.cn` 看仿冒命中。
3. **AI 助手**：点示例"我妈被拉进一个荐股群…"，看五段式作答 + 关联案例 + 96110 提示。
4. **自测**：快测 10 题 → 提交 → 看防坑画像、六维条形图、高危场景与建议。
5. **上报**：🎤 语音转写 → ✍️ AI 成稿 → 提交 → 踩坑广场点赞。
6. **家人守护**：添加家人订阅风险领域 → 🛡️一键守护扫描 → 🔊朗读播报；可开🔍大字模式。
7. **PC 端**：窗口拉宽到 ≥980px，自动切换桌面布局。
</details>

---

## 📚 文档与资源

| 文档 | 说明 |
|------|------|
| [🚀 部署指南 DEPLOYMENT](docs/DEPLOYMENT.md) | 本地/裸机/Docker/compose 部署、生产配置、Nginx、CI/CD、FAQ |
| [🛠 二次开发指南 DEVELOPMENT](docs/DEVELOPMENT.md) | 架构约定、新增模块手把手、接入云 LLM/向量/语音、前端二开 |
| [🎬 宣传片脚本 Storyboard](docs/宣传片脚本-Storyboard.md) | 5 分钟产品发布短片分镜与旁白 |
| [📺 产品宣传片（可播放/可录制）](promo/) | 纯 HTML 自动播放动态宣传片，可全屏播放或录屏导出视频 |

---

## 🤝 参与贡献

**人人都能参与！** 本项目采用最宽松的开源协议，欢迎任何形式的贡献 —— 修 bug、加功能、补内容、改文案、写文档、提建议都欢迎：

1. **Fork** 本仓库并基于 `main` 新建 `feat/xxx` 分支；
2. 提交前自检：后端 `mvn -f backend/pom.xml verify`，前端 `npx tsc --noEmit && npm run build`（与 CI 一致，全绿即可）；
3. 用约定式提交信息（`feat: / fix: / docs: …`，中文描述清晰）；
4. 提 **Pull Request**，描述清楚动机与改动。

> 工程约定、如何新增模块、如何接入云能力，详见 [二次开发指南](docs/DEVELOPMENT.md)。
> 行为准则：保持友善、尊重、就事论事；内容须为科普改写示意，不点名定性具体企业。

---

## 📄 开源协议

本项目基于 **[MIT License](LICENSE)** 开源 —— 一个被广泛采用、对贡献者最友好的**宽松许可协议**：

- ✅ **自由**使用、复制、修改、合并、发布、分发、再许可、商用；
- ✅ 个人、公司、商业项目均可，无需付费、无需授权申请；
- 📌 唯一要求：在副本中保留版权与许可声明（一行字的事）。

> 我们希望「人人都能参与进来」——无论你是想学习、二次开发，还是把它用到自己的产品里，MIT 都给你最大的自由。
> （如需更极致的「零条件 / 公共领域」协议，如 The Unlicense / 0BSD，也可在 issue 中提出。）

---

## ⚠️ 说明
- 所有内容为**科普改写示意**，不点名定性具体企业；AI 输出标注"不构成法律/官方结论"。
- 反诈专线 **96110** · 消费维权 **12315**。
- Windows PowerShell 直接 `Invoke-RestMethod` 看接口可能显示中文乱码（PS5.1 解码问题），浏览器/`curl.exe` 正常。

<p align="center"><sub>出品 · <b>topbat</b> &nbsp;|&nbsp; 《有坑没坑，先查查》</sub></p>
