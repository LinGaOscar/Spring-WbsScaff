-- 模板節點新增 notes 欄位，供 L3 節點填寫預設說明
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'wbs_template_nodes' AND COLUMN_NAME = 'notes'
)
ALTER TABLE wbs_template_nodes ADD notes NVARCHAR(MAX) NULL;
