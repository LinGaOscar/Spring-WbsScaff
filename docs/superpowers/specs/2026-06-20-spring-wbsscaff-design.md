# Spring-WbsScaff 系統設計文件

**日期：** 2026-06-20
**版本：** 1.0

---

## 1. 專案目標

WbsScaff（純前端單頁 HTML）的進階後台版本。目標為具備企業後台管理、部門層級權限、多人即時協作的 WBS 管理平台。

---

## 2. 技術棧

| 層級 | 技術 |
|---|---|
| 語言 / 執行環境 | Java 21（Virtual Threads） |
| 框架 | Spring Boot 3.4 |
| 架構模式 | Spring MVC（Controller → Service → Repository） |
| 專案管理 | Maven（pom.xml） |
| 模板引擎 | Thymeleaf 3（Header / 頁面內容 / Footer 片段） |
| 前端響應式 | Vue 3（本地靜態 `vue.global.prod.min.js`，離線可用） |
| 即時協作 | Spring WebSocket + STOMP（SockJS fallback） |
| 安全 | Spring Security + Spring Session（PostgreSQL 後端）+ HttpOnly Cookie |
| ORM | Spring Data JPA + Hibernate |
| 資料庫 | PostgreSQL 16 |
| 部署 | Docker Compose（App + PostgreSQL） |
| 靜態 Vendor | `src/main/resources/static/js/`（vue、stomp、sockjs、xlsx） |

---

## 3. 架構總覽

```
┌─────────────────────────────────────────────────┐
│              Browser（離線可用）                  │
│  Thymeleaf HTML + Vue 3（local）+ STOMP Client  │
└────────────────────┬────────────────────────────┘
                     │ HTTP / WebSocket
┌────────────────────▼────────────────────────────┐
│           Spring Boot 3（Java 21）               │
│  ┌──────────┐ ┌──────────┐ ┌─────────────────┐  │
│  │Spring MVC│ │WebSocket │ │Spring Security  │  │
│  │Thymeleaf │ │  STOMP   │ │Spring Session   │  │
│  └──────────┘ └──────────┘ └─────────────────┘  │
│  ┌──────────────────────────────────────────┐   │
│  │         Spring Data JPA（Hibernate）      │   │
│  └──────────────────────────────────────────┘   │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│              PostgreSQL 16（Docker）             │
└─────────────────────────────────────────────────┘
```

---

## 4. 模組結構（MVC 分層）

```
src/main/java/com/wbsscaff/
├── WbsScaffApplication.java
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   └── SecurityConfig.java
├── user/
│   ├── UserController.java
│   ├── UserService.java
│   ├── UserRepository.java
│   ├── User.java
│   └── UserDto.java
├── department/
│   └── Department.java          # Entity only，無 Controller（UI 由 seed 預設）
├── project/
│   ├── ProjectController.java
│   ├── ProjectService.java
│   ├── ProjectRepository.java
│   ├── ProjectMemberRepository.java
│   ├── Project.java
│   └── ProjectMember.java
├── wbs/
│   ├── WbsController.java
│   ├── WbsService.java
│   ├── WbsRepository.java
│   └── WbsNode.java
├── template/
│   ├── TemplateController.java
│   ├── TemplateService.java
│   ├── TemplateRepository.java
│   ├── TemplateNodeRepository.java
│   ├── WbsTemplate.java
│   └── WbsTemplateNode.java
├── collab/
│   ├── CollabController.java
│   ├── CollabService.java
│   └── WebSocketConfig.java
└── common/
    ├── GlobalExceptionHandler.java
    └── ApiResponse.java
```

---

## 5. Thymeleaf 模板結構

```
src/main/resources/
├── templates/
│   ├── fragments/
│   │   ├── header.html          # 頂部導覽列（含登入使用者資訊）
│   │   ├── footer.html          # 頁尾
│   │   └── sidebar.html         # 側邊選單（依角色顯示）
│   ├── auth/
│   │   └── login.html
│   ├── admin/
│   │   └── users.html           # 使用者管理（ADMIN）
│   ├── project/
│   │   ├── list.html            # 專案列表
│   │   └── detail.html          # WBS 編輯器 + 即時協作主頁面
│   ├── template/
│   │   └── list.html            # 模板管理頁
│   └── error/
│       └── 403.html
└── static/
    └── js/
        ├── vue.global.prod.min.js
        ├── stomp.min.js
        ├── sockjs.min.js
        └── xlsx.min.js
```

