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
INSERT INTO wbs_templates (id, name, description, is_system) VALUES
    (1, '新功能開發', 'SIT+PROD 兩階段流程（功能開發用）', TRUE),
    (2, '專案開發',   'SIT+PROD 兩階段流程（專案開發用）', TRUE);
SELECT setval('wbs_templates_id_seq', 2);

-- 模板節點：新功能開發（template_id=1，兩層結構）
INSERT INTO wbs_template_nodes (id, template_id, parent_id, title, sort_order) VALUES
    ( 1, 1, NULL,  'SIT 階段',          0),
    ( 2, 1,    1,  '主機申請',          0),
    ( 3, 1,    1,  '防火牆設定',        1),
    ( 4, 1,    1,  '連線測試',          2),
    ( 5, 1,    1,  '執行環境/驅動安裝', 3),
    ( 6, 1,    1,  '功能測試',          4),
    ( 7, 1,    1,  '整合測試',          5),
    ( 8, 1,    1,  'IT 測試報告',       6),
    ( 9, 1, NULL,  'PROD 階段',         1),
    (10, 1,    9,  '主機申請',          0),
    (11, 1,    9,  '防火牆設定',        1),
    (12, 1,    9,  '連線測試',          2),
    (13, 1,    9,  '執行環境/驅動安裝', 3),
    (14, 1,    9,  'USER 測試報告',     4),
    (15, 1,    9,  '資安檢核表',        5),
    (16, 1,    9,  '正式部署',          6),
    (17, 1,    9,  '交易測試',          7);

-- 模板節點：專案開發（template_id=2，兩層結構）
INSERT INTO wbs_template_nodes (id, template_id, parent_id, title, sort_order) VALUES
    (18, 2, NULL,  'SIT 階段',          0),
    (19, 2,   18,  '主機申請',          0),
    (20, 2,   18,  '防火牆設定',        1),
    (21, 2,   18,  '連線測試',          2),
    (22, 2,   18,  '執行環境/驅動安裝', 3),
    (23, 2,   18,  '功能測試',          4),
    (24, 2,   18,  '整合測試',          5),
    (25, 2,   18,  'IT 測試報告',       6),
    (26, 2, NULL,  'PROD 階段',         1),
    (27, 2,   26,  '主機申請',          0),
    (28, 2,   26,  '防火牆設定',        1),
    (29, 2,   26,  '連線測試',          2),
    (30, 2,   26,  '執行環境/驅動安裝', 3),
    (31, 2,   26,  '黑箱測試',          4),
    (32, 2,   26,  '白箱測試',          5),
    (33, 2,   26,  '第三方測試',        6),
    (34, 2,   26,  'USER 測試報告',     7),
    (35, 2,   26,  '資安檢核表',        8),
    (36, 2,   26,  '正式部署',          9),
    (37, 2,   26,  '交易測試',         10);
SELECT setval('wbs_template_nodes_id_seq', 37);

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
