-- changeset tin:add-column
ALTER TABLE users
    ADD s3_url nvarchar(512) null;