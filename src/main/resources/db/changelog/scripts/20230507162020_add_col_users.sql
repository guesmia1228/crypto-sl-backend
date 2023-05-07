-- changeset tin:add-column
ALTER TABLE users
    ADD s3_key nvarchar(512) null;