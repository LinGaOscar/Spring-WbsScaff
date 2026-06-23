# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## 常用指令

```bash
# 啟動資料庫
docker compose up -d

# 重置資料庫（清除所有資料重新初始化）
docker compose down -v && docker compose up -d

# 編譯並打包（跳過測試）
mvn clean install -DskipTests -q

# 啟動應用（開發時）
mvn spring-boot:run

# 打包後直接執行 JAR（正式部署流程）
java -jar target/spring-wbsscaff-*.jar

# 執行手動 migration（不重置資料庫）
docker exec spring-wbsscaff-db-1 psql -U wbsscaff -d wbsscaff -f /path/to/migration.sql
```

> **重要**：Thymeleaf 模板與靜態資源（JS/CSS）都打包進 JAR，每次修改這些檔案後必須重新執行 `mvn clean install -DskipTests` 才會生效。

---

## 架構核心觀念

### 混合式 SSR + CSR

- **Thymeleaf**（SSR）負責頁面結構、注入 Spring Security 資料（CSRF token、PROJECT_ID、READ_ONLY 狀態）
- **Vue 3**（CSR，離線 CDN，不使用建置工具）負責 WBS 互動層
- Vue app 掛載在 `<div id="app">` 之上，透過 Thymeleaf 注入的 JS 全域變數（`CSRF_HEADER`、`CSRF_TOKEN`、`PROJECT_ID`、`READ_ONLY`）取得初始資料

### WBS 編輯器前端架構（`wbs-editor.js`）

- **單一元件**：`WbsNodeComp`（`defineComponent`）遞迴渲染，接收 `node`、`locked`、`depth`、`deleteMode` props
- **3 層深度限制**：`depth` prop 從根傳入（初始為 `1`），元件在 `depth >= 3` 時禁止新增子節點、禁止拖曳 drop
- **延遲儲存**：所有欄位異動（title、status、owner、date、notes）先存入 `pendingChanges` Map，不立即送出 WebSocket；按「儲存」按鈕或切換鎖定時才批次送出
- **刪除模式**：切換 `deleteMode` ref，所有節點套用 CSS shake 動畫 + 透明 overlay；點 overlay 彈出 `confirm()` 再發 WebSocket delete
- **備註欄位**：只在 `depth >= 3`（L3）的節點顯示

### WebSocket 即時協作流程

```
前端 STOMP publish → CollabController (@MessageMapping)
  → WbsService.updateNode() 持久化
  → broker.convertAndSend("/topic/project/{id}/nodes") 廣播
前端訂閱 /topic/project/{id}/nodes → 依 NODE_UPDATE / NODE_CREATE / NODE_DELETE 更新 flatNodes
```

- **CSRF 處理雙軌**：
  - REST API（`fetch`）：`X-CSRF-TOKEN` Header
  - WebSocket：CSRF 停用（SockJS 使用 HttpOnly Cookie session 驗證）
- CollabController 的每個 `@MessageMapping` 都呼叫 `checkWriteAccess()`，使用與 REST 相同的 `ProjectService.canWriteProject()` 判斷

### 權限邏輯核心

- `ProjectService.canReadProject()` / `canWriteProject()`：所有權限判斷的唯一入口
- 歸檔的專案（`project.isArchived() == true`）對全員強制唯讀
- `User.canManageSection()`：`SECTION_CHIEF || PROJECT_LEADER`，用於模板與快速子項管理
- 部門隔離：DIRECTOR 屬於部（`parent_id IS NULL`），查閱時比對子科的 `parent.id`

### 資料庫 Schema 管理

- **不使用** Flyway / Liquibase
- `db/init/`：Docker 首次啟動自動執行（`01-schema.sql` 建表、`02-seed.sql` 種子資料）
- `db/migrate/`：需手動執行的增量 migration（命名格式：`NN-description.sql`）
- `ddl-auto: none`，不依賴 Hibernate 自動建表

### 測試帳號（密碼皆為 `test1234`）

| Email | 角色 | 所屬 |
|---|---|---|
| `director@company.com` | `DIRECTOR` | 資訊部 |
| `chief@infotech.com` | `SECTION_CHIEF` | 資訊科 |
| `leader@infotech.com` | `PROJECT_LEADER` | 資訊科 |
| `member1@infotech.com` | `PROJECT_MEMBER` | 資訊科 |
| `member2@infotech.com` | `PROJECT_MEMBER` | 資訊科 |
| `chief2@infotech.com` | `SECTION_CHIEF` | 資訊科2 |
| `leader2@infotech.com` | `PROJECT_LEADER` | 資訊科2 |
| `member3@infotech.com` | `PROJECT_MEMBER` | 資訊科2 |

---

## 常見陷阱

- **靜態資源改了沒生效**：需重新 `mvn clean install` 打包進 JAR 再重啟
- **新增 migration 後資料不對**：`db/migrate/` 的 SQL 不會自動執行，需手動 `psql` 套用
- **WBS `_open` 狀態**：`buildTree()` 每次重建都會將 `_open` 重設為 `true`；toggling close 後，若 `flatNodes` 有任何更新就會重展開（已知行為）
- **WebSocket 節點 ID 傳遞**：`CollabController` 的 update / delete 使用 STOMP `@Header("nodeId")`，不是 payload body
