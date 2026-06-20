# 坑查查 · 部署指南（DEPLOYMENT）

> 适用版本：v1.0.0 / 后端 `kengchacha-backend-2.0.0` / 前端 `kengchacha-frontend-2.0.0`
> 架构：**前后端分离**，后端模块化单体（Spring Boot），前端静态产物（Vite 构建）。

---

## 目录
- [1. 架构与端口](#1-架构与端口)
- [2. 环境要求](#2-环境要求)
- [3. 本地开发运行](#3-本地开发运行)
- [4. 构建产物](#4-构建产物)
- [5. 单机部署（裸机 / VM）](#5-单机部署裸机--vm)
- [6. Docker 部署](#6-docker-部署)
- [7. docker-compose 一键编排](#7-docker-compose-一键编排)
- [8. 生产配置（Profile / 数据库 / 三方能力）](#8-生产配置profile--数据库--三方能力)
- [9. Nginx 反向代理示例](#9-nginx-反向代理示例)
- [10. 健康检查、优雅停机与日志](#10-健康检查优雅停机与日志)
- [11. CI/CD](#11-cicd)
- [12. 常见问题（FAQ）](#12-常见问题faq)

---

## 1. 架构与端口

```
                 ┌────────────────────────┐
   浏览器 ──────▶ │  Nginx / 静态服务器       │  :80 / :443
   (H5 + PC)     │  - 托管前端 dist 静态资源  │
                 │  - /api/* 反向代理后端     │
                 └───────────┬────────────┘
                             │ /api/*
                             ▼
                 ┌────────────────────────┐
                 │  Spring Boot 后端         │  :8080
                 │  模块化单体（11 个模块）    │
                 │  虚拟线程 + 优雅停机        │
                 └───────────┬────────────┘
                             ▼
                 ┌────────────────────────┐
                 │  数据库                   │
                 │  dev: H2 内存(PG 兼容)    │
                 │  prod: PostgreSQL(+pgvector)
                 └────────────────────────┘
```

| 组件 | 端口（默认） | 说明 |
|------|------|------|
| 前端（dev） | `5173` | Vite dev server，`/api` 代理到 `:8080` |
| 前端（prod） | `80/443` | 静态 `dist/` 由 Nginx 托管 |
| 后端 | `8080` | Spring Boot；可用 `--server.port=` 改 |
| H2 控制台（dev） | `8080/h2-console` | 仅开发期，生产务必关闭 |
| 数据库（prod） | `5432` | PostgreSQL |

---

## 2. 环境要求

| 工具 | 版本 | 用途 |
|------|------|------|
| JDK | **21**（Temurin/Adoptium 推荐） | 后端编译运行（使用虚拟线程） |
| Maven | 3.9+ | 后端构建（或用仓库内 `mvnw`，如有） |
| Node.js | **20 LTS+** | 前端构建（CI 用 20，本机 ≥18 亦可） |
| npm | 9+ | 前端依赖 |
| Docker | 24+（可选） | 容器化部署 |
| PostgreSQL | 14+（可选，生产） | 生产数据库；如用向量检索装 `pgvector` |

校验：
```bash
java -version     # 21.x
mvn -v            # Apache Maven 3.9.x
node -v && npm -v # v20.x / 10.x
```

---

## 3. 本地开发运行

```bash
# 1) 后端（:8080）
mvn -f backend/pom.xml spring-boot:run
# 健康自检：
curl http://localhost:8080/api/refresh/countdown

# 2) 前端（:5173）— 另开一个终端
cd frontend
npm install
npm run dev        # 打开 http://localhost:5173
```
> 前端通过 Vite 代理把 `/api/*` 转发到 `http://localhost:8080`，**务必先启动后端**。

---

## 4. 构建产物

### 4.1 后端可执行 JAR
```bash
mvn -f backend/pom.xml -DskipTests package
# 产物：backend/target/kengchacha-backend-2.0.0.jar （fat jar，内嵌 Tomcat）
java -jar backend/target/kengchacha-backend-2.0.0.jar
```

### 4.2 前端静态资源
```bash
npm --prefix frontend ci          # CI 用 ci；本地首次可用 install
npm --prefix frontend run build
# 产物：frontend/dist/  （index.html + assets/*，纯静态）
```
> 生产环境前端 **不需要 Node 运行时**，只需把 `dist/` 交给任意静态服务器（Nginx / OSS / CDN）。

---

## 5. 单机部署（裸机 / VM）

适合一台 Linux 服务器（Ubuntu/CentOS）：

```bash
# === 后端 ===
# 1. 上传 kengchacha-backend-2.0.0.jar 到 /opt/kengchacha/
# 2. 用 systemd 托管（见下）

# === 前端 ===
# 1. 上传 dist/ 到 /var/www/kengchacha
# 2. Nginx 托管 + 反代 /api（见第 9 节）
```

**systemd 服务**：`/etc/systemd/system/kengchacha.service`
```ini
[Unit]
Description=Kengchacha Backend
After=network.target

[Service]
User=app
WorkingDirectory=/opt/kengchacha
# 生产建议显式指定 profile 与外部配置
ExecStart=/usr/bin/java -jar /opt/kengchacha/kengchacha-backend-2.0.0.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=file:/opt/kengchacha/application-prod.yml
SuccessExitStatus=143
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now kengchacha
sudo systemctl status kengchacha
journalctl -u kengchacha -f          # 看日志
```

---

## 6. Docker 部署

仓库未内置 Dockerfile，可直接复制以下内容。

### 6.1 后端 `backend/Dockerfile`（多阶段构建）
```dockerfile
# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/kengchacha-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

### 6.2 前端 `frontend/Dockerfile`（构建后用 Nginx 托管）
```dockerfile
# ---- build ----
FROM node:20-alpine AS build
WORKDIR /web
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- run ----
FROM nginx:1.27-alpine
COPY --from=build /web/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

### 6.3 前端 `frontend/nginx.conf`
```nginx
server {
  listen 80;
  server_name _;
  root /usr/share/nginx/html;
  index index.html;

  # SPA 路由回退（react-router 前端路由）
  location / {
    try_files $uri $uri/ /index.html;
  }

  # 反向代理后端（容器网络内用服务名 backend）
  location /api/ {
    proxy_pass http://backend:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }

  # 静态资源缓存
  location /assets/ {
    expires 30d;
    add_header Cache-Control "public, immutable";
  }
}
```

构建并运行：
```bash
docker build -t kengchacha-backend ./backend
docker build -t kengchacha-frontend ./frontend
docker network create kcc
docker run -d --name backend  --network kcc kengchacha-backend
docker run -d --name frontend --network kcc -p 80:80 kengchacha-frontend
```

---

## 7. docker-compose 一键编排

根目录 `docker-compose.yml`（含 PostgreSQL，生产形态）：
```yaml
services:
  db:
    image: pgvector/pgvector:pg16     # 自带 pgvector，便于切向量检索
    environment:
      POSTGRES_DB: kengchacha
      POSTGRES_USER: kcc
      POSTGRES_PASSWORD: ${DB_PASSWORD:-change-me}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U kcc -d kengchacha"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build: ./backend
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/kengchacha
      SPRING_DATASOURCE_USERNAME: kcc
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-change-me}
      KENGCHACHA_AI_PROVIDER: mock     # 切云模型见第 8 节
    expose: ["8080"]

  frontend:
    build: ./frontend
    depends_on: [backend]
    ports: ["80:80"]

volumes:
  pgdata:
```
```bash
DB_PASSWORD=your-strong-pw docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

> 注：切 PostgreSQL 时需提供 PG 版 `schema.sql`/`data.sql` 或开启 `ddl-auto`，见第 8.2 节。

---

## 8. 生产配置（Profile / 数据库 / 三方能力）

### 8.1 配置优先级
Spring Boot 配置可被命令行参数、环境变量、外部 `application-prod.yml` 覆盖。推荐：**代码内只放 dev 默认值，生产值用环境变量或外部文件注入**。

### 8.2 切 PostgreSQL（生产）
新增 `application-prod.yml`（不入库或入 secret）：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://db:5432/kengchacha
    username: kcc
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate          # 生产用迁移工具(Flyway/Liquibase)管表结构
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  sql:
    init:
      mode: never                 # 生产关闭脚本自动初始化
  h2:
    console:
      enabled: false              # 生产务必关闭 H2 控制台
```
并在 `backend/pom.xml` 增加 PostgreSQL 驱动：
```xml
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <scope>runtime</scope>
</dependency>
```
> 建库脚本：把 `backend/src/main/resources/schema.sql` 适配为 PG 方言后，交由 Flyway（`db/migration/V1__init.sql`）管理。

### 8.3 三方能力开关（核心可插拔点）
后端通过 `@ConditionalOnProperty` 在「离线 Mock」与「云服务实现」间切换，**默认全 mock，零密钥可跑**：

| 配置项 | 默认 | 生产可选值 | 说明 |
|------|------|------|------|
| `kengchacha.ai.provider` | `mock` | `qwen` / `deepseek` … | AI 助手生成层（`LlmClient`） |
| `kengchacha.ai.embedding` | `mock` | `bge` / `m3e` … | 向量化（`EmbeddingClient`） |
| `kengchacha.ai.voice` | `mock` | `xfyun` / `aliyun` … | 语音 ASR/TTS（`VoiceClient`） |
| `kengchacha.refresh.interval-hours` | `2` | 任意正整数 | 头条滚动更新间隔 |

环境变量写法（kebab → 大写下划线）：
```bash
KENGCHACHA_AI_PROVIDER=qwen
KENGCHACHA_AI_VOICE=xfyun
# 对应密钥按你新增实现里读取的 key 注入，例如：
QWEN_API_KEY=sk-xxxx
```
> 接入真实云服务的代码改造步骤见《二次开发指南》第 5 节。

---

## 9. Nginx 反向代理示例（裸机部署）

`/etc/nginx/conf.d/kengchacha.conf`：
```nginx
server {
  listen 80;
  server_name kengchacha.example.com;

  root /var/www/kengchacha;     # 前端 dist
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;   # SPA 路由回退
  }

  location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 60s;
  }
}
```
HTTPS 建议用 Certbot 一键签发：`sudo certbot --nginx -d kengchacha.example.com`。

---

## 10. 健康检查、优雅停机与日志

- **健康检查**：`GET /api/refresh/countdown` 返回 `code:0` 即存活（轻量、无副作用）。
  - 如需标准探针，可加 `spring-boot-starter-actuator`，暴露 `/actuator/health`（K8s liveness/readiness）。
- **优雅停机**：`server.shutdown: graceful` 已开启；systemd `SuccessExitStatus=143` 已兼容 SIGTERM。
- **日志**：默认输出到 stdout（容器友好）。裸机用 `journalctl -u kengchacha -f`；生产可配 `logging.file.name` 或接入 ELK/Loki。
- **JVM 参数（容器内推荐）**：`-XX:MaxRAMPercentage=75 -XX:+UseG1GC`，加到 `ENTRYPOINT` 的 java 参数即可。

---

## 11. CI/CD

仓库已内置 GitHub Actions：`.github/workflows/ci.yml`

- **后端 job**：JDK 21 + `mvn verify`（编译·测试·打包）→ 归档 `kengchacha-backend-jar`
- **前端 job**：Node 20 + `npm ci` → `tsc --noEmit`（类型检查）→ `vite build` → 归档 `kengchacha-frontend-dist`
- 触发：push/PR 到 `main`、手动 `workflow_dispatch`；同分支并发自动取消旧任务。

**扩展为 CD（示意）**：在 CI 通过后追加 job，用 `docker/build-push-action` 推镜像到 registry，再经 SSH/ArgoCD/K8s 滚动发布。Release 产物可由 tag 触发 `softprops/action-gh-release` 自动上传 jar。

---

## 12. 常见问题（FAQ）

| 现象 | 原因 / 处理 |
|------|------|
| `Port 8080 was already in use` | 已有进程占用；换端口 `--server.port=8081` 或停掉旧进程 |
| 前端能开但接口 404/跨域 | 未启动后端，或反代 `/api` 未配置；dev 检查 `vite.config.ts` 代理 |
| PowerShell `Invoke-RestMethod` 中文乱码 | PS5.1 解码问题；用浏览器或 `curl.exe` 验证，非接口问题 |
| 重启后数据丢失 | dev 用 H2 **内存库**，重启即清空；生产请切 PostgreSQL（第 8.2 节） |
| H2 控制台暴露在公网 | 生产必须 `spring.h2.console.enabled=false` |
| 接口都正常但 AI/语音是"离线版" | 默认 `*.provider=mock`；按第 8.3 节切云服务 |

---

> 出品：**topbat** · 文档随版本演进，欢迎 PR 修订。
