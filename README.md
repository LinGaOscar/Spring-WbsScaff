# Spring-WbsScaff

企業後台 WBS（Work Breakdown Structure）多人即時協作系統。

## 技術架構

| 層次 | 技術 |
|---|---|
| 後端 | Java 21 + Spring Boot 3.4 + Spring MVC |
| 安全 | Spring Security 6 + Spring Session JDBC（HttpOnly Cookie，無 JWT） |
| 即時協作 | Spring WebSocket + STOMP + SockJS |
| 前端 | Thymeleaf 3 + Vue 3 CDN（離線靜態） |
| 資料庫 | SQL Server 2022（Docker） |
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
SQL Server 2022（資料 + Session 儲存）
```

### 角色與權限

組織架構：部（Division）→ 科（Section），部長屬於部、其餘角色屬於科。

| 角色 | 說明 | 建立專案 | 編輯 WBS | 管理成員 | 管理模板 | 跨科查閱 |
|---|---|:---:|:---:|:---:|:---:|:---:|
| `DIRECTOR` | 部長 | ✅（掛在部） | 僅自建專案 | 僅自建專案 | ❌ | ✅（唯讀） |
| `SECTION_CHIEF` | 科長 | ✅ | ✅（本科） | ✅（本科） | ✅ | ❌ |
| `PROJECT_LEADER` | 專案 Leader | ✅ | 限加入的專案 | 限自己的專案 | ✅ | ❌ |
| `PROJECT_MEMBER` | 專案 Member | ❌ | 限加入的專案 | ❌ | ❌ | ❌ |

### 部門隔離

- 專案建立時自動繼承建立者所屬科（部長建立則掛在部層級）
- 科長、Leader 只能看本科專案；Member 只能看被加入的專案
- 部長可查閱下屬所有科的專案，但不得編輯（自建的除外）
- 歸檔的專案對全員強制唯讀

---

## 快速啟動

```bash
# 1. 複製 .env 並設定密碼
cp .env.example .env   # 編輯 MSSQL_SA_PASSWORD

# 2. 啟動 SQL Server（首次自動執行 db/mssql/ 建立 schema 與種子資料）
docker compose up -d

# 3. 編譯並啟動應用
mvn clean install -DskipTests
java -jar target/spring-wbsscaff-*.jar
```

預設埠：`http://localhost:8080`

> **重置資料庫**：`docker compose down -v && docker compose up -d`

---

## 功能說明

### 認證

| 功能 | 說明 |
|---|---|
| 登入 | 電子郵件 + 密碼，路徑 `/login` |
| 登出 | 清除 Session，重導至登入頁 |
| Session | 儲存於 SQL Server `SPRING_SESSION` 表（HttpOnly Cookie） |
| 未登入保護 | 存取任何頁面自動重導至 `/login` |

---

### 專案管理

| 功能 | 說明 | 可操作角色 |
|---|---|---|
| 專案列表 | 依角色顯示可見專案卡片（`/projects`） | 全員 |
| 新增專案 | 填寫名稱與負責人，建立後自動綁定所屬科 | DIRECTOR / SECTION_CHIEF / PROJECT_LEADER |
| 歸檔專案 | 完成後移至歷史，全員強制唯讀 | SECTION_CHIEF / PROJECT_LEADER（本科） |
| 還原歸檔 | 將歷史專案重新移回進行中 | SECTION_CHIEF / PROJECT_LEADER（本科） |
| 歷史專案 | 查看所有已歸檔的舊專案（`/projects/history`） | 全員 |
| 不存在專案 | 返回自訂 404 頁面，非 Whitelabel Error | — |

---

### WBS 編輯器

路徑：`/projects/{id}`

#### 節點操作

| 功能 | 說明 |
|---|---|
| 新增根節點（L1） | 點工具列「＋新增大項」 |
| 新增子節點 | 在節點上點 ＋，最多 3 層（L1→L2→L3） |
| 編輯節點 | 標題、負責人、開始日期、結束日期、狀態 |
| 備註欄位 | 僅 L3 最末層節點可填寫備註 |
| 刪除節點 | 切換刪除模式 → 節點出現紅色遮罩 + 震動動畫 → 點選後 `confirm()` 確認刪除 |
| 節點狀態 | 未開始（NOT_STARTED）/ 進行中（IN_PROGRESS）/ 已完成（DONE） |

#### 工具列功能

| 功能 | 說明 |
|---|---|
| 儲存 | 所有欄位異動暫存於前端，按「儲存」批次送出 |
| 刪除模式 | 切換全節點刪除狀態，再次點擊退出 |
| 套用模板 | 從系統或自訂模板一鍵匯入節點結構，覆蓋現有內容 |
| 另存為模板 | 將目前 WBS 結構存成可重複使用的自訂主模板 |
| 拖曳快速子項 | 從左側面板拖曳常用子項至節點下方 |
| 匯出 | 將 WBS 匯出為 JSON / CSV / XLSX 下載備份 |

