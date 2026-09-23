-- ============================================================
-- Lead Discovery Platform - Contact Provenance & Verification
-- Flyway migration V3
-- ============================================================

-- 1. Add provenance & verification columns to email_addresses
ALTER TABLE email_addresses
    ADD COLUMN IF NOT EXISTS source_domain VARCHAR(255),
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(30) NOT NULL DEFAULT 'UNVERIFIED';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_email_verification_status'
    ) THEN
        ALTER TABLE email_addresses
            ADD CONSTRAINT chk_email_verification_status
            CHECK (verification_status IN ('VERIFIED', 'EXTERNAL', 'UNVERIFIED'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_email_addresses_verification ON email_addresses(verification_status);
CREATE INDEX IF NOT EXISTS idx_email_addresses_source_domain ON email_addresses(source_domain);

-- 2. Add provenance & verification columns to phone_numbers
ALTER TABLE phone_numbers
    ADD COLUMN IF NOT EXISTS source_domain VARCHAR(255),
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(30) NOT NULL DEFAULT 'UNVERIFIED';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_phone_verification_status'
    ) THEN
        ALTER TABLE phone_numbers
            ADD CONSTRAINT chk_phone_verification_status
            CHECK (verification_status IN ('VERIFIED', 'EXTERNAL', 'UNVERIFIED'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_phone_numbers_verification ON phone_numbers(verification_status);
CREATE INDEX IF NOT EXISTS idx_phone_numbers_source_domain ON phone_numbers(source_domain);

-- 3. Add provenance & verification columns to social_links
ALTER TABLE social_links
    ADD COLUMN IF NOT EXISTS source_page_id BIGINT REFERENCES source_pages(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS source_domain VARCHAR(255),
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(30) NOT NULL DEFAULT 'UNVERIFIED';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_social_verification_status'
    ) THEN
        ALTER TABLE social_links
            ADD CONSTRAINT chk_social_verification_status
            CHECK (verification_status IN ('VERIFIED', 'EXTERNAL', 'UNVERIFIED'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_social_links_source_page ON social_links(source_page_id);
CREATE INDEX IF NOT EXISTS idx_social_links_verification ON social_links(verification_status);

-- 4. Clean up / classify existing records based on provenance
UPDATE email_addresses e
SET verification_status = 'VERIFIED'
FROM websites w
WHERE e.organization_id = w.organization_id
  AND w.is_official = TRUE
  AND (
      e.normalized_value ILIKE '%@' || replace(replace(replace(w.normalized_url, 'https://', ''), 'http://', ''), 'www.', '')
      OR e.normalized_value ILIKE '%@%' || split_part(replace(replace(replace(w.normalized_url, 'https://', ''), 'http://', ''), 'www.', ''), '/', 1)
  );

UPDATE phone_numbers p
SET verification_status = 'VERIFIED'
FROM websites w
WHERE p.organization_id = w.organization_id
  AND w.is_official = TRUE;

UPDATE social_links s
SET verification_status = 'VERIFIED'
FROM websites w
WHERE s.organization_id = w.organization_id
  AND w.is_official = TRUE;
