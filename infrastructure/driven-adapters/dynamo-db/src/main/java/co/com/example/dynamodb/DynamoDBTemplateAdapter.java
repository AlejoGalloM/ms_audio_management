package co.com.example.dynamodb;

import co.com.example.model.transcribe.Audio;
import co.com.example.model.transcribe.DynamoGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;


@Repository
public class DynamoDBTemplateAdapter implements DynamoGateway {
    private final String tableName;
    private final DynamoDbAsyncClient dynamoAsyncDbClient;

    public DynamoDBTemplateAdapter(@Value("${aws.dynamodb.table-name}") String tableName, DynamoDbAsyncClient dynamoAsyncDbClient) {
        this.tableName = tableName;
        this.dynamoAsyncDbClient = dynamoAsyncDbClient;
    }

    @Override
    public Mono<Audio> saveTranscript(Audio audio) {
        return Mono.fromFuture(() -> {
            var putItemRequest = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(Map.of(
                            "jobId", AttributeValue.builder().n(String.valueOf(audio.getJobId())).build(),
                            "fileName", AttributeValue.builder().s(audio.getFileName()).build(),
                            "transcript", AttributeValue.builder().s(audio.getTranscript()).build()
                    ))
                    .build();

            return dynamoAsyncDbClient.putItem(putItemRequest);
        }).then(Mono.just(audio));
    }


    @Override
    public Mono<Audio> getTranscript(String jobId) {
        return Mono.fromFuture(() -> {
            var getItemRequest = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(
                            "jobId", AttributeValue.builder().n(jobId).build()
                    ))
                    .build();

            return dynamoAsyncDbClient.getItem(getItemRequest)
                    .thenApply(getItemResponse -> {
                        if (getItemResponse.hasItem() && !getItemResponse.item().isEmpty()) {
                            Map<String, AttributeValue> item = getItemResponse.item();
                            return new Audio(
                                    Integer.parseInt(item.get("jobId").n()),
                                    item.get("fileName").s(),
                                    item.get("transcript").s()
                            );
                        } else {
                            return null;
                        }
                    });
        }).flatMap(audio -> audio != null ? Mono.just(audio) : Mono.empty());
    }

    @Override
    public Mono<Void> updateTranscript(Audio audio) {
        return Mono.fromFuture(() -> {
            var updateItemRequest = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(
                            "jobId", AttributeValue.builder().n(String.valueOf(audio.getJobId())).build()
                    ))
                    .attributeUpdates(Map.of(
                            "transcript", AttributeValueUpdate.builder()
                                    .value(AttributeValue.builder().s(audio.getTranscript()).build())
                                    .action(AttributeAction.PUT)
                                    .build()
                    ))
                    .build();

            return dynamoAsyncDbClient.updateItem(updateItemRequest);
        }).then();
    }
}
