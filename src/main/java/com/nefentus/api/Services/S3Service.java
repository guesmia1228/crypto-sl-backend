package com.nefentus.api.Services;

import java.io.InputStream;

public interface S3Service {
    String presignedURL(String key);
    String uploadToS3Bucket(InputStream inputStream, String key);
}
