# WBS 管理系統 — 系統說明

> 企業後台 WBS 多人即時協作系統

---

## 第 1 頁｜系統定位

**Work Breakdown Structure（工作分解結構）管理平台**

- 解決跨角色、多部門的專案任務分解與進度追蹤
- 多人同步在線編輯，不需刷新頁面即可看到他人異動
- 角色權限分層，支援部長 → 科長 → Leader → 成員的組織架構

---

## 第 2 頁｜技術架構

```
瀏覽器
  ├── Thymeleaf（SSR 頁面結構）
  └── Vue 3 CDN（CSR 互動層，離線靜態）
         │  REST API（CSRF Token）
         │  WebSocket / STOMP / SockJS（即時協作）
         ▼
Spring Boot 3.4（Java 21）
  ├── Spring MVC — REST API
  ├── Spring WebSocket — 即時廣播
  └── Spring Security — Session 驗證
         │
         ▼
PostgreSQL 16（資料 + Session 儲存）
```

| 層次 | 技術 |
|---|---|
| 後端 | Java 21 + Spring Boot 3.4 |
| 安全 | Spring Security + Spring Session JDBC |
| 即時 | WebSocket + STOMP + SockJS |
| 前端 | Thymeleaf + Vue 3（無建置工具） |
| 資料庫 | PostgreSQL 16（Docker） |

---

## 第 3 頁｜組織架構與角色

```
資訊部（部）
├── 資訊科（科）
│   ├── 科長（SECTION_CHIEF）
│   ├── Leader（PROJECT_LEADER）
│   └── 成員（PROJECT_MEMBER）
└── 資訊科2（科）
    ├── 科長2
    └── ...
部長（DIRECTOR）── 屬於資訊部，查閱全部下屬科
```

| 角色 | 建立專案 | 編輯 WBS | 管理成員 | 跨科查閱 |
|---|:---:|:---:|:---:|:---:|
| DIRECTOR 部長 | ✅ | 限自建 | 限自建 | ✅唯讀 |
| SECTION_CHIEF 科長 | ✅ | ✅本科 | ✅本科 | ❌ |
| PROJECT_LEADER Leader | ✅ | ✅加入的 | ✅自建的 | ❌ |
| PROJECT_MEMBER 成員 | ❌ | ✅加入的 | ❌ | ❌ |

---

## 第 4 頁｜WBS 三層結構

```
SIT（L1 大項）
├── 環境建置（L2 工作包）
│   ├── 主機申請       ← L3：狀態 + 負責人 + 日期 + 備註
│   ├── 防火牆開通
│   └── 主機佈版
├── 系統開發（L2）
│   ├── 功能開發
│   └── 功能驗測
└── 系統上線（L2）
    ├── 測試執行
    └── IT 測試報告
UAT（L1）
PROD（L1）
```

- **L1 / L2**：顯示狀態、負責人、日期（無備註欄）
- **L3**：顯示全部欄位，包含備註
- 最大深度為 3 層，不可再往下新增子節點

---

## 第 5 頁｜WBS 編輯器 UI

**工具列**（編輯模式下顯示）

| 按鈕 | 功能 |
|---|---|
| 🔓 編輯中 / 🔒 鎖定 | 切換編輯狀態（鎖定時自動儲存暫存異動） |
| + 新增大項 | 在根層新增 L1 節點 |
| 🗑 刪除模式 | 所有節點晃動，點選節點確認後刪除 |
| 另存為模板 | 將目前 WBS 結構儲存為本科自訂模板 |
| 💾 儲存* / 儲存 | 有未儲存異動時顯示藍底，送出所有暫存變更 |
| 匯出 ▾ | CSV / XLSX 匯出 |

**節點欄位（每列）**

`▼` 狀態徽章 標題 負責人▾ 📅開始日 📅結束日 [備註（L3）] [+ 子]

---

## 第 6 頁｜即時協作機制

**連線流程**
1. 頁面載入 → 建立 SockJS 連線
2. STOMP CONNECT → 訂閱 `/topic/project/{id}/nodes`、`/cursors`、`/presence`
3. 發送 `/app/project/{id}/join` → 服務端廣播在線列表

