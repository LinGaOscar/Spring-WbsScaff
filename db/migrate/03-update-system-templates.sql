-- 更新系統模板為 SIT/UAT/PROD 三層結構
DELETE FROM wbs_template_nodes WHERE template_id IN (1, 2);
DELETE FROM wbs_templates WHERE id IN (1, 2);

INSERT INTO wbs_templates (id, name, description, is_system, is_default) VALUES
    (1, '系統開發標準', 'SIT→UAT→PROD 三階段、三層結構標準模板', TRUE, FALSE);
SELECT setval('wbs_templates_id_seq', 1);

INSERT INTO wbs_template_nodes (id, template_id, parent_id, title, sort_order) VALUES
    ( 1, 1, NULL, 'SIT',  0),
    ( 2, 1, NULL, 'UAT',  1),
    ( 3, 1, NULL, 'PROD', 2),
    ( 4, 1,  1, '環境建置', 0),
    ( 5, 1,  1, '系統開發', 1),
    ( 6, 1,  1, '系統上線', 2),
    ( 7, 1,  4, '主機申請',    0),
    ( 8, 1,  4, '防火牆開通',  1),
    ( 9, 1,  4, '主機佈版',    2),
    (10, 1,  5, '功能開發', 0),
    (11, 1,  5, '功能驗測', 1),
    (12, 1,  6, '測試執行',    0),
    (13, 1,  6, 'IT 測試報告', 1),
    (14, 1,  2, '環境建置',   0),
    (15, 1,  2, '使用者測試', 1),
    (16, 1,  2, '系統上線',   2),
    (17, 1, 14, '主機申請',   0),
    (18, 1, 14, '防火牆開通', 1),
    (19, 1, 14, '主機佈版',   2),
    (20, 1, 15, '功能驗測', 0),
    (21, 1, 15, 'UAT 報告', 1),
    (22, 1, 16, '上線準備', 0),
    (23, 1, 16, '正式上線', 1),
    (24, 1,  3, '環境建置', 0),
    (25, 1,  3, '系統上線', 1),
    (26, 1, 24, '主機申請',   0),
    (27, 1, 24, '防火牆開通', 1),
    (28, 1, 24, '主機佈版',   2),
    (29, 1, 25, '正式部署', 0),
    (30, 1, 25, '交易測試', 1),
    (31, 1, 25, '資安檢核', 2);
SELECT setval('wbs_template_nodes_id_seq', 31);
