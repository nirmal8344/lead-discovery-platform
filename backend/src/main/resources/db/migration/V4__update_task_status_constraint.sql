-- ============================================================
-- Lead Discovery Platform - Update Task Status Constraint
-- Flyway migration V4
-- ============================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_task_status'
    ) THEN
        ALTER TABLE scraping_tasks DROP CONSTRAINT chk_task_status;
    END IF;

    ALTER TABLE scraping_tasks
        ADD CONSTRAINT chk_task_status
        CHECK (status IN (
            'CREATED',
            'QUEUED',
            'RUNNING',
            'COMPLETED',
            'COMPLETED_WITH_NO_RESULTS',
            'PARTIALLY_COMPLETED',
            'FAILED',
            'CANCELLED'
        ));
END $$;
