-- WBS Scaffold 種子資料 (SQL Server)
-- 組織架構：資訊部 > 資訊科 / 資訊科2

-- 部門
SET IDENTITY_INSERT departments ON;
INSERT INTO departments (id, name, parent_id) VALUES
    (1, N'資訊部',  NULL),
    (2, N'資訊科',  1),
    (3, N'資訊科2', 1);
SET IDENTITY_INSERT departments OFF;
DBCC CHECKIDENT('departments', RESEED, 3);

-- 使用者（密碼統一為 test1234，BCrypt $2b$10$）
SET IDENTITY_INSERT users ON;
INSERT INTO users (id, email, password_hash, display_name, department_id, role) VALUES
    (1, N'director@company.com', N'$2b$10$A6tAs/0xEks8XeKUpdZP9OER8ZDKYEv9Tt42cQxGYnqHklF/zjaHm', N'部長',       1, N'DIRECTOR'),
    (2, N'chief@infotech.com',   N'$2b$10$A6tAs/0xEks8XeKUpdZP9OER8ZDKYEv9Tt42cQxGYnqHklF/zjaHm', N'資訊科長',   2, N'SECTION_CHIEF'),
    (3, N'leader@infotech.com',  N'$2b$10$A6tAs/0xEks8XeKUpdZP9OER8ZDKYEv9Tt42cQxGYnqHklF/zjaHm', N'資訊Leader', 2, N'PROJECT_LEADER'),
    (4, N'member1@infotech.com', N'$2b$10$A6tAs/0xEks8XeKUpdZP9OER8ZDKYEv9Tt42cQxGYnqHklF/zjaHm', N'資訊成員一', 2, N'PROJECT_MEMBER'),
    (5, N'member2@infotech.com', N'$2b$10$A6tAs/0xEks8XeKUpdZP9OER8ZDKYEv9Tt42cQxGYnqHklF/zjaHm', N'資訊成員二', 2, N'PROJECT_MEMBER'),
    (6, N'chief2@infotech.com',  N'$2b$10$A6tAs/0xEks8XeKUpdZP9OER8ZDKYEv9Tt42cQxGYnqHklF/zjaHm', N'資訊科長2',  3, N'SECTION_CHIEF'),
    (7, N'leader2@infotech.com', N'$2b$10$A6tAs/0xEks8XeKUpdZP9OER8ZDKYEv9Tt42cQxGYnqHklF/zjaHm', N'資訊Leader2',3, N'PROJECT_LEADER'),
    (8, N'member3@infotech.com', N'$2b$10$A6tAs/0xEks8XeKUpdZP9OER8ZDKYEv9Tt42cQxGYnqHklF/zjaHm', N'資訊成員三', 3, N'PROJECT_MEMBER');
SET IDENTITY_INSERT users OFF;
DBCC CHECKIDENT('users', RESEED, 8);

-- 系統模板
SET IDENTITY_INSERT wbs_templates ON;
INSERT INTO wbs_templates (id, name, description, is_system, is_default) VALUES
    (1, N'系統開發標準', N'SIT→UAT→PROD 三階段、三層結構標準模板', 1, 0),
    (2, N'功能新增',     N'現有系統新增功能，SIT→UAT→PROD 三階段標準流程', 1, 0),
    (3, N'EOS系統下線',  N'End of Service，系統退場下線，SIT→UAT→PROD 三階段', 1, 0),
    (4, N'專案開發',     N'全新系統建置，SIT→UAT→PROD 三階段完整開發流程', 1, 0);
SET IDENTITY_INSERT wbs_templates OFF;
DBCC CHECKIDENT('wbs_templates', RESEED, 4);

