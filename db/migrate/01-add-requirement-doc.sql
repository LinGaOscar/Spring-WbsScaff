-- 為 wbs_quick_items 加入需求單欄位
-- 執行方式：在 PostgreSQL 容器中執行此 script
-- docker exec -i spring-wbsscaff-db-1 psql -U wbs -d wbsdb < db/migrate/01-add-requirement-doc.sql

ALTER TABLE wbs_quick_items
    ADD COLUMN IF NOT EXISTS requirement_doc VARCHAR(500);
