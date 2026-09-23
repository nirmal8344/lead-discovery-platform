-- ============================================================
-- Lead Discovery Platform - Initial Schema
-- Flyway migration V1
-- ============================================================

-- ---------------------------------------------------------------
-- users
-- ---------------------------------------------------------------
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- ---------------------------------------------------------------
-- scraping_tasks
-- ---------------------------------------------------------------
CREATE TABLE scraping_tasks (
    id                      BIGSERIAL PRIMARY KEY,
    location                VARCHAR(255) NOT NULL,
    keyword                 VARCHAR(255) NOT NULL,
    max_results             INTEGER NOT NULL DEFAULT 50,
    max_pages_per_site      INTEGER NOT NULL DEFAULT 5,
    required_fields         TEXT,
    optional_filters        TEXT,
    status                  VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    progress_percentage     INTEGER NOT NULL DEFAULT 0,
    current_stage           VARCHAR(150),
    discovered_businesses   INTEGER NOT NULL DEFAULT 0,
    processed_websites      INTEGER NOT NULL DEFAULT 0,
    leads_saved             INTEGER NOT NULL DEFAULT 0,
    failed_records          INTEGER NOT NULL DEFAULT 0,
    start_time              TIMESTAMP,
    end_time                TIMESTAMP,
    error_message           TEXT,
    created_by              BIGINT REFERENCES users(id),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_task_status CHECK (status IN
        ('CREATED','QUEUED','RUNNING','COMPLETED','PARTIALLY_COMPLETED','FAILED','CANCELLED'))
);

CREATE INDEX idx_scraping_tasks_status ON scraping_tasks(status);
CREATE INDEX idx_scraping_tasks_created_at ON scraping_tasks(created_at);

-- ---------------------------------------------------------------
-- organizations (the discovered leads)
-- ---------------------------------------------------------------
CREATE TABLE organizations (
    id                  BIGSERIAL PRIMARY KEY,
    scraping_task_id    BIGINT NOT NULL REFERENCES scraping_tasks(id) ON DELETE CASCADE,
    business_name       VARCHAR(500) NOT NULL,
    normalized_name     VARCHAR(500) NOT NULL,
    category            VARCHAR(255),
    address             VARCHAR(500),
    city                VARCHAR(255),
    state               VARCHAR(255),
    country             VARCHAR(255),
    source_url          TEXT,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'UNVERIFIED',
    confidence_score    NUMERIC(5,2) NOT NULL DEFAULT 0,
    missing_fields      TEXT,
    processing_status   VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    scraping_timestamp  TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_org_verification_status CHECK (verification_status IN
        ('UNVERIFIED','PARTIALLY_VERIFIED','VERIFIED','FAILED')),
    CONSTRAINT chk_org_processing_status CHECK (processing_status IN
        ('PENDING','PROCESSED','FAILED','DUPLICATE'))
);

CREATE INDEX idx_organizations_task ON organizations(scraping_task_id);
CREATE INDEX idx_organizations_normalized_name ON organizations(normalized_name);
CREATE INDEX idx_organizations_city ON organizations(city);

-- ---------------------------------------------------------------
-- websites
-- ---------------------------------------------------------------
CREATE TABLE websites (
    id               BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    url              TEXT NOT NULL,
    normalized_url   TEXT NOT NULL,
    is_official      BOOLEAN NOT NULL DEFAULT FALSE,
    status           VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    last_crawled_at  TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_website_org_url UNIQUE (organization_id, normalized_url)
);

CREATE INDEX idx_websites_organization ON websites(organization_id);

-- ---------------------------------------------------------------
-- source_pages
-- ---------------------------------------------------------------
CREATE TABLE source_pages (
    id           BIGSERIAL PRIMARY KEY,
    website_id   BIGINT NOT NULL REFERENCES websites(id) ON DELETE CASCADE,
    page_url     TEXT NOT NULL,
    page_type    VARCHAR(30) NOT NULL DEFAULT 'OTHER',
    http_status  INTEGER,
    fetched_at   TIMESTAMP,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_source_page_url UNIQUE (website_id, page_url),
    CONSTRAINT chk_page_type CHECK (page_type IN
        ('HOME','ABOUT','CONTACT','TEAM','SERVICES','OTHER'))
);

CREATE INDEX idx_source_pages_website ON source_pages(website_id);

-- ---------------------------------------------------------------
-- contacts
-- ---------------------------------------------------------------
CREATE TABLE contacts (
    id               BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    contact_person   VARCHAR(255) NOT NULL,
    role             VARCHAR(255),
    source_page_id   BIGINT REFERENCES source_pages(id) ON DELETE SET NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contacts_organization ON contacts(organization_id);

-- ---------------------------------------------------------------
-- phone_numbers
-- ---------------------------------------------------------------
CREATE TABLE phone_numbers (
    id                BIGSERIAL PRIMARY KEY,
    organization_id   BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    raw_value         VARCHAR(50) NOT NULL,
    normalized_value  VARCHAR(50) NOT NULL,
    phone_type        VARCHAR(20) NOT NULL DEFAULT 'PHONE',
    source_page_id    BIGINT REFERENCES source_pages(id) ON DELETE SET NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_phone_org_value_type UNIQUE (organization_id, normalized_value, phone_type),
    CONSTRAINT chk_phone_type CHECK (phone_type IN ('PHONE','WHATSAPP'))
);

CREATE INDEX idx_phone_numbers_organization ON phone_numbers(organization_id);
CREATE INDEX idx_phone_numbers_normalized ON phone_numbers(normalized_value);

-- ---------------------------------------------------------------
-- email_addresses
-- ---------------------------------------------------------------
CREATE TABLE email_addresses (
    id                BIGSERIAL PRIMARY KEY,
    organization_id   BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    raw_value         VARCHAR(255) NOT NULL,
    normalized_value  VARCHAR(255) NOT NULL,
    source_page_id    BIGINT REFERENCES source_pages(id) ON DELETE SET NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_email_org_value UNIQUE (organization_id, normalized_value)
);

CREATE INDEX idx_email_addresses_organization ON email_addresses(organization_id);
CREATE INDEX idx_email_addresses_normalized ON email_addresses(normalized_value);

-- ---------------------------------------------------------------
-- social_links
-- ---------------------------------------------------------------
CREATE TABLE social_links (
    id               BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    platform         VARCHAR(50) NOT NULL,
    url              TEXT NOT NULL,
    source_page_id   BIGINT REFERENCES source_pages(id) ON DELETE SET NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_social_org_platform_url UNIQUE (organization_id, platform, url)
);

CREATE INDEX idx_social_links_organization ON social_links(organization_id);

-- ---------------------------------------------------------------
-- scraping_logs
-- ---------------------------------------------------------------
CREATE TABLE scraping_logs (
    id                BIGSERIAL PRIMARY KEY,
    scraping_task_id  BIGINT NOT NULL REFERENCES scraping_tasks(id) ON DELETE CASCADE,
    level             VARCHAR(20) NOT NULL DEFAULT 'INFO',
    message           TEXT NOT NULL,
    context           TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_log_level CHECK (level IN ('DEBUG','INFO','WARN','ERROR'))
);

CREATE INDEX idx_scraping_logs_task ON scraping_logs(scraping_task_id);
