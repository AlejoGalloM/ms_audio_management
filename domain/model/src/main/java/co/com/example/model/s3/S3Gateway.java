package co.com.example.model.s3;

import reactor.core.publisher.Mono;

import java.io.InputStream;

public interface S3Gateway {
    Mono<Void> uploadFile(byte[] file, String fileName);

    Mono<String> obtainFile(String jobNumber);
}
