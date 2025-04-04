package co.com.example.s3.adapter;

import co.com.example.extractdata.ExtractData;
import co.com.example.model.s3.S3Gateway;
import org.springframework.stereotype.Repository;
import co.com.example.s3.operations.S3Operations;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Repository
@AllArgsConstructor
public class S3Adapter implements S3Gateway {

    private final S3Operations s3Operations;

    @Override
    public Mono<Void> uploadFile(byte[] file, String fileName) {
        return s3Operations.uploadObject(file, fileName)
                .then();
    }

    @Override
    public Mono<String> obtainFile(String jobNumber) {
        var jobTranscription = String.format("transcription%s.json", jobNumber);
        return s3Operations.obtainFile(jobTranscription)
                .flatMap(file -> Mono.fromCallable(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file))) {
                        return reader.lines().collect(Collectors.joining("\n"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }))
                .flatMap(ExtractData::extractTranscript);
    }
}
