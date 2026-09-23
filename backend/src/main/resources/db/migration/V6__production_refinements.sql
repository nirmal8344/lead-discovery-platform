-- ============================================================
-- Lead Discovery Platform - Production Refinements
-- Flyway migration V6
-- ============================================================

-- Add pincode column to organizations table
ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS pincode VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_organizations_pincode ON organizations(pincode);
