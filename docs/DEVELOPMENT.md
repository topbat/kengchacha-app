# 坑查查 · 二次开发指南（DEVELOPMENT）

> 面向在本项目基础上**新增模块 / 接入真实云服务 / 二次定制**的开发者。
> 设计哲学一句话：**模块化单体 + 接口隔离 + 离线确定性默认** —— 开发期零密钥可跑，切生产只换实现、上层不变。

---

## 目录
- [1. 架构总览](#1-架构总览)
- [2. 工程约定（必读）](#2-工程约定必读)
- [3. 后端分包与分层](#3-后端分包与分层)
- [4. 新增一个后端模块（手把手）](#4-新增一个后端模块手把手)
- [5. 接入真实云服务（LLM / 向量 / 语音）](#5-接入真实云服务llm--向量--语音)
- [6. 数据层（schema / data / 切库）](#6-数据层schema--data--切库)
- [7. 配置项清单](#7-配置项清单)
- [8. 前端二次开发](#8-前端二次开发)
- [9. 代码风格与提交规范](#9-代码风格与提交规范)
- [10. 测试与 CI](#10-测试与-ci)

---

## 1. 架构总览

```
backend (Spring Boot 模块化单体)            frontend (Vite + React SPA)
┌──────────────────────────────┐          ┌──────────────────────────────┐
│ Controller  →  Service  →  Repository │  │ pages/*  →  api.ts  →  /api/* │
│      ↑统一 ApiResponse / 全局异常       │  │   ↑ react-router 路由          │
│ 可插拔: LlmClient/EmbeddingClient/    │  │   ↑ styles.css 品牌变量        │
│         VoiceClient (@Conditional)    │  │   ↑ voice.ts (Web Speech)     │
└──────────────────────────────┘          └──────────────────────────────┘
        每个业务模块 = 一个 com.kengchacha.<module> 包（未来微服务边界）
```

模块清单（`com.kengchacha.*`）：`common`(基建) · `content` · `refresh` · `quiz` · `ugc` · `assistant` · `growth` · `toolbox` · `guardian` · `media` · `voice` · `recommend`。

---

## 2. 工程约定（必读）

### 2.1 统一响应体 `ApiResponse<T>`
所有 Controller 返回 `ApiResponse<T>`，`code=0` 成功：
```java
public record ApiResponse<T>(int code, String msg, T data) {
    public static <T> ApiResponse<T> ok(T data) { ... }   // code=0
    public static <T> ApiResponse<T> fail(String msg) { ... } // code=1
}
```
前端 `api.ts` 统一解包 `data` 字段，约定 `code !== 0` 视为业务失败。

### 2.2 全局异常 `GlobalExceptionHandler`
`@RestControllerAdvice` 统一兜底，把异常转成 `ApiResponse.fail(...)`，**业务代码直接抛异常即可**，不要在每个方法里 try-catch 拼错误码。校验失败（`@Valid`）也由它转换为可读 msg。

### 2.3 分页 `PageResult<T>`
列表分页统一用 `common.PageResult`（含 `list/total/page/size`），不要各模块自造分页结构。

### 2.4 CORS
开发期跨域在 `common.WebConfig` 统一放行；生产由 Nginx 同源代理，无需后端开 CORS。

### 2.5 命名 / 包结构
- 一个业务一个包；包内再分 `dto/` 子包放出入参视图。
- Entity 用 JPA `@Entity`；对外**绝不直接返回 Entity**，一律转 DTO（`*View` / `*Card` / `*Result`）。
- Controller 只做参数绑定与调用 Service；业务逻辑在 Service。

---

## 3. 后端分包与分层

以 `content`（避坑头条）为例的标准结构：
```
content/
├── Content.java               @Entity 实体
├── ContentTag.java            @Entity 标签
├── ContentRepository.java     extends JpaRepository
├── ContentTagRepository.java
├── ContentService.java        业务逻辑（@Service）
├── ContentController.java      @RestController @RequestMapping("/api/content")
└── dto/
    ├── ContentCard.java       列表卡片视图
    ├── ContentDetail.java     详情视图
    └── TagGroup.java          标签分组
```
分层依赖方向严格单向：`Controller → Service → Repository → Entity`，**不可反向**。

---

## 4. 新增一个后端模块（手把手）

假设新增「**红黑榜 `ranking`**」模块，提供 `GET /api/ranking/top`。

**① 建包**：`com.kengchacha.ranking`

**② 实体** `RankItem.java`
```java
package com.kengchacha.ranking;

import jakarta.persistence.*;
import lombok.Data;

@Data @Entity @Table(name = "rank_item")
public class RankItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int score;
}
```

**③ 仓库** `RankItemRepository.java`
```java
package com.kengchacha.ranking;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RankItemRepository extends JpaRepository<RankItem, Long> {
    List<RankItem> findTop10ByOrderByScoreDesc();
}
```

**④ DTO** `dto/RankView.java`
```java
package com.kengchacha.ranking.dto;
public record RankView(String name, int score) {}
```

**⑤ 服务** `RankingService.java`
```java
package com.kengchacha.ranking;

import com.kengchacha.ranking.dto.RankView;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RankingService {
    private final RankItemRepository repo;
    public RankingService(RankItemRepository repo) { this.repo = repo; }

    public List<RankView> top() {
        return repo.findTop10ByOrderByScoreDesc()
                   .stream().map(r -> new RankView(r.getName(), r.getScore())).toList();
    }
}
```

**⑥ 控制器** `RankingController.java`
```java
package com.kengchacha.ranking;

import com.kengchacha.common.ApiResponse;
import com.kengchacha.ranking.dto.RankView;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ranking")
public class RankingController {
    private final RankingService service;
    public RankingController(RankingService service) { this.service = service; }

    @GetMapping("/top")
    public ApiResponse<List<RankView>> top() {
        return ApiResponse.ok(service.top());
    }
}
```

**⑦ 建表 + 种子**：在 `resources/schema.sql` 加 `create table rank_item(...)`，`resources/data.sql` 插入种子数据（dev 用）。

**⑧ 验证**：`mvn -f backend/pom.xml spring-boot:run` 后 `curl http://localhost:8080/api/ranking/top`。

> 复制任意现成模块（如 `growth`/`refresh` 是最简模板）改名最省事。

---

## 5. 接入真实云服务（LLM / 向量 / 语音）

这是本项目最重要的可插拔设计。三个能力各有一个接口 + 一个 Mock 实现，靠 `@ConditionalOnProperty` 切换。

### 5.1 现状（默认 mock）
```java
public interface LlmClient { String complete(String prompt); }

@Component
@ConditionalOnProperty(name = "kengchacha.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmClient implements LlmClient {
    public String complete(String prompt) { return "（AI 离线规则版）……"; }
}
```

### 5.2 新增云实现（以通义千问为例）
```java
package com.kengchacha.assistant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kengchacha.ai.provider", havingValue = "qwen")
public class QwenLlmClient implements LlmClient {

    @Value("${qwen.api-key:}") private String apiKey;

    @Override
    public String complete(String prompt) {
        // TODO: 用 RestClient/WebClient 调用 DashScope，把 prompt 发出去取回文本
        // return dashScopeClient.chat(apiKey, prompt);
        return "...";
    }
}
```
**关键点**：
1. `havingValue = "qwen"`（**不带** `matchIfMissing`），与 Mock 互斥 —— 同一时刻只有一个 `LlmClient` Bean 生效。
2. 配置 `kengchacha.ai.provider=qwen` + 注入 `QWEN_API_KEY` 即可切换，**Service 层代码完全不改**（它只依赖 `LlmClient` 接口）。
3. `EmbeddingClient`（`kengchacha.ai.embedding=bge`）、`VoiceClient`（`kengchacha.ai.voice=xfyun`）同理，照抄此模式。

### 5.3 切换矩阵
| 接口 | 配置项 | mock（默认） | 生产实现示例 |
|------|------|------|------|
| `LlmClient` | `kengchacha.ai.provider` | 离线规则文本 | `QwenLlmClient` / `DeepSeekLlmClient` |
| `EmbeddingClient` | `kengchacha.ai.embedding` | 字+bigram 哈希向量 | BGE / M3E / 向量化云 API |
| `VoiceClient` | `kengchacha.ai.voice` | 确定性 WAV + SSML | 讯飞 / 阿里云 ASR·TTS |

> 原则：**新增实现，不改接口、不改调用方**。这样一次接入、全局可用，且 dev 永远能离线跑测试。

---

## 6. 数据层（schema / data / 切库）

- **dev**：H2 内存库（`MODE=PostgreSQL` 兼容模式），启动时 `schema.sql` 建表 + `data.sql` 灌种子；`ddl-auto=none`（表结构以脚本为准）。重启清空，便于反复试。
- **新增表**：改 `schema.sql`（建表）与 `data.sql`（种子），保持两者同步；字段命名 `snake_case`，实体用 `@Column` 映射。
- **切 PostgreSQL（生产）**：见《部署指南》第 8.2 节 —— 换驱动、换 dialect、关脚本初始化、用 Flyway/Liquibase 管迁移、`ddl-auto=validate`。
- **向量检索**：生产可把内存向量索引替换为 `pgvector` / Milvus（同样经 `EmbeddingClient` 与检索接口隔离）。

---

## 7. 配置项清单

`application.yml` 中 `kengchacha.*` 自定义项：

| 配置项 | 默认 | 说明 |
|------|------|------|
| `kengchacha.refresh.interval-hours` | `2` | 避坑头条滚动更新间隔（小时） |
| `kengchacha.ai.provider` | `mock` | AI 助手生成层实现选择 |
| `kengchacha.ai.embedding` | `mock` | 向量化实现选择 |
| `kengchacha.ai.voice` | `mock` | 语音 ASR/TTS 实现选择 |

Spring 通用项：`server.port` / `spring.datasource.*` / `spring.profiles.active` / `spring.h2.console.enabled` 等。
> 新增配置建议归到 `kengchacha.*` 命名空间，并可建 `@ConfigurationProperties` 强类型绑定。

---

## 8. 前端二次开发

### 8.1 接口封装 `src/api.ts`
所有请求走这里，统一拼 `/api` 前缀、解包 `ApiResponse.data`、处理错误。新增接口在此加一个函数，**不要在页面里裸 `fetch`**。

### 8.2 新增一个页面 + 路由
1. `src/pages/Ranking.tsx` 写页面组件；
2. `src/App.tsx` 注册路由与导航：
```tsx
import Ranking from './pages/Ranking'
// tabs 数组里加一项（底部 TabBar + PC 侧栏共用）：
{ to: '/ranking', ic: '🏆', label: '红黑榜' }
// Routes 里加：
<Route path="/ranking" element={<Ranking />} />
```
> `tabs` 同时渲染移动端底部 TabBar 与 PC 侧栏；`extra` 数组里的项只在 PC 侧栏出现。

### 8.3 品牌 / 主题变量
全站颜色集中在 `src/styles.css` 的 `:root`，二开换肤改这里即可：
| 变量 | 值 | 含义 |
|------|------|------|
| `--brand` | `#0E9E8E` | 品牌主色（青绿） |
| `--brand-d` | `#0B6E6B` | 品牌深色（顶栏/按钮渐变） |
| `--radar` | `#16D6B4` | 强调薄荷绿 |
| `--ink` / `--ink2` / `--muted` | `#0F172A` / `#475569` / `#94A3B8` | 文字三级 |
| `--g` / `--a` / `--r` | `#22C55E` / `#F59E0B` / `#EF4444` | 低/中/高危语义色 |
| `--bg` / `--card` / `--line` | `#F4F7FA` / `#FFF` / `#E6EBF1` | 背景/卡片/分割线 |

### 8.4 响应式（H5 / PC）
同一套组件，CSS 媒体查询在 `≥980px` 切换为「侧栏 + 宽屏」布局，`<980px` 维持 H5 底部 TabBar。新增页面只要复用既有 class，即自动获得双端适配。

### 8.5 语音 `src/voice.ts`
封装浏览器 Web Speech API（ASR 识别 / TTS 朗读），在线优先；不支持时回退后端 `/api/voice/*`。需要朗读/语音输入时调用这里的封装即可。

---

## 9. 代码风格与提交规范

- **后端**：构造器注入（不用字段 `@Autowired`）；DTO 用 `record`；Lombok `@Data/@Getter` 减样板；不返回 Entity。
- **前端**：函数组件 + Hooks；TS 严格类型；请求集中 `api.ts`；样式走 CSS 变量。
- **提交信息**：约定式提交 `feat: / fix: / docs: / chore: / refactor:`，中文描述清晰可读。
- **分支**：`main` 为发布分支；功能走 `feat/xxx` 分支 + PR。
- **PR 前自检**：后端 `mvn -f backend/pom.xml verify`，前端 `npx tsc --noEmit && npm run build` 全绿（与 CI 一致）。

---

## 10. 测试与 CI

- 后端测试位于 `backend/src/test/...`，`mvn verify` 会执行；新增模块请补 Service 层单测。
- 前端以 `tsc --noEmit` 做类型门禁 + `vite build` 做构建门禁。
- 推送/PR 到 `main` 自动触发 `.github/workflows/ci.yml`（后端打包归档 jar、前端构建归档 dist）。

---

> 出品：**topbat** · 欢迎遵循上述约定提交 PR，保持「接口隔离、离线可跑」的工程基线。
