package com.example.demo;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@ConfigurationProperties(prefix = "storage")
record StorageProps(
    String endpoint,
    String bucket,
    String accessKey,
    String secretKey
) {}

@Configuration
@EnableConfigurationProperties(StorageProps.class)
public class S3Config {

    @Bean
    S3Client s3Client(StorageProps p) {
        return S3Client.builder()
                .endpointOverride(URI.create(p.endpoint()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(p.accessKey(), p.secretKey())))
                .forcePathStyle(true)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Bean
    S3Presigner s3Presigner(StorageProps p) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(p.endpoint()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(p.accessKey(), p.secretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Bean
    ApplicationRunner initBucket(S3Client s3, StorageProps p) {
        return args -> {
            try {
                s3.headBucket(HeadBucketRequest.builder().bucket(p.bucket()).build());
            } catch (NoSuchBucketException e) {
                s3.createBucket(CreateBucketRequest.builder().bucket(p.bucket()).build());
            }
            try {
                s3.putBucketCors(PutBucketCorsRequest.builder()
                        .bucket(p.bucket())
                        .corsConfiguration(CORSConfiguration.builder()
                                .corsRules(CORSRule.builder()
                                        .allowedOrigins("*")
                                        .allowedMethods("GET", "PUT", "POST", "HEAD", "DELETE")
                                        .allowedHeaders("*")
                                        .build())
                                .build())
                        .build());
            } catch (Exception ignored) {
            }
        };
    }
}
