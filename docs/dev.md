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
# 1. 啟動 DB
docker compose up -d db

# 2. 設定環境變數（或直接帶參數）
export DB_PASSWORD=wbsscaff_dev_2026

# 3. 啟動（預設 port 8080；如 8080 被占用可換 port）
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=8081 \
    --spring.datasource.url=jdbc:postgresql://localhost:5432/wbsscaff \
    --spring.datasource.username=wbsscaff \
    --spring.datasource.password=wbsscaff_dev_2026"
```

啟動後 DataSeeder 自動植入種子資料（idempotent）。

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
└── wbs/            # WBS 節點 CRUD

src/main/resources/
├── application.yml
├── static/
│   ├── css/        # app.css、wbs.css
│   └── js/         # Vue、STOMP、SockJS、xlsx（離線）、wbs-editor.js
└── templates/      # Thymeleaf 頁面
    ├── fragments/  # header、sidebar、footer
    ├── admin/      # 使用者管理頁
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

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/api/users/me` | 任何登入者 | 取得當前使用者資訊 |
| GET | `/api/users` | ADMIN / IT_USER | 列出所有使用者 |
| POST | `/api/users` | ADMIN | 建立使用者 |
| PUT | `/api/users/{id}` | ADMIN | 更新使用者 |
| DELETE | `/api/users/{id}` | ADMIN | 停用使用者 |
| PATCH | `/api/users/{id}/can-create-project` | ADMIN | 設定建專案權限 |

### 專案

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/api/projects` | 任何登入者 | 列出可見專案（依角色/部門過濾） |
| POST | `/api/projects` | canCreateProject / ADMIN | 建立專案 |
| GET | `/api/projects/{id}` | 成員 / ADMIN / IT_USER | 取得專案資訊 |
| GET | `/api/projects/{id}/members` | 成員 / ADMIN / IT_USER | 列出成員 |
| POST | `/api/projects/{id}/members` | ADMIN / 負責人 | 新增成員 |
| DELETE | `/api/projects/{id}/members/{userId}` | ADMIN / 負責人 | 移除成員 |
| PATCH | `/api/projects/{id}/owner` | ADMIN | 更換負責人 |

### WBS 節點

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/api/projects/{id}/nodes` | 成員 / ADMIN / IT_USER | 取得所有節點 |
| POST | `/api/projects/{id}/nodes` | 成員 / ADMIN | 建立節點 |
| PUT | `/api/projects/{id}/nodes/{nodeId}` | 成員 / ADMIN | 更新節點 |
| DELETE | `/api/projects/{id}/nodes/{nodeId}` | 成員 / ADMIN | 刪除節點 |
| PATCH | `/api/projects/{id}/nodes/reorder` | 成員 / ADMIN | 重新排序 |
| POST | `/api/projects/{id}/nodes/init` | 成員 / ADMIN | 套用模板初始化 |
| POST | `/api/projects/{id}/save-as-template` | 成員 / ADMIN | 另存為模板 |

> IT_USER 僅限 GET，所有寫入操作返回 `SecurityException`。

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
  - 頁面層（`/projects/{id}`）：非成員 redirect 到 `/projects`
  - API 層（節點 CRUD）：`checkMember()` 驗證成員資格
  - 節點操作額外驗證 `node.project_id == projectId`（防止跨專案操作）
- **部門隔離**：MEMBER 只能見同部門專案；IT_USER / ADMIN 可跨部門

---

## 常見問題

**Q: 啟動後看不到任何專案？**
A: 確認登入帳號是否已加入專案成員，或使用 `admin@wbsscaff.com` 確認資料。

**Q: `users_role_check` constraint 錯誤？**
A: Role enum 新增值後需手動更新 PostgreSQL CHECK constraint：
```sql
ALTER TABLE users DROP CONSTRAINT users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('ADMIN','IT_USER','MEMBER'));
```

**Q: WebSocket 連線失敗？**
A: 確認 `/ws/**` 路徑在 SecurityConfig 中已設為 `permitAll()`，STOMP 認證由 WebSocketSecurityConfig 負責。