-- 模板節點：系統開發標準（template_id=1，三層結構）
-- L1：SIT / UAT / PROD
-- L2（各階段下）：環境建置 / 系統開發 / 系統上線
-- L3（各工作包下）：具體任務
SET IDENTITY_INSERT wbs_template_nodes ON;
INSERT INTO wbs_template_nodes (id, template_id, parent_id, title, sort_order) VALUES
    -- L1
    ( 1, 1, NULL, N'SIT',  0),
    ( 2, 1, NULL, N'UAT',  1),
    ( 3, 1, NULL, N'PROD', 2),

    -- SIT > L2
    ( 4, 1,  1, N'環境建置', 0),
    ( 5, 1,  1, N'系統開發', 1),
    ( 6, 1,  1, N'系統上線', 2),

    -- SIT > 環境建置 > L3
    ( 7, 1,  4, N'主機申請',   0),
    ( 8, 1,  4, N'防火牆開通', 1),
    ( 9, 1,  4, N'主機佈版',   2),

    -- SIT > 系統開發 > L3
    (10, 1,  5, N'功能開發', 0),
    (11, 1,  5, N'功能驗測', 1),

    -- SIT > 系統上線 > L3
    (12, 1,  6, N'測試執行',   0),
    (13, 1,  6, N'IT 測試報告', 1),

    -- UAT > L2
    (14, 1,  2, N'環境建置',   0),
    (15, 1,  2, N'使用者測試', 1),
    (16, 1,  2, N'系統上線',   2),

    -- UAT > 環境建置 > L3
    (17, 1, 14, N'主機申請',   0),
    (18, 1, 14, N'防火牆開通', 1),
    (19, 1, 14, N'主機佈版',   2),

    -- UAT > 使用者測試 > L3
    (20, 1, 15, N'功能驗測', 0),
    (21, 1, 15, N'UAT 報告', 1),

    -- UAT > 系統上線 > L3
    (22, 1, 16, N'上線準備', 0),
    (23, 1, 16, N'正式上線', 1),

    -- PROD > L2
    (24, 1,  3, N'環境建置', 0),
    (25, 1,  3, N'系統上線', 1),

    -- PROD > 環境建置 > L3
    (26, 1, 24, N'主機申請',   0),
    (27, 1, 24, N'防火牆開通', 1),
    (28, 1, 24, N'主機佈版',   2),

    -- PROD > 系統上線 > L3
    (29, 1, 25, N'正式部署', 0),
    (30, 1, 25, N'交易測試', 1),
    (31, 1, 25, N'資安檢核', 2);

-- 功能新增模板節點 (template_id=2)
INSERT INTO wbs_template_nodes (id, template_id, parent_id, title, sort_order) VALUES
    (32, 2, NULL, N'SIT',  0), (33, 2, NULL, N'UAT',  1), (34, 2, NULL, N'PROD', 2),
    (35, 2, 32, N'需求討論', 0), (36, 2, 32, N'環境建置', 1), (37, 2, 32, N'功能開發', 2), (38, 2, 32, N'文件準備', 3),
    (39, 2, 35, N'需求確認', 0), (40, 2, 35, N'需求分析', 1),
    (41, 2, 36, N'防火牆申請', 0),
    (42, 2, 37, N'功能設計', 0), (43, 2, 37, N'程式開發', 1), (44, 2, 37, N'功能測試', 2),
    (45, 2, 38, N'SIT 測試報告', 0), (46, 2, 38, N'資安確認書', 1),
    (47, 2, 33, N'環境建置', 0), (48, 2, 33, N'使用者測試', 1), (49, 2, 33, N'文件準備', 2),
    (50, 2, 47, N'UAT佈版', 0), (51, 2, 47, N'防火牆申請', 1),
    (52, 2, 48, N'功能驗測', 0),
    (53, 2, 49, N'UAT測試報告', 0), (54, 2, 49, N'資安確認書', 1),
    (55, 2, 34, N'文件準備', 0), (56, 2, 34, N'系統上線', 1),
    (57, 2, 55, N'上線需求單', 0), (58, 2, 55, N'SIT 測試報告', 1), (59, 2, 55, N'UAT測試報告', 2), (60, 2, 55, N'資安確認書', 3), (61, 2, 55, N'系統版控', 4),
    (62, 2, 56, N'正式部署', 0), (63, 2, 56, N'正式啟用', 1), (64, 2, 56, N'功能測試', 2);

