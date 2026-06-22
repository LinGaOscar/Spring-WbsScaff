# 開發說明

## 環境需求

| 工具 | 版本 |
|---|---|
| Java | 21 |
| Maven | 3.9+ |
| Docker | 24+ |
| PostgreSQL | 16（透過 Docker） |

---

## 本機啟動

```bash
# 1. 複製 .env 並設定資料庫密碼
cp .env.example .env   # 編輯 DB_PASSWORD

# 2. 啟動 PostgreSQL
docker compose up -d

# 3. 編譯並啟動應用
mvn clean install -DskipTests
mvn spring-boot:run
```

`.env` 會被 `spring-dotenv` 自動讀取，不需手動 export 環境變數。

啟動後 DataSeeder 自動植入種子資料（idempotent，僅在 DB 空白時執行）。

---

## 專案結構

```
src/main/java/com/wbsscaff/
├── auth/           # Spring Security 設定、UserDetailsService
├── collab/         # WebSocket 協作：CollabController、CollabService、訊息 DTO
├── common/         # ApiResponse、DataSeeder、GlobalExceptionHandler
├── config/         # WebSocket 安全設定
├── department/     # 部門實體與 Repository
├── project/        # 專案 CRUD、成員管理
├── template/       # WBS 模板系統
├── user/           # 使用者管理
└── wbs/            # WBS 節點 CRUD、快速子項

src/main/resources/
├── application.yml
├── static/
│   ├── css/        # app.css、wbs.css
│   └── js/         # Vue、STOMP、SockJS、xlsx（離線）、wbs-editor.js
└── templates/      # Thymeleaf 頁面
    ├── fragments/  # header、sidebar、footer
    ├── admin/      # 快速子項管理頁
    ├── project/    # 專案列表、WBS 編輯器
    └── template/   # 模板管理頁
```

---

## API 端點

### 認證

| 方法 | 路徑 | 說明 |
|---|---|---|
| GET | `/login` | 登入頁 |
| POST | `/auth/login` | 表單登入（Spring Security） |
| POST | `/auth/logout` | 登出 |

### 使用者

| 方法 | 路徑 | 說明 |
|---|---|---|
| GET | `/api/users/me` | 取得當前使用者資訊 |
| GET | `/api/users` | 列出所有使用者（任何登入者） |

### 專案

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/api/projects` | 任何登入者 | 列出可見專案（依角色過濾） |
| POST | `/api/projects` | DIRECTOR / SECTION_CHIEF / PROJECT_LEADER | 建立專案 |
| GET | `/api/projects/{id}` | 可讀該專案者 | 取得專案資訊 |
| GET | `/api/projects/{id}/members` | 可讀該專案者 | 列出成員 |
| POST | `/api/projects/{id}/members` | 科長（本科）/ 專案 owner | 新增成員 |
| DELETE | `/api/projects/{id}/members/{userId}` | 科長（本科）/ 專案 owner | 移除成員 |
| PATCH | `/api/projects/{id}/owner` | 科長（本科）/ 專案 owner | 更換負責人 |

### WBS 節點

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/api/projects/{id}/nodes` | 可讀該專案者 | 取得所有節點 |
| POST | `/api/projects/{id}/nodes` | 可寫該專案者 | 建立節點 |
| PUT | `/api/projects/{id}/nodes/{nodeId}` | 可寫該專案者 | 更新節點 |
| DELETE | `/api/projects/{id}/nodes/{nodeId}` | 可寫該專案者 | 刪除節點 |
| PATCH | `/api/projects/{id}/nodes/reorder` | 可寫該專案者 | 重新排序 |
| POST | `/api/projects/{id}/nodes/init` | 可寫該專案者 | 套用模板初始化 |
| POST | `/api/projects/{id}/save-as-template` | SECTION_CHIEF / PROJECT_LEADER | 另存為模板 |
| POST | `/api/projects/{id}/apply-template/{templateId}` | 可寫該專案者 | 套用模板 |

> **可讀／可寫判斷**：見 `ProjectService.canReadProject()` / `canWriteProject()`

### 快速子項

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/api/quick-items` | 任何登入者 | 取得可見快速子項（全域 + 本科） |
| POST | `/api/quick-items` | SECTION_CHIEF / PROJECT_LEADER | 新增 |
| PUT | `/api/quick-items/{id}` | SECTION_CHIEF / PROJECT_LEADER | 修改 |
| DELETE | `/api/quick-items/{id}` | SECTION_CHIEF / PROJECT_LEADER | 刪除 |

### 即時協作（WebSocket / STOMP）

| 端點 | 說明 |
|---|---|
| `/ws` | SockJS 連線端點 |
| `/app/project/{id}/join` | 加入專案（廣播 presence JOIN） |
| `/app/project/{id}/leave` | 離開專案（廣播 presence LEAVE） |
| `/app/project/{id}/cursor` | 發送游標位置 |
| `/app/project/{id}/node/create` | 建立節點（廣播給所有成員） |
| `/app/project/{id}/node/update` | 更新節點 |
| `/app/project/{id}/node/delete` | 刪除節點 |
| `/topic/project/{id}/nodes` | 訂閱：節點變更 |
| `/topic/project/{id}/cursors` | 訂閱：游標同步 |
| `/topic/project/{id}/presence` | 訂閱：在線狀態 |

---

## 安全機制

- **Session**：Spring Session JDBC，儲存在 PostgreSQL `spring_session` 表
- **CSRF**：Thymeleaf 表單自動注入 `_csrf`；Vue AJAX 使用 `X-CSRF-TOKEN` Header
- **WebSocket CSRF**：禁用（SockJS 使用 HttpOnly Cookie，session 層已驗證）
- **IDOR 防護**：
  - 頁面層（`/projects/{id}`）：無讀取權限者 redirect 到 `/projects`
  - API 層：`canReadProject()` / `canWriteProject()` 驗證角色與部門

---

## 常見問題

**Q: 啟動後看不到任何專案？**
A: 確認登入帳號是否已加入專案成員，或確認帳號所屬科別有建立專案。

**Q: WebSocket 連線失敗？**
A: 確認 `/ws/**` 路徑在 SecurityConfig 中已設為 `permitAll()`，STOMP 認證由 WebSocketSecurityConfig 負責。

**Q: `.env` 沒有被讀取？**
A: 確認 `.env` 在專案根目錄（與 `pom.xml` 同層）。`spring-dotenv` 依賴已加入 `pom.xml`，會在 Spring Boot 啟動時自動載入。
