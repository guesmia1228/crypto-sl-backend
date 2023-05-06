-- changeset tin:add-column
ALTER TABLE kyc_image
    ADD s3_key nvarchar(512) null;