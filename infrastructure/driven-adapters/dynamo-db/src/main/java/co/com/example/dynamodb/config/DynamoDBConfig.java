package co.com.example.dynamodb.config;

import co.com.example.exceptions.TechnicalException;
import co.com.example.exceptions.message.TechnicalExceptionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.net.URI;

@Slf4j
@Configuration
public class DynamoDBConfig {


    public DynamoDbAsyncClient amazonDynamoDB(@Value("${aws.dynamodb.endpoint}") String endpoint,
                                              @Value("${adapter.aws.s3.region}") String region) {
        return DynamoDbAsyncClient.builder()
                .credentialsProvider(ProfileCredentialsProvider.create("default"))
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .build();
    }

    @Bean
    public DynamoDbEnhancedAsyncClient getDynamoDbEnhancedAsyncClient(DynamoDbAsyncClient client) {
        return DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(client)
                .build();
    }

    @Bean
    @Profile({"local"})
    public DynamoDbAsyncClient connectToDynamoLocal(@Value("${aws.dynamodb.table-name}") String tableName,
                                                    @Value("${aws.dynamodb.endpoint}") String endpoint,
                                                    @Value("${adapter.aws.s3.region}") String region) {
        var conn = amazonDynamoDB(endpoint, region);
        validateDynamoDBConnection(conn, tableName);
        return conn;

    }

    void validateDynamoDBConnection(DynamoDbAsyncClient dynamoDbAsyncClient, String tableName) {
        try {
            ScanResponse scanResponse = dynamoDbAsyncClient.scan(ScanRequest.builder()
                    .tableName(tableName)
                    .build()).get();
            log.info("Connection to DynamoDB table {} successful.", tableName);
        } catch (ResourceNotFoundException e) {
            log.error("The table {} does not exist.", tableName);
            throw new TechnicalException(TechnicalExceptionMessage.TECHNICAL_DYNAMODB_EXCEPTION);
        } catch (Exception e) {
            log.error("An error occurred while validating the DynamoDB connection: {}", e.getMessage());
            throw new TechnicalException(TechnicalExceptionMessage.TECHNICAL_DYNAMODB_EXCEPTION);
        }
    }
}
