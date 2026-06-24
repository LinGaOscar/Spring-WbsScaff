-- WBS Scaffold 資料庫 Schema
-- 由 PostgreSQL 初始化時執行（僅在 volume 首次建立時）

CREATE TABLE IF NOT EXISTS departments (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    parent_id   BIGINT REFERENCES departments(id),
    created_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(200) NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    department_id   BIGINT REFERENCES departments(id),
    role            VARCHAR(20) NOT NULL DEFAULT 'PROJECT_MEMBER'
                        CHECK (role IN ('DIRECTOR','SECTION_CHIEF','PROJECT_LEADER','PROJECT_MEMBER')),
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS projects (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    department_id   BIGINT REFERENCES departments(id),
    owner_id        BIGINT REFERENCES users(id),
    created_by      BIGINT REFERENCES users(id),
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS project_members (
    project_id      BIGINT NOT NULL REFERENCES projects(id),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    assigned_by     BIGINT REFERENCES users(id),
    joined_at       TIMESTAMP,
    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE IF NOT EXISTS wbs_nodes (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES projects(id),
    parent_id   BIGINT,
    title       VARCHAR(300) NOT NULL,
    owner       VARCHAR(100),
    start_date  DATE,
    end_date    DATE,
    status      VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED'
                    CHECK (status IN ('NOT_STARTED','IN_PROGRESS','DONE')),
    notes       TEXT,
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wbs_templates (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    owner_id    BIGINT REFERENCES users(id),
    is_system   BOOLEAN NOT NULL DEFAULT FALSE,
    is_default  BOOLEAN NOT NULL DEFAULT FALSE,
    cloned_from BIGINT,
    section_id  BIGINT REFERENCES departments(id),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wbs_template_nodes (
    id          BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES wbs_templates(id),
    parent_id   BIGINT,
    title       VARCHAR(300) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS wbs_quick_items (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    category        VARCHAR(255),
    sort_order      INT,
    requirement_doc VARCHAR(500),
    section_id      BIGINT REFERENCES departments(id),
    created_at      TIMESTAMP
);
