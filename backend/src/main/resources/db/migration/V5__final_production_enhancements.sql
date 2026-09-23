-- ============================================================
-- Lead Discovery Platform - Final Production Enhancements
-- Flyway migration V5
-- ============================================================

-- 1. Add search_radius_km and max_crawl_depth to scraping_tasks
ALTER TABLE scraping_tasks
    ADD COLUMN IF NOT EXISTS search_radius_km INTEGER,
    ADD COLUMN IF NOT EXISTS max_crawl_depth INTEGER NOT NULL DEFAULT 3;

-- 2. Create scraping_task_errors table for tracking failed / skipped websites
CREATE TABLE IF NOT EXISTS scraping_task_errors (
    id             BIGSERIAL PRIMARY KEY,
    task_id        BIGINT NOT NULL REFERENCES scraping_tasks(id) ON DELETE CASCADE,
    url            TEXT,
    domain         VARCHAR(255),
    error_type     VARCHAR(100) NOT NULL,
    error_message  TEXT,
    stage          VARCHAR(100),
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_scraping_task_errors_task ON scraping_task_errors(task_id);
CREATE INDEX IF NOT EXISTS idx_scraping_task_errors_created_at ON scraping_task_errors(created_at);