-- EOS系統下線模板節點 (template_id=3)
INSERT INTO wbs_template_nodes (id, template_id, parent_id, title, sort_order) VALUES
    (65, 3, NULL, N'SIT',  0), (66, 3, NULL, N'UAT',  1), (67, 3, NULL, N'PROD', 2),
    (68, 3, 65, N'需求討論', 0), (69, 3, 65, N'資料作業', 1), (70, 3, 65, N'文件準備', 2),
    (71, 3, 68, N'下線評估', 0), (72, 3, 68, N'影響分析', 1),
    (73, 3, 69, N'資料備份', 0), (74, 3, 69, N'資料驗證', 1), (75, 3, 69, N'資料遷移測試', 2),
    (76, 3, 70, N'SIT 測試報告', 0), (77, 3, 70, N'資安確認書', 1),
    (78, 3, 66, N'環境建置', 0), (79, 3, 66, N'使用者測試', 1), (80, 3, 66, N'文件準備', 2),
    (81, 3, 78, N'主機切轉', 0), (82, 3, 78, N'主機防火牆開通', 1), (83, 3, 78, N'環境建置', 2),
    (84, 3, 79, N'功能驗測', 0), (85, 3, 79, N'問題追蹤', 1),
    (86, 3, 80, N'UAT測試報告', 0), (87, 3, 80, N'資安確認書', 1),
    (88, 3, 67, N'文件準備', 0), (89, 3, 67, N'系統上線', 1), (90, 3, 67, N'系統下線', 2),
    (91, 3, 88, N'下線申請單', 0), (92, 3, 88, N'資安確認書', 1), (93, 3, 88, N'使用者通知', 2), (94, 3, 88, N'系統公告', 3),
    (95, 3, 89, N'主機申請', 0), (96, 3, 89, N'服務停止', 1), (97, 3, 89, N'主機切轉', 2), (98, 3, 89, N'主機防火牆開通', 3), (99, 3, 89, N'USER防火牆開通', 4), (100, 3, 89, N'環境建置', 5),
    (101, 3, 90, N'主機拆除回收', 0);

-- 專案開發模板節點 (template_id=4)
INSERT INTO wbs_template_nodes (id, template_id, parent_id, title, sort_order) VALUES
    (102, 4, NULL, N'SIT',  0), (103, 4, NULL, N'UAT',  1), (104, 4, NULL, N'PROD', 2),
    (105, 4, 102, N'需求討論', 0), (106, 4, 102, N'環境建置', 1), (107, 4, 102, N'系統開發', 2), (108, 4, 102, N'文件準備', 3),
    (109, 4, 105, N'需求確認', 0), (110, 4, 105, N'需求分析', 1),
    (111, 4, 106, N'主機申請', 0), (112, 4, 106, N'資料庫申請', 1), (113, 4, 106, N'主機防火牆開通', 2), (114, 4, 106, N'環境建置', 3), (115, 4, 106, N'CD_Server申請', 4),
    (116, 4, 107, N'系統設計', 0), (117, 4, 107, N'資料庫設計', 1), (118, 4, 107, N'程式開發', 2), (119, 4, 107, N'功能測試', 3),
    (120, 4, 108, N'SIT 測試報告', 0), (121, 4, 108, N'資安確認書', 1), (122, 4, 108, N'安控檢核表', 2),
    (123, 4, 103, N'環境建置', 0), (124, 4, 103, N'使用者測試', 1), (125, 4, 103, N'文件準備', 2),
    (126, 4, 123, N'主機申請', 0), (127, 4, 123, N'資料庫申請', 1), (128, 4, 123, N'主機防火牆開通', 2), (129, 4, 123, N'USER防火牆開通', 3), (130, 4, 123, N'環境建置', 4), (131, 4, 123, N'CD_Server申請', 5),
    (132, 4, 124, N'功能驗測', 0), (133, 4, 124, N'問題追蹤', 1),
    (134, 4, 125, N'UAT測試報告', 0), (135, 4, 125, N'資安確認書', 1), (136, 4, 125, N'安控檢核表', 2),
    (137, 4, 104, N'文件準備', 0), (138, 4, 104, N'環境建置', 1), (139, 4, 104, N'系統上線', 2),
    (140, 4, 137, N'上線需求單', 0), (141, 4, 137, N'SIT 測試報告', 1), (142, 4, 137, N'UAT測試報告', 2), (143, 4, 137, N'資安確認書', 3), (144, 4, 137, N'系統版控', 4), (145, 4, 137, N'安控檢核表', 5), (146, 4, 137, N'黑白箱掃描', 6), (147, 4, 137, N'第三方掃描', 7),
    (148, 4, 138, N'主機申請', 0), (149, 4, 138, N'資料庫申請', 1), (150, 4, 138, N'主機防火牆開通', 2), (151, 4, 138, N'USER防火牆開通', 3), (152, 4, 138, N'環境建置', 4), (153, 4, 138, N'系統帳號申請', 5), (154, 4, 138, N'系統帳號納管', 6),
    (155, 4, 139, N'正式部署', 0), (156, 4, 139, N'正式啟用', 1), (157, 4, 139, N'功能測試', 2);

SET IDENTITY_INSERT wbs_template_nodes OFF;
DBCC CHECKIDENT('wbs_template_nodes', RESEED, 157);