頁面片段引用方式：
```html
<div th:replace="~{fragments/header :: header}"></div>
<!-- 頁面內容 -->
<div th:replace="~{fragments/footer :: footer}"></div>
```

---

## 6. 資料庫 Schema

```sql
-- 部門（預先 seed，無管理 UI，日後由組織圖同步）
CREATE TABLE departments (
  id          BIGSERIAL PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  manager_id  BIGINT REFERENCES users(id),
  created_at  TIMESTAMP DEFAULT NOW()
);

-- 使用者
CREATE TABLE users (
  id                  BIGSERIAL PRIMARY KEY,
  email               VARCHAR(200) UNIQUE NOT NULL,
  password_hash       VARCHAR(255),
  display_name        VARCHAR(100) NOT NULL,
  department_id       BIGINT REFERENCES departments(id),
  role                VARCHAR(20) NOT NULL DEFAULT 'MEMBER', -- ADMIN | MEMBER
  can_create_project  BOOLEAN DEFAULT FALSE,
  enabled             BOOLEAN DEFAULT TRUE,
  created_at          TIMESTAMP DEFAULT NOW()
);

-- 專案
CREATE TABLE projects (
  id            BIGSERIAL PRIMARY KEY,
  name          VARCHAR(200) NOT NULL,
  department_id BIGINT REFERENCES departments(id),
  owner_id      BIGINT REFERENCES users(id),   -- 專案負責人
  created_by    BIGINT REFERENCES users(id),
  created_at    TIMESTAMP DEFAULT NOW(),
  updated_at    TIMESTAMP DEFAULT NOW()
);

-- 專案成員
CREATE TABLE project_members (
  project_id   BIGINT REFERENCES projects(id) ON DELETE CASCADE,
  user_id      BIGINT REFERENCES users(id),
  assigned_by  BIGINT REFERENCES users(id),
  joined_at    TIMESTAMP DEFAULT NOW(),
  PRIMARY KEY (project_id, user_id)
);

-- WBS 節點（層數無限制）
CREATE TABLE wbs_nodes (
  id          BIGSERIAL PRIMARY KEY,
  project_id  BIGINT REFERENCES projects(id) ON DELETE CASCADE,
  parent_id   BIGINT REFERENCES wbs_nodes(id) ON DELETE CASCADE,
  title       VARCHAR(300) NOT NULL,
  owner       VARCHAR(100),
  start_date  DATE,
  end_date    DATE,
  status      VARCHAR(20) DEFAULT 'NOT_STARTED', -- NOT_STARTED | IN_PROGRESS | DONE
  notes       TEXT,
  sort_order  INT DEFAULT 0,
  created_at  TIMESTAMP DEFAULT NOW(),
  updated_at  TIMESTAMP DEFAULT NOW()
);

-- WBS 模板
CREATE TABLE wbs_templates (
  id           BIGSERIAL PRIMARY KEY,
  name         VARCHAR(200) NOT NULL,
  description  TEXT,
  owner_id     BIGINT REFERENCES users(id),      -- NULL = 系統模板
  is_system    BOOLEAN DEFAULT FALSE,
  is_default   BOOLEAN DEFAULT FALSE,            -- 該用戶的預設自訂模板
  cloned_from  BIGINT REFERENCES wbs_templates(id),
  created_at   TIMESTAMP DEFAULT NOW(),
  updated_at   TIMESTAMP DEFAULT NOW()
);

-- 模板節點（樹狀，無 project 關聯）
CREATE TABLE wbs_template_nodes (
  id          BIGSERIAL PRIMARY KEY,
  template_id BIGINT REFERENCES wbs_templates(id) ON DELETE CASCADE,
  parent_id   BIGINT REFERENCES wbs_template_nodes(id) ON DELETE CASCADE,
  title       VARCHAR(300) NOT NULL,
  sort_order  INT DEFAULT 0
);

-- Spring Session（框架自動建立，無需手動建立）
-- spring_session, spring_session_attributes
```

