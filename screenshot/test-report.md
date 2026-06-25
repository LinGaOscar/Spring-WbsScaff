# WBS 管理系統 — 功能驗測報告

**測試日期：** 2026-06-25
**測試環境：** MSSQL 2022 Docker + Spring Boot 3.4
**測試帳號：** chief@infotech.com（資訊科科長）、chief2@infotech.com（資訊科2科長）

---

## 測試結果總覽

| TC | 方向 | 功能說明（10字） | 結果 | 截圖 |
|---|---|---|---|---|
| TC-01 | ✅ 正 | 登入頁面正常顯示 | PASS | TC01-login-page.png |
| TC-02 | ❌ 反 | 錯誤密碼登入失敗 | PASS | TC02-login-fail.png |
| TC-03 | ✅ 正 | 正確帳密登入成功 | PASS | TC03-login-success.png |
| TC-04 | ✅ 正 | 專案列表正確渲染 | PASS | TC04-project-list.png |
| TC-05 | ✅ 正 | WBS 編輯頁面載入 | PASS | TC05-wbs-page.png |
| TC-06 | ✅ 正 | WBS 新增根節點成功 | PASS | TC06-wbs-add-node.png |
| TC-07 | ✅ 正 | 節點狀態更新儲存 | PASS | TC07-wbs-update-node.png |
| TC-08 | ❌ 反 | 刪除模式震動提示 | PASS | TC08-wbs-delete-mode.png |
| TC-09 | ✅ 正 | 套用模板節點展開 | PASS | TC09-wbs-apply-template.png |
| TC-10 | ✅ 正 | 主模板預覽節點展 | PASS | TC10-template-preview.png |
| TC-11 | ✅ 正 | 子模板48筆全顯示 | PASS | TC11-quick-items.png |
| TC-12 | ✅ 正 | 專案成員列表展開 | PASS | TC12-members.png |
| TC-13 | ✅ 正 | 歷史專案頁面顯示 | PASS | TC13-history.png |
| TC-14 | ❌ 反 | 不存在專案顯示404 | PASS | TC14-not-found.png |
| TC-15 | ❌ 反 | 跨部門存取被重導 | PASS | TC15-cross-dept-denied.png |
| TC-16 | ❌ 反 | 未登入自動重導入 | PASS | TC16-unauthenticated-redirect.png |

---

## 關鍵驗測細節

| 項目 | 驗證值 |
|---|---|
| 登入失敗重導 | `/login?error` |
| 登入成功重導 | `/projects` |
| 節點 CRUD API | POST/PUT/DELETE 全部 HTTP 200 |
| 套用模板 API | HTTP 200，節點完整展開 |
| 系統模板數量 | 4 個 |
| 快速子項數量 | 48 筆 |
| 專案成員 | 3 人（Leader + 成員一、二） |
| 跨部門 API | HTTP 403 |
| 不存在專案頁面 | 自訂 404 HTML（非 Whitelabel） |
| 未登入重導 | `/login` |

---

## 本次修復項目

| 項目 | 說明 |
|---|---|
| 自訂 404 頁 | 新增 `error/404.html` 與 `error/5xx.html`，取代 Spring Boot Whitelabel |
| 專案頁 JSON 錯誤 | `projectDetailPage` 加 try-catch，EntityNotFoundException 改回傳 404 HTML |

---

## 整體結論

**16 / 16 PASS — 系統所有功能正常，無缺陷。**

截圖存放位置：`screenshot/` 目錄（與本報告同層）
