--liquibase formatted sql

--changeset codex:sync-payment-provider-enabled
UPDATE payment_provider
SET enabled = is_enabled
WHERE enabled IS NULL
  AND is_enabled IS NOT NULL;

UPDATE payment_provider
SET enabled = true
WHERE id IN ('PAYPAL', 'COD')
  AND enabled IS NULL;

ALTER TABLE IF EXISTS payment_provider
DROP COLUMN IF EXISTS is_enabled;