**節點異動廣播**
```
使用者改欄位 → 前端暫存 pendingChanges
按「儲存」→ STOMP publish /app/project/{id}/node/update
  → CollabController 驗證權限 → WbsService 持久化
  → convertAndSend /topic/project/{id}/nodes
  → 所有訂閱者收到 NODE_UPDATE 並更新本地 flatNodes
```

**協作游標**：每次滑鼠移動到節點時發送游標位置，各使用者以不同顏色圓點顯示

---

## 第 7 頁｜模板系統

**系統模板**（全員唯讀）
- 系統開發標準：SIT → UAT → PROD 三階段、三層 31 個節點

**科別模板**
- 科長 / Leader 可將現有 WBS 另存為本科模板
- 僅本科成員可見與套用
- 支援複製系統模板再修改

**套用流程**
1. 新建專案（尚無節點）→ 點「套用模板」
2. 選擇模板 → 伺服器端 BFS 多輪建立，保留父子層級
3. 套用後 WBS 完整展開，可在此基礎上修改

---

## 第 8 頁｜快速子項（Quick Items）

- 左側面板顯示可拖曳的預設子項清單（僅編輯模式可見）
- 拖曳至節點 → 新增為子節點（最深到 L3）
- 拖曳至空白區 → 新增為 L1 根節點
- 全域子項（所有人可見）+ 科別子項（本科才能看）
- 科長 / Leader 可管理（新增、修改、刪除）

---

## 第 9 頁｜歷史專案（歸檔）

- 科長 / Leader 可將完成的專案歸檔（`archived = true`）
- 歸檔後對全員強制唯讀，顯示「📦 已歸檔（唯讀）」
- 歷史專案清單：`/projects/history`
- 仍可查看完整 WBS 內容（唯讀模式），支援 CSV / XLSX 匯出

---

## 第 10 頁｜資料庫設計重點

**核心表**
- `departments`：部（parent_id IS NULL）→ 科（parent_id NOT NULL）
- `projects`：繼承建立者所屬部門（`department_id`），記錄 owner 與 created_by
- `wbs_nodes`：自參考樹（`parent_id FK→wbs_nodes`），`sort_order` 控制排序
- `project_members`：複合 PK（project_id + user_id）

**Session 管理**
- Spring Session JDBC 儲存在 `spring_session` 表
- Cookie `SESSION`（HttpOnly），無 JWT

**Schema 管理**
- `db/init/`：首次啟動自動執行（建表 + 種子資料）
- `db/migrate/`：手動執行的增量 SQL，不使用 Flyway

---

## 第 11 頁｜安全機制

| 機制 | 說明 |
|---|---|
| 認證 | Spring Security 表單登入，路徑 `/auth/login` |
| Session | PostgreSQL JDBC Session，HttpOnly Cookie |
| CSRF（REST） | Thymeleaf 自動注入 `_csrf`；Vue fetch 加 `X-CSRF-TOKEN` Header |
| CSRF（WebSocket） | 停用（SockJS 使用 session Cookie 驗證） |
| IDOR 防護 | 頁面層 redirect；API 層呼叫 `canReadProject()` / `canWriteProject()` |
| 歸檔保護 | `canWriteProject()` 第一步就檢查 `isArchived()`，不需其他地方重複判斷 |

---

## 第 12 頁｜本機快速啟動

```bash
# 複製環境設定
cp .env.example .env
# 編輯 .env，設定 DB_PASSWORD

# 啟動資料庫
docker compose up -d

# 編譯並執行
mvn clean install -DskipTests
mvn spring-boot:run

# 開啟瀏覽器
open http://localhost:8080
```

**測試登入**

| 帳號 | 密碼 | 角色 |
|---|---|---|
| `leader@infotech.com` | `test1234` | PROJECT_LEADER |
| `chief@infotech.com` | `test1234` | SECTION_CHIEF |
| `director@company.com` | `test1234` | DIRECTOR |

> 重置資料庫：`docker compose down -v && docker compose up -d`
