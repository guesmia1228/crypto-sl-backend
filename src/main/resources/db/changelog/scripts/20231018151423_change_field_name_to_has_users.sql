-- liquibase formatted sql

-- changeset tin:alter-users
ALTER TABLE users
RENAME COLUMN mfa TO has_totp,
RENAME COLUMN is_require_otp TO has_otp;