-- ============================================================
-- Lead Discovery Platform - User Role & Name Migration
-- Flyway migration V2
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS name VARCHAR(255);

-- Backfill name from username if name is null
UPDATE users SET name = username WHERE name IS NULL;

ALTER TABLE users ALTER COLUMN name SET NOT NULL;

-- Allow username to be nullable if registration uses email and name
ALTER TABLE users ALTER COLUMN username DROP NOT NULL;

-- Add role column
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(30) NOT NULL DEFAULT 'USER';

-- Add check constraint for roles
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_user_role') THEN
        ALTER TABLE users ADD CONSTRAINT chk_user_role CHECK (role IN ('USER', 'ADMIN'));
    END IF;
END $$;
