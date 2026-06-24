-- WBS Scaffold 資料庫 Schema (SQL Server)
-- 合併所有 PostgreSQL migration，含 archived、requirement_doc 欄位

IF OBJECT_ID('dbo.departments', 'U') IS NULL
CREATE TABLE departments (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100) NOT NULL,
    parent_id   BIGINT REFERENCES departments(id),
    created_at  DATETIME2
);

IF OBJECT_ID('dbo.users', 'U') IS NULL
CREATE TABLE users (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    email           NVARCHAR(200) NOT NULL UNIQUE,
    password_hash   NVARCHAR(MAX) NOT NULL,
    display_name    NVARCHAR(100) NOT NULL,
    department_id   BIGINT REFERENCES departments(id),
    role            NVARCHAR(20) NOT NULL DEFAULT 'PROJECT_MEMBER'
                        CHECK (role IN ('DIRECTOR','SECTION_CHIEF','PROJECT_LEADER','PROJECT_MEMBER')),
    enabled         BIT NOT NULL DEFAULT 1,
    created_at      DATETIME2
);

IF OBJECT_ID('dbo.projects', 'U') IS NULL
CREATE TABLE projects (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(200) NOT NULL,
    department_id   BIGINT REFERENCES departments(id),
    owner_id        BIGINT REFERENCES users(id),
    created_by      BIGINT REFERENCES users(id),
    archived        BIT NOT NULL DEFAULT 0,
    created_at      DATETIME2,
    updated_at      DATETIME2
);

IF OBJECT_ID('dbo.project_members', 'U') IS NULL
CREATE TABLE project_members (
    project_id      BIGINT NOT NULL REFERENCES projects(id),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    assigned_by     BIGINT REFERENCES users(id),
    joined_at       DATETIME2,
    PRIMARY KEY (project_id, user_id)
);

IF OBJECT_ID('dbo.wbs_nodes', 'U') IS NULL
CREATE TABLE wbs_nodes (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES projects(id),
    parent_id   BIGINT,
    title       NVARCHAR(300) NOT NULL,
    owner       NVARCHAR(100),
    start_date  DATE,
    end_date    DATE,
    status      NVARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED'
                    CHECK (status IN ('NOT_STARTED','IN_PROGRESS','DONE')),
    notes       NVARCHAR(MAX),
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  DATETIME2,
    updated_at  DATETIME2
);

IF OBJECT_ID('dbo.wbs_templates', 'U') IS NULL
CREATE TABLE wbs_templates (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX),
    owner_id    BIGINT REFERENCES users(id),
    is_system   BIT NOT NULL DEFAULT 0,
    is_default  BIT NOT NULL DEFAULT 0,
    cloned_from BIGINT,
    section_id  BIGINT REFERENCES departments(id),
    created_at  DATETIME2,
    updated_at  DATETIME2
);

IF OBJECT_ID('dbo.wbs_template_nodes', 'U') IS NULL
CREATE TABLE wbs_template_nodes (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES wbs_templates(id),
    parent_id   BIGINT,
    title       NVARCHAR(300) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0
);

IF OBJECT_ID('dbo.wbs_quick_items', 'U') IS NULL
CREATE TABLE wbs_quick_items (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    title           NVARCHAR(255) NOT NULL,
    category        NVARCHAR(255),
    sort_order      INT,
    requirement_doc NVARCHAR(500),
    section_id      BIGINT REFERENCES departments(id),
    created_at      DATETIME2
);
