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
| manager_id | bigint FK→users | 部門主管 |
| created_at | timestamp | |

### `users` — 使用者

| 欄位 | 類型 | 說明 |
|---|---|---|
| id | bigserial PK | |
| email | varchar(200) UNIQUE | 登入帳號 |
| password_hash | text | BCrypt |
| display_name | varchar(100) | 顯示名稱 |
| department_id | bigint FK→departments | 所屬部門（nullable） |
| role | varchar CHECK | `ADMIN` / `IT_USER` / `MEMBER` |
| can_create_project | boolean | 是否可建立專案 |
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
| assigned_by | bigint FK→users | 加入者 |
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
| is_system | boolean | 系統模板（唯讀）|
| created_by | bigint FK→users | 建立者（系統模板為 null） |
| created_at | timestamp | |

### `wbs_template_nodes` — 模板節點

| 欄位 | 類型 | 說明 |
|---|---|---|
| id | bigserial PK | |
| template_id | bigint FK→wbs_templates | |
| parent_id | bigint FK→wbs_template_nodes | |
| title | varchar(200) | |
| sort_order | int | |

### `spring_session` / `spring_session_attributes` — Session 儲存

由 Spring Session JDBC 自動管理，不需手動操作。

---

## 種子資料（DataSeeder 自動植入）

### 系統帳號

| Email | 密碼 | 角色 | 說明 |
|---|---|---|---|
| `admin@wbsscaff.com` | `admin1234` | `ADMIN` | 系統管理員，全權限 |
| `it@system.com` | `it1234` | `IT_USER` | IT 稽核，跨部門唯讀 |

### AAA 部門測試帳號

| Email | 密碼 | 角色 | canCreateProject | 職責 |
|---|---|---|:---:|---|
| `manager@aaa.com` | `manager1234` | `MEMBER` | ✅ | AAA 部門主管（dept.manager） |
| `leader@aaa.com` | `leader1234` | `MEMBER` | ✅ | 專案 Leader |
| `member@aaa.com` | `member1234` | `MEMBER` | ❌ | 一般成員 |

### 系統模板

| 名稱 | 說明 |
|---|---|
| 新功能開發 | SIT + PROD 兩階段（功能開發用）|
| 專案開發 | SIT + PROD 兩階段（含三方測試）|

### 預設部門

資訊部、業務部、財務部、人資部、產品部、AAA（測試）

---

## 部門隔離機制

- `Project.department_id` 在建立時自動繼承 `creator.department_id`
- `MEMBER` 查詢專案時使用 `findByMemberOrOwnerAndDepartment`：
  ```sql
  WHERE (owner_id = ? OR member user_id = ?)
    AND (department_id IS NULL OR department_id = ?)
  ```
- `ADMIN` / `IT_USER` 使用 `findAll()`，不受部門限制
