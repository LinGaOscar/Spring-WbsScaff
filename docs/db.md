# 資料庫說明

## 連線資訊（本機開發）

| 項目 | 值 |
|---|---|
| Host | `localhost:5432` |
| Database | `wbsscaff` |
| Username | `wbsscaff` |
| Password | 見 `.env` 的 `DB_PASSWORD` |

---

## 資料表結構

### `departments` — 部門

| 欄位 | 類型 | 說明 |
|---|---|---|
| id | bigserial PK | |
| name | varchar(100) | 部門名稱 |
| parent_id | bigint FK→departments | 上層部門（null = 部，non-null = 科） |
| created_at | timestamp | |

### `users` — 使用者

| 欄位 | 類型 | 說明 |
|---|---|---|
| id | bigserial PK | |
| email | varchar(200) UNIQUE | 登入帳號 |
| password_hash | text | BCrypt |
| display_name | varchar(100) | 顯示名稱 |
| department_id | bigint FK→departments | 所屬部門（nullable） |
| role | varchar CHECK | `DIRECTOR` / `SECTION_CHIEF` / `PROJECT_LEADER` / `PROJECT_MEMBER` |
| enabled | boolean | 帳號是否啟用 |
| created_at | timestamp | |

### `projects` — 專案

| 欄位 | 類型 | 說明 |
|---|---|---|
| id | bigserial PK | |
| name | varchar(200) | 專案名稱 |
| department_id | bigint FK→departments | 所屬部門（建立時自動繼承建立者部門） |
| owner_id | bigint FK→users | 專案負責人 |
| created_by | bigint FK→users | 建立者 |
| created_at | timestamp | |
| updated_at | timestamp | |

### `project_members` — 專案成員（複合 PK）

| 欄位 | 類型 | 說明 |
|---|---|---|
| project_id | bigint FK→projects | |
| user_id | bigint FK→users | |
| assigned_by | bigint FK→users | 指派者 |
| assigned_at | timestamp | |

### `wbs_nodes` — WBS 節點

| 欄位 | 類型 | 說明 |
|---|---|---|
| id | bigserial PK | |
| project_id | bigint FK→projects | |
| parent_id | bigint FK→wbs_nodes | 父節點（nullable = 根節點） |
| title | varchar(200) | 節點標題 |
| owner | varchar(100) | 負責人 |
| start_date | date | 開始日 |
| end_date | date | 結束日 |
| notes | text | 備註 |
| status | varchar CHECK | `NOT_STARTED` / `IN_PROGRESS` / `DONE` |
| sort_order | int | 同層排序 |
| created_at | timestamp | |
| updated_at | timestamp | |

### `wbs_templates` — WBS 模板

| 欄位 | 類型 | 說明 |
|---|---|---|
| id | bigserial PK | |
| name | varchar(200) | 模板名稱 |
| description | text | 說明 |
| is_system | boolean | 系統模板（不可刪除） |
| section_id | bigint FK→departments | 所屬科（null = 系統模板，全員可用） |
| owner_id | bigint FK→users | 建立者 |
| cloned_from | bigint | 複製來源模板 ID |
| is_default | boolean | 是否為預設模板 |
| created_at | timestamp | |

### `wbs_template_nodes` — 模板節點

| 欄位 | 類型 | 說明 |
|---|---|---|
| id | bigserial PK | |
| template_id | bigint FK→wbs_templates | |
| parent_id | bigint FK→wbs_template_nodes | |
| title | varchar(200) | |
| sort_order | int | |

### `wbs_quick_items` — 快速子項

| 欄位 | 類型 | 說明 |
|---|---|---|
| id | bigserial PK | |
| title | varchar(200) | 子項標題 |
| category | varchar(100) | 分類 |
| section_id | bigint FK→departments | 所屬科（null = 全域，所有人可見） |
| sort_order | int | 排序 |

### `spring_session` / `spring_session_attributes` — Session 儲存

由 Spring Session JDBC 自動管理，不需手動操作。

---

## 種子資料（DataSeeder 自動植入）

### 組織架構

```
資訊部（部）
├── 資訊科（科）
└── 資訊科2（科）
```

### 測試帳號

> 所有帳號密碼統一為 **`test1234`**

| Email | 密碼 | 角色 | 所屬 | 說明 |
|---|---|---|---|---|
| `director@company.com` | `test1234` | `DIRECTOR` | 資訊部 | 部長，可查閱下屬科所有專案（唯讀），可自建專案 |
| `chief@infotech.com` | `test1234` | `SECTION_CHIEF` | 資訊科 | 科長，管理資訊科所有專案與成員 |
| `leader@infotech.com` | `test1234` | `PROJECT_LEADER` | 資訊科 | 專案 Leader，可建立與管理自己的專案 |
| `member1@infotech.com` | `test1234` | `PROJECT_MEMBER` | 資訊科 | 一般成員，僅能操作被加入的專案 |
| `member2@infotech.com` | `test1234` | `PROJECT_MEMBER` | 資訊科 | 一般成員 |
| `chief2@infotech.com` | `test1234` | `SECTION_CHIEF` | 資訊科2 | 科長 |
| `leader2@infotech.com` | `test1234` | `PROJECT_LEADER` | 資訊科2 | 專案 Leader |
| `member3@infotech.com` | `test1234` | `PROJECT_MEMBER` | 資訊科2 | 一般成員 |

### 系統模板（全員可用）

| 名稱 | 說明 |
|---|---|
| 系統開發標準 | SIT → UAT → PROD 三階段、三層結構標準模板 |

**模板結構（3 層）**
- L1：SIT / UAT / PROD
- L2（各階段）：環境建置 / 系統開發（或使用者測試）/ 系統上線
- L3（各工作包）：主機申請、防火牆開通、主機佈版、功能開發、功能驗測…等

### 測試專案

| 專案名稱 | 所屬科 | 負責人 | 成員 |
|---|---|---|---|
| 資訊科測試專案 | 資訊科 | leader | member1、member2 |
| 資訊科2測試專案 | 資訊科2 | leader2 | member3 |

---

## 部門隔離機制

- `Project.department_id` 建立時自動繼承建立者所屬部門
- 部長（`DIRECTOR`）屬於部層級，可查閱下屬科所有專案但不可編輯；自建專案可編輯
- 科長（`SECTION_CHIEF`）只能見本科專案，可指派任意使用者為成員
- Leader / Member 只能見被加入的專案（或本科所有專案 for Leader）
- 詳細權限邏輯：`ProjectService.canReadProject()` / `canWriteProject()`