---

## 7. 權限模型

| 角色 | 判斷方式 |
|---|---|
| 系統管理員 | `users.role = 'ADMIN'` |
| 部門主管 | `departments.manager_id = 登入使用者 id` |
| 一般成員 | 其餘 |

| 動作 | 允許條件 |
|---|---|
| 建立專案 | `can_create_project = true` 或 `ADMIN` |
| 變更專案負責人 | 該部門主管或 `ADMIN` |
| 指派專案成員 | 專案 `owner_id` 或部門主管或 `ADMIN` |
| 編輯 WBS 節點 | 是該專案的 `project_members` 成員 |
| 管理使用者 | `ADMIN` |
| 刪除系統模板 | ❌ 任何人皆不可 |
| 管理自訂模板 | 模板 `owner_id` 本人 |

---

## 8. API 端點

### Auth
| Method | Path | 說明 |
|---|---|---|
| POST | `/auth/login` | 登入（建立 Spring Session） |
| POST | `/auth/logout` | 登出（銷毀 Session） |

### 使用者管理
| Method | Path | 說明 |
|---|---|---|
| GET | `/api/users` | 列出使用者 |
| POST | `/api/users` | 新增使用者（ADMIN） |
| PUT | `/api/users/{id}` | 編輯使用者 |
| DELETE | `/api/users/{id}` | 停用使用者（ADMIN） |
| PATCH | `/api/users/{id}/can-create-project` | 主管指派建立專案權限 |

### 專案管理
| Method | Path | 說明 |
|---|---|---|
| GET | `/api/projects` | 我的專案列表 |
| POST | `/api/projects` | 新增專案（需 can_create_project） |
| PUT | `/api/projects/{id}` | 編輯專案資訊 |
| DELETE | `/api/projects/{id}` | 刪除專案（ADMIN / 主管） |
| PATCH | `/api/projects/{id}/owner` | 變更負責人（主管 / ADMIN） |
| GET | `/api/projects/{id}/members` | 列出專案成員 |
| POST | `/api/projects/{id}/members` | 新增成員（負責人 / 主管） |
| DELETE | `/api/projects/{id}/members/{userId}` | 移除成員 |

### WBS 節點
| Method | Path | 說明 |
|---|---|---|
| GET | `/api/projects/{id}/nodes` | 取得完整樹狀結構 |
| POST | `/api/projects/{id}/nodes` | 新增節點 |
| PUT | `/api/projects/{id}/nodes/{nodeId}` | 更新節點 |
| DELETE | `/api/projects/{id}/nodes/{nodeId}` | 刪除節點（含子樹） |
| PATCH | `/api/projects/{id}/nodes/reorder` | 拖曳排序 |
| POST | `/api/projects/{id}/nodes/init` | 套用模板初始化節點 |
| POST | `/api/projects/{id}/nodes/import-template` | 上傳模板 JSON 初始化節點 |
| POST | `/api/projects/{id}/save-as-template` | 將目前 WBS 另存為自訂模板 |

### WBS 模板
| Method | Path | 說明 |
|---|---|---|
| GET | `/api/templates` | 列出可用模板（系統 + 個人） |
| POST | `/api/templates/clone/{templateId}` | 複製系統模板建立自訂模板 |
| PUT | `/api/templates/{id}` | 編輯自訂模板（僅限本人） |
| DELETE | `/api/templates/{id}` | 刪除自訂模板（僅限本人，系統模板不可刪） |
| PATCH | `/api/templates/{id}/set-default` | 設為我的預設模板 |
| GET | `/api/templates/{id}/export` | 下載模板 JSON |

