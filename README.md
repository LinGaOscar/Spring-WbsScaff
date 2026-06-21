# Spring-WbsScaff

企業後台 WBS（Work Breakdown Structure）多人即時協作系統。

## 技術架構

| 層次 | 技術 |
|---|---|
| 後端 | Java 21 + Spring Boot 3.4 + Spring MVC |
| 安全 | Spring Security 6 + Spring Session JDBC（HttpOnly Cookie，無 JWT） |
| 即時協作 | Spring WebSocket + STOMP + SockJS |
| 前端 | Thymeleaf 3 + Vue 3 CDN（離線靜態） |
| 資料庫 | PostgreSQL 16 |
| 部署 | Docker Compose |

### 架構流程

```
瀏覽器
  │  Thymeleaf 頁面（SSR）
  │  Vue 3（CSR 互動層）
  │  SockJS / STOMP（即時協作）
  ▼
Spring MVC Controller
  │  Spring Security（Session Cookie 驗證）
  ▼
Service Layer
  │  JPA / Hibernate
  ▼
PostgreSQL（資料 + Session 儲存）
```

### 角色與權限

| 角色 | 說明 | 建立專案 | 管理 WBS | 查閱所有資料 | 使用者管理 |
|---|---|:---:|:---:|:---:|:---:|
| `ADMIN` | 系統管理員 | ✅ | ✅ | ✅ | ✅ |
| `IT_USER` | IT 稽核（唯讀） | ❌ | ❌（唯讀） | ✅ | ❌（唯讀） |
| `MEMBER` | 一般成員 | 依設定 | 限所屬專案 | ❌ | ❌ |

### 部門隔離

- `MEMBER` 只能看到**同部門**的專案與 WBS
- 建立專案時自動繼承建立者所屬部門
- 跨部門資料由 `IT_USER` 或 `ADMIN` 稽核

---

## 快速啟動

### 本機開發

```bash
# 1. 複製 .env 並設定資料庫密碼
cp .env.example .env   # 編輯 DB_PASSWORD

# 2. 啟動 PostgreSQL（Docker）
docker compose up -d db

# 3. 啟動應用
mvn spring-boot:run
```

預設埠：`http://localhost:8080`

### Docker 完整部署

```bash
docker compose up -d
```

---

## 功能說明

### 登入 / 登出
- 路徑：`/login`、`/auth/logout`
- Session 儲存於 PostgreSQL（`spring_session` 表）
- Cookie：`SESSION`（HttpOnly）

### 專案管理
- 列表：`/projects` — 僅顯示使用者所屬部門且已加入的專案
- 建立：需 `canCreateProject = true` 或 `ADMIN`
- 成員管理：`ADMIN` 或專案負責人可新增 / 移除成員

### WBS 編輯器
- 路徑：`/projects/{id}`
- 支援樹狀節點（多層）、負責人、日期、備註、狀態循環
- 即時協作：節點異動即時同步至所有在線成員，顯示協作者游標
- 匯出：CSV / XLSX

### 模板系統
- 系統模板（唯讀）：「新功能開發」、「專案開發」
- 自訂模板：另存為模板、套用至新專案

### 使用者管理
- 路徑：`/admin/users`（`ADMIN` 完整操作；`IT_USER` 唯讀）

---

## 文件

- [`docs/db.md`](docs/db.md) — 資料庫結構與測試資料
- [`docs/dev.md`](docs/dev.md) — 開發環境、API 端點、安全機制