#### 即時協作（WebSocket）

| 功能 | 說明 |
|---|---|
| 節點即時同步 | 任一方新增 / 修改 / 刪除，所有在線成員畫面即時更新 |
| 協作者顯示 | 右上角顯示目前在線協作者頭像與游標位置 |
| 歸檔鎖定 | 歸檔後工具列鎖定，所有操作皆停用 |
| 權限保護 | 每次 WebSocket 操作皆驗證 `canWriteProject()` |

---

### 主模板管理

路徑：`/templates`

| 功能 | 說明 | 可操作角色 |
|---|---|---|
| 查看系統模板 | 4 個系統預設模板（系統開發標準 / 功能新增 / EOS系統下線 / 專案開發），唯讀預覽節點結構 | 全員 |
| 查看自訂模板 | 本科建立的自訂模板，可展開預覽節點 | 全員（本科） |
| 新增空白模板 | 點「＋ 新增模板」輸入名稱，直接進入編輯頁 | SECTION_CHIEF / PROJECT_LEADER |
| 編輯模板節點 | 進入 `/templates/{id}/edit`，新增 / 修改 / 刪除節點，支援拖曳排序（`/templates/{id}/edit`） | SECTION_CHIEF / PROJECT_LEADER（本科） |
| L3 備註欄位 | L3 節點可設定預設備註說明，套用至專案時一併帶入 | SECTION_CHIEF / PROJECT_LEADER |
| 改名模板 | 修改自訂模板名稱 | SECTION_CHIEF / PROJECT_LEADER（本科） |
| 刪除模板 | 移除不再使用的自訂模板（系統模板不可刪） | SECTION_CHIEF / PROJECT_LEADER（本科） |
| 匯出 JSON | 下載模板節點結構為 JSON 備份 | SECTION_CHIEF / PROJECT_LEADER |

---

### 子模板管理（快速子項）

路徑：`/admin/quick-items`（SECTION_CHIEF / PROJECT_LEADER 限定）

| 功能 | 說明 |
|---|---|
| 查看快速子項 | 全域 48 筆系統預設子項 + 本科自訂子項 |
| 新增子項 | 填寫名稱、分類、需求單號、排序 |
| 編輯子項 | 修改現有子項的名稱、分類、需求單號（全域子項不可改） |
| 刪除子項 | 移除本科自訂子項 |
| 拖曳使用 | 在 WBS 頁面左側面板拖曳至節點下方快速新增子任務 |

> 子項以科（Section）為共用單位，同科所有成員可見相同清單。

---

### 專案人員管理

路徑：`/admin/members`（SECTION_CHIEF / PROJECT_LEADER 限定）

| 功能 | 說明 |
|---|---|
| 查看成員 | 列出本科每個專案的成員清單 |
| 新增成員 | 將科內帳號加入指定專案 |
| 移除成員 | 將成員從專案中移除，立即失去 WBS 編輯權限 |
| 變更負責人 | 修改專案的主要負責 Leader |

---

### 錯誤處理

| 狀況 | 行為 |
|---|---|
| 存取不存在的專案 | 顯示自訂 404 頁面（含側欄導覽，可返回首頁） |
| 無存取權限 | 重導至 `/projects`，不顯示錯誤訊息 |
| 伺服器錯誤 | 顯示自訂 5xx 頁面 |
| 未登入 | 任何頁面自動重導至 `/login` |

---

## 測試帳號

密碼皆為 `test1234`

| Email | 角色 | 所屬 |
|---|---|---|
| `director@company.com` | DIRECTOR | 資訊部 |
| `chief@infotech.com` | SECTION_CHIEF | 資訊科 |
| `leader@infotech.com` | PROJECT_LEADER | 資訊科 |
| `member1@infotech.com` | PROJECT_MEMBER | 資訊科 |
| `member2@infotech.com` | PROJECT_MEMBER | 資訊科 |
| `chief2@infotech.com` | SECTION_CHIEF | 資訊科2 |
| `leader2@infotech.com` | PROJECT_LEADER | 資訊科2 |
| `member3@infotech.com` | PROJECT_MEMBER | 資訊科2 |

---

## 文件

- [`docs/db.md`](docs/db.md) — 資料庫結構與測試資料
- [`docs/dev.md`](docs/dev.md) — 開發環境、API 端點、安全機制
- [`CLAUDE.md`](CLAUDE.md) — 開發架構指引（供 Claude Code 使用）
