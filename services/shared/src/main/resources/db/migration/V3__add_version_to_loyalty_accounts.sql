-- Add version to loyalty_accounts
-- Защита от двойных списаний баллов при конкурентных запросах
ALTER TABLE loyalty_accounts
ADD COLUMN version INTEGER NOT NULL DEFAULT 0;