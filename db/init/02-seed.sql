-- WBS Scaffold 種子資料
-- 組織架構：資訊部 > 資訊科 / 資訊科2

-- 部門
INSERT INTO departments (id, name, parent_id) VALUES
    (1, '資訊部',  NULL),
    (2, '資訊科',  1),
    (3, '資訊科2', 1);
SELECT setval('departments_id_seq', 3);

-- 使用者（密碼皆為 BCrypt，對應明文見 docs/db.md）
INSERT INTO users (id, email, password_hash, display_name, department_id, role) VALUES
    (1, 'director@company.com', '$2a$10$xgrSW1eTL/QUbDRVULbGCuM3WxKIxI1v.47S9nP.RkPUJuylzeJ52', '部長',       1, 'DIRECTOR'),
    (2, 'chief@infotech.com',   '$2a$10$POo1dvjnrqUTekB1oN1MkOCZbEy6/jIMlvOEmY93SYFDALe..7V1O', '資訊科長',   2, 'SECTION_CHIEF'),
    (3, 'leader@infotech.com',  '$2a$10$6h9wB9u4eT3w.hMHmqYwjOISiy8Xm3kQ06Hfho5U9saLB57WDaj8G', '資訊Leader', 2, 'PROJECT_LEADER'),
    (4, 'member1@infotech.com', '$2a$10$ozAKpKl88F2APf79pcdWf.aGvZUzdOgizAfBZGqghk61WXlV4cdb.', '資訊成員一', 2, 'PROJECT_MEMBER'),
    (5, 'member2@infotech.com', '$2a$10$3MTa5RlnTxD60uQnwjOf1ObCa5UmVbje2PEV61j9XDNwPsIPDaxPy', '資訊成員二', 2, 'PROJECT_MEMBER'),
    (6, 'chief2@infotech.com',  '$2a$10$hZUD8PAoIesk3.CuWOvFJOYMy9DHg6o7cKsSDL3W/4zECrQNDjcI.', '資訊科長2',  3, 'SECTION_CHIEF'),
    (7, 'leader2@infotech.com', '$2a$10$unwGWxSdLoVB2qT4P6AeZOzm7cHiYbh/gwpvApOBPx0bIblEJ55Wy', '資訊Leader2',3, 'PROJECT_LEADER'),
    (8, 'member3@infotech.com', '$2a$10$q8.KcOuLpsblGEqF4dLqhukQvBoi50ek35G/rafX42skTBTDQ0W3y', '資訊成員三', 3, 'PROJECT_MEMBER');
SELECT setval('users_id_seq', 8);

-- 系統模板
INSERT INTO wbs_templates (id, name, description, is_system, is_default) VALUES
    (1, '系統開發標準', 'SIT→UAT→PROD 三階段、三層結構標準模板', TRUE, FALSE);
SELECT setval('wbs_templates_id_seq', 1);

-- 模板節點：系統開發標準（template_id=1，三層結構）
-- L1：SIT / UAT / PROD
-- L2（各階段下）：環境建置 / 系統開發 / 系統上線
-- L3（各工作包下）：具體任務
INSERT INTO wbs_template_nodes (id, template_id, parent_id, title, sort_order) VALUES
    -- L1
    ( 1, 1, NULL, 'SIT',  0),
    ( 2, 1, NULL, 'UAT',  1),
    ( 3, 1, NULL, 'PROD', 2),

    -- SIT > L2
    ( 4, 1,  1, '環境建置', 0),
    ( 5, 1,  1, '系統開發', 1),
    ( 6, 1,  1, '系統上線', 2),

    -- SIT > 環境建置 > L3
    ( 7, 1,  4, '主機申請',   0),
    ( 8, 1,  4, '防火牆開通', 1),
    ( 9, 1,  4, '主機佈版',   2),

    -- SIT > 系統開發 > L3
    (10, 1,  5, '功能開發', 0),
    (11, 1,  5, '功能驗測', 1),

    -- SIT > 系統上線 > L3
    (12, 1,  6, '測試執行',   0),
    (13, 1,  6, 'IT 測試報告', 1),

    -- UAT > L2
    (14, 1,  2, '環境建置',   0),
    (15, 1,  2, '使用者測試', 1),
    (16, 1,  2, '系統上線',   2),

    -- UAT > 環境建置 > L3
    (17, 1, 14, '主機申請',   0),
    (18, 1, 14, '防火牆開通', 1),
    (19, 1, 14, '主機佈版',   2),

    -- UAT > 使用者測試 > L3
    (20, 1, 15, '功能驗測', 0),
    (21, 1, 15, 'UAT 報告', 1),

    -- UAT > 系統上線 > L3
    (22, 1, 16, '上線準備', 0),
    (23, 1, 16, '正式上線', 1),

    -- PROD > L2
    (24, 1,  3, '環境建置', 0),
    (25, 1,  3, '系統上線', 1),

    -- PROD > 環境建置 > L3
    (26, 1, 24, '主機申請',   0),
    (27, 1, 24, '防火牆開通', 1),
    (28, 1, 24, '主機佈版',   2),

    -- PROD > 系統上線 > L3
    (29, 1, 25, '正式部署', 0),
    (30, 1, 25, '交易測試', 1),
    (31, 1, 25, '資安檢核', 2);
SELECT setval('wbs_template_nodes_id_seq', 31);

-- 全域快速子項
INSERT INTO wbs_quick_items (id, title, category, sort_order, section_id) VALUES
    (1, '開防火牆',   '常用', 0, NULL),
    (2, '申請環境',   '常用', 1, NULL),
    (3, '部署申請',   '常用', 2, NULL),
    (4, '系統測試',   '常用', 3, NULL),
    (5, '使用者驗收', '常用', 4, NULL),
    (6, '正式上線',   '常用', 5, NULL),
    (7, '文件更新',   '常用', 6, NULL),
    (8, '會議記錄',   '常用', 7, NULL);
SELECT setval('wbs_quick_items_id_seq', 8);

-- 測試專案
INSERT INTO projects (id, name, department_id, owner_id, created_by) VALUES
    (1, '資訊科測試專案',  2, 3, 3),
    (2, '資訊科2測試專案', 3, 7, 7);
SELECT setval('projects_id_seq', 2);

-- 測試專案成員
INSERT INTO project_members (project_id, user_id, assigned_by) VALUES
    (1, 3, 3),
    (1, 4, 3),
    (1, 5, 3),
    (2, 7, 7),
    (2, 8, 7);