-- 全域快速子項（共 48 項，8 類）
SET IDENTITY_INSERT wbs_quick_items ON;
INSERT INTO wbs_quick_items (id, title, category, sort_order, section_id) VALUES
    -- 需求（5）
    (1,  N'需求討論',     N'需求', 0, NULL),
    (2,  N'需求確認',     N'需求', 1, NULL),
    (3,  N'需求分析',     N'需求', 2, NULL),
    (4,  N'下線評估',     N'需求', 3, NULL),
    (5,  N'影響分析',     N'需求', 4, NULL),
    -- 環境建置（11）
    (6,  N'環境建置',         N'環境建置', 0, NULL),
    (7,  N'主機申請',         N'環境建置', 1, NULL),
    (8,  N'資料庫申請',       N'環境建置', 2, NULL),
    (9,  N'主機防火牆開通',   N'環境建置', 3, NULL),
    (10, N'USER防火牆開通',   N'環境建置', 4, NULL),
    (11, N'CD_Server申請',    N'環境建置', 5, NULL),
    (12, N'系統帳號申請',     N'環境建置', 6, NULL),
    (13, N'系統帳號納管',     N'環境建置', 7, NULL),
    (14, N'UAT佈版',          N'環境建置', 8, NULL),
    (15, N'防火牆申請',       N'環境建置', 9, NULL),
    (16, N'主機切轉',         N'環境建置', 10, NULL),
    -- 開發（7）
    (17, N'功能開發',   N'開發', 0, NULL),
    (18, N'系統開發',   N'開發', 1, NULL),
    (19, N'功能設計',   N'開發', 2, NULL),
    (20, N'系統設計',   N'開發', 3, NULL),
    (21, N'資料庫設計', N'開發', 4, NULL),
    (22, N'程式開發',   N'開發', 5, NULL),
    (23, N'功能測試',   N'開發', 6, NULL),
    -- 測試（3）
    (24, N'使用者測試', N'測試', 0, NULL),
    (25, N'功能驗測',   N'測試', 1, NULL),
    (26, N'問題追蹤',   N'測試', 2, NULL),
    -- 文件（12）
    (27, N'文件準備',      N'文件', 0, NULL),
    (28, N'SIT 測試報告',  N'文件', 1, NULL),
    (29, N'UAT測試報告',   N'文件', 2, NULL),
    (30, N'資安確認書',    N'文件', 3, NULL),
    (31, N'安控檢核表',    N'文件', 4, NULL),
    (32, N'黑白箱掃描',    N'文件', 5, NULL),
    (33, N'第三方掃描',    N'文件', 6, NULL),
    (34, N'上線需求單',    N'文件', 7, NULL),
    (35, N'系統版控',      N'文件', 8, NULL),
    (36, N'下線申請單',    N'文件', 9, NULL),
    (37, N'使用者通知',    N'文件', 10, NULL),
    (38, N'系統公告',      N'文件', 11, NULL),
    -- 上線（4）
    (39, N'系統上線', N'上線', 0, NULL),
    (40, N'正式部署', N'上線', 1, NULL),
    (41, N'正式啟用', N'上線', 2, NULL),
    (42, N'服務停止', N'上線', 3, NULL),
    -- 資料作業（4）
    (43, N'資料作業',     N'資料作業', 0, NULL),
    (44, N'資料備份',     N'資料作業', 1, NULL),
    (45, N'資料驗證',     N'資料作業', 2, NULL),
    (46, N'資料遷移測試', N'資料作業', 3, NULL),
    -- 系統下線（2）
    (47, N'系統下線',     N'系統下線', 0, NULL),
    (48, N'主機拆除回收', N'系統下線', 1, NULL);
SET IDENTITY_INSERT wbs_quick_items OFF;
DBCC CHECKIDENT('wbs_quick_items', RESEED, 48);

-- 測試專案
SET IDENTITY_INSERT projects ON;
INSERT INTO projects (id, name, department_id, owner_id, created_by) VALUES
    (1, N'資訊科測試專案',  2, 3, 3),
    (2, N'資訊科2測試專案', 3, 7, 7);
SET IDENTITY_INSERT projects OFF;
DBCC CHECKIDENT('projects', RESEED, 2);

-- 測試專案成員（project_members 無 IDENTITY 欄位，不需處理）
INSERT INTO project_members (project_id, user_id, assigned_by) VALUES
    (1, 3, 3),
    (1, 4, 3),
    (1, 5, 3),
    (2, 7, 7),
    (2, 8, 7);
