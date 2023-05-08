package com.nefentus.api.Services.impl;

import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.nefentus.api.Services.S3Service;
import com.nefentus.api.config.AWSProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;


@Service
@Slf4j
public class S3ServiceImpl implements S3Service {

    private final AmazonS3 s3client;
    private final AWSProperties awsProperties;

    @Autowired
    public S3ServiceImpl(AWSProperties awsProperties) {
        this.awsProperties = awsProperties;
        this.s3client = AmazonS3ClientBuilder.standard().withRegion(awsProperties.getS3().getRegions()).build();
    }

    @Override
    public String presignedURL(String key) {
        try {
            // Set the presigned URL to expire after one hour.
            java.util.Date expiration = new java.util.Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += 1000 * 3600; // 60 minute
            expiration.setTime(expTimeMillis);

            // Generate the presigned URL.
            log.info("Generating pre-signed URL.");
            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                    new GeneratePresignedUrlRequest(awsProperties.getS3Value(), key)
                            .withMethod(HttpMethod.GET)
                            .withExpiration(expiration);
            URL url = s3client.generatePresignedUrl(generatePresignedUrlRequest);

            log.info("Pre-Signed URL: " + url.toString());
            return url.toString();
        } catch (SdkClientException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            log.error("generatePresignedURL error", e);
        }// Amazon S3 couldn't be contacted for a response, or the client
// couldn't parse the response from Amazon S3.
        return StringUtils.EMPTY;
    }

    public String uploadToS3Bucket(InputStream inputStream, String key) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/png");
            s3client.putObject(new PutObjectRequest(awsProperties.getS3Value(), key, inputStream, null));
            URL uri = s3client.getUrl(awsProperties.getS3Value(), key);
            log.info(uri.toString());
            return uri.toString();
        } catch (SdkClientException e) {
            log.error("uploadToS3Bucket: upload to S3 is failure.", e);
        }
        return StringUtils.EMPTY;
    }

    @Override
    public String uploadToS3BucketProfilePic(InputStream inputStream, String key) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/png");
            s3client.putObject(new PutObjectRequest(awsProperties.getS3ValueProfilePic(), key, inputStream, null));
            URL uri = s3client.getUrl(awsProperties.getS3ValueProfilePic(), key);
            log.info(uri.toString());
            return uri.toString();
        } catch (SdkClientException e) {
            log.error("uploadToS3Bucket: upload to S3 is failure.", e);
        }
        return StringUtils.EMPTY;
    }
}
