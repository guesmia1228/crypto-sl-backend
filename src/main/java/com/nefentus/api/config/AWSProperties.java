package com.nefentus.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Created on 12/7/2019.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "amazon-properties")
public class AWSProperties {
    private final S3 s3 = new S3();

    @Getter
    @Setter
    public static class S3 {
        private String bucketName;
        private String regions;
        private String dateFormatPattern;
        private String folder;
    }
}