### 匯出（報表）
| Method | Path | 說明 |
|---|---|---|
| GET | `/api/projects/{id}/export/csv` | 匯出 CSV |
| GET | `/api/projects/{id}/export/xlsx` | 匯出 XLSX（前端 SheetJS 產生） |

### Thymeleaf 頁面路由
| Path | View | 說明 |
|---|---|---|
| `/login` | `auth/login` | 登入頁 |
| `/projects` | `project/list` | 專案列表 |
| `/projects/{id}` | `project/detail` | WBS 編輯器 + 協作 |
| `/admin/users` | `admin/users` | 使用者管理（ADMIN） |
| `/templates` | `template/list` | 模板管理 |

---

## 9. 即時協作設計（WebSocket + STOMP）

### 連線設定
```
端點：/ws（SockJS fallback）
Application prefix：/app
Broker prefix：/topic
驗證：WebSocket 握手時驗證 Spring Session Cookie
用戶顏色：依 userId hash 指派，Session 內固定
```

### STOMP Topics

**節點變更** `/topic/project/{id}/nodes`
```json
{
  "type": "NODE_UPDATE | NODE_CREATE | NODE_DELETE",
  "nodeId": 123,
  "payload": { "title": "...", "status": "IN_PROGRESS" },
  "operator": { "userId": 1, "displayName": "Oscar" },
  "timestamp": "2026-06-20T10:00:00"
}
```

**游標位置** `/topic/project/{id}/cursors`
```json
{
  "userId": 1,
  "displayName": "Oscar",
  "color": "#4A90D9",
  "hoveringNodeId": 42,
  "editingNodeId": null
}
```

**上線/離線** `/topic/project/{id}/presence`
```json
{
  "type": "JOIN | LEAVE",
  "userId": 1,
  "displayName": "Oscar",
  "color": "#4A90D9"
}
```

### 衝突處理
Last Write Wins：兩人同時編輯同一節點，以最後送出的變更為準，Server 廣播後所有人同步。

### Vue 3 協作 UI
| 事件 | UI 呈現 |
|---|---|
| 其他人 hover 節點 | 節點左側出現對方頭像色塊 |
| 其他人正在編輯 | 輸入框顯示彩色邊框 + 名字標籤 |
| 節點變更廣播 | Vue data 即時更新，無需重整 |
| 人員加入/離開 | 右上角 presence bar 更新 |

---

## 10. 模板流程

```
【自訂模板建立入口】

入口 1：複製系統模板
  系統模板 ──「複製並編輯」──► 自訂模板

入口 2：從專案另存
  專案啟用後 ──「匯入他人模板 JSON」──► 套用至專案節點
                                         └──「另存為我的自訂模板」

【建立專案時的起始選擇】
  ┌─────────────────┬──────────────────┬───────────┐
  │  選擇系統模板   │  選擇我的自訂模板 │ 匯入 JSON │
  └────────┬────────┴────────┬──────────┴─────┬─────┘
           └─────────────────┴────────────────┘
                             │
                     套用為專案起始節點
```

---

## 11. 部門說明

- `departments` 資料表保留，啟動時由 `data.sql` seed 預設部門資料
- **無管理 UI**，待日後組織圖總表完成後透過同步機制寫入
- 使用者的 `department_id` 由 ADMIN 在使用者管理頁設定

---

## 12. 安全設計

- **Spring Session + PostgreSQL**：Session 狀態存 DB，非記憶體
- **HttpOnly + SameSite=Strict Cookie**：JS 無法讀取，防 XSS + CSRF
- **密碼**：BCrypt hash 儲存
- **WebSocket**：握手時驗證 Spring Session，未登入拒絕連線
- **系統模板保護**：Service 層判斷 `is_system = true` 一律拒絕刪除/編輯
- **SSO 擴充點**：`SecurityConfig` 預留 OAuth2 設定區塊，日後可啟用

---

## 13. Docker Compose 結構

```yaml
services:
  app:
    build: .
    ports: ["8080:8080"]
    depends_on: [db]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/wbsscaff

  db:
    image: postgres:16
    environment:
      POSTGRES_DB: wbsscaff
      POSTGRES_USER: wbsscaff
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```
