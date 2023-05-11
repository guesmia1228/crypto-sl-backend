-- changeset tin:add-column
ALTER TABLE users
    ADD is_require_kyc bit not null DEFAULT 0;
