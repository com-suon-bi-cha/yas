--liquibase formatted sql

--changeset codex:payment-provider-is-enabled-compat
ALTER TABLE IF EXISTS payment_provider
ADD COLUMN IF NOT EXISTS is_enabled boolean;
