package co.com.example.s3.config;

import co.com.example.exceptions.TechnicalException;
import co.com.example.exceptions.message.TechnicalExceptionMessage;
import co.com.example.s3.config.model.S3ConnectionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

import java.net.URI;
import java.util.List;

@Slf4j
@Configuration
public class S3Config {

    @Profile({"dev", "cer", "pdn"})
    @Bean
    public S3AsyncClient s3AsyncClient(S3ConnectionProperties s3Properties) {
        return S3AsyncClient.builder()
                .region(Region.of(s3Properties.region()))
                .build();
    }


    public S3AsyncClient localS3AsyncClient(S3ConnectionProperties s3Properties) {
        return S3AsyncClient.builder()
                .region(Region.of(s3Properties.region()))
                .endpointOverride(URI.create(s3Properties.endpoint()))
                .credentialsProvider(ProfileCredentialsProvider.create("default"))
                .build();
    }

    public void validateS3Connection(S3AsyncClient s3Client) {
        try {
            s3Client.listBuckets().get();
            log.info("Connection to S3 successful.");
        } catch (Exception e) {
            log.info("Connection to S3 failed for: {}", e.getMessage());
            throw new TechnicalException(TechnicalExceptionMessage.TECHNICAL_S3_EXCEPTION);
        }
    }

    public void validateS3Connection(S3AsyncClient s3Client, String requiredBucketName) {
        try {
            ListBucketsResponse listBucketsResponse = s3Client.listBuckets().get();
            List<Bucket> buckets = listBucketsResponse.buckets();

            boolean bucketExists = buckets.stream()
                    .anyMatch(bucket -> bucket.name().equals(requiredBucketName));

            if (bucketExists) {
                s3Client.headBucket(b -> b.bucket(requiredBucketName));
                log.info("Connection to S3 bucket {} successful.", requiredBucketName);
            } else {
                log.error("The bucket {} does not exist.", requiredBucketName);
            }
        } catch (SdkClientException e) {
            log.error("An error occurred while validating the S3 connection: {}", e.getMessage());
        } catch (Exception e) {
            throw new TechnicalException(TechnicalExceptionMessage.TECHNICAL_S3_EXCEPTION);
        }
    }

    public void validateS3ConnectionBucket(S3AsyncClient s3Client, String requiredBucketName) {
        try {
            s3Client.headBucket(b -> b.bucket(requiredBucketName));
            log.info("Connection to S3 bucket {} successful.", requiredBucketName);
        } catch (SdkClientException e) {
            log.error("An error occurred while validating the S3 connection: {}", e.getMessage());
        } catch (Exception e) {
            throw new TechnicalException(TechnicalExceptionMessage.TECHNICAL_S3_EXCEPTION);
        }
    }

    @Bean
    @Profile({"local"})
    public S3AsyncClient s3AsyncClientLocal(S3ConnectionProperties s3Properties, @Value("${adapter.aws.s3.storageBucketName}") String bucketName) {
        S3AsyncClient s3Client = localS3AsyncClient(s3Properties);
        validateS3Connection(s3Client);
        validateS3Connection(s3Client, bucketName);
        return s3Client;
    }
}
