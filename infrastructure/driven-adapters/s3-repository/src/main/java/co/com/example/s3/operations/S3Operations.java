package co.com.example.s3.operations;

import co.com.example.exceptions.TechnicalException;
import co.com.example.exceptions.message.TechnicalExceptionMessage;
import co.com.example.s3.config.model.S3ConnectionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Operations {

    private final S3ConnectionProperties s3ConnectionProperties;
    private final S3AsyncClient s3AsyncClient;


    public Mono<Boolean> uploadObject(byte[] file, String newName) {
        log.info("Uploading file to S3 with name: {}", newName);
        log.debug("File content (first 100 bytes): {}", new String(file, 0, Math.min(file.length, 100)));

        PutObjectRequest putObjectRequest = configurePutObject(s3ConnectionProperties.storageBucketName(), newName);

        return Mono.fromFuture(s3AsyncClient.putObject(putObjectRequest, AsyncRequestBody.fromBytes(file)))
                .map(response -> {
                    log.info("S3 response: {}", response);
                    return response.sdkHttpResponse().isSuccessful();
                })
                .onErrorResume(Exception.class, exception -> {
                    log.error("Error uploading file to S3", exception);
                    return Mono.defer(() -> Mono.error(new TechnicalException(TechnicalExceptionMessage.S3_PUT_OBJECT_FAIL)));
                });
    }

    private PutObjectRequest configurePutObject(String bucketName, String objectKey) {
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
    }

    public Mono<InputStream> obtainFile(String fileName) {
        return Mono.fromFuture(() -> {
                    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                            .bucket(s3ConnectionProperties.storageBucketName())
                            .key(fileName)
                            .build();

                    return s3AsyncClient.getObject(getObjectRequest, AsyncResponseTransformer.toBytes());
                })
                .map(BytesWrapper::asInputStream)
                .retryWhen(
                        Retry.fixedDelay(5, Duration.ofMinutes(1L))
                                .filter(throwable -> throwable instanceof S3Exception)
                                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                        new RuntimeException("El archivo no se generó dentro del tiempo esperado"))
                );
    }


    public Mono<Void> copyFile(String sourceFileName, String targetFileName) {
        return Mono.create(sink -> {
            String targetKey = "trascriptions" + "/" + targetFileName;

            CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                    .copySource(s3ConnectionProperties.storageBucketName() + "/" + sourceFileName)
                    .bucket(s3ConnectionProperties.storageBucketName())
                    .key(targetKey)
                    .build();

            s3AsyncClient.copyObject(copyObjectRequest)
                    .whenComplete((response, exception) -> {
                        if (exception != null) {
                            sink.error(exception);
                        } else {
                            sink.success();
                        }
                    });
        });
    }
}