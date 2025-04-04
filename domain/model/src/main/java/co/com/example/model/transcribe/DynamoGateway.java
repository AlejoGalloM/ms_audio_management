package co.com.example.model.transcribe;

import reactor.core.publisher.Mono;

public interface DynamoGateway {
    Mono<Audio> saveTranscript(Audio audio);

    Mono<Audio> getTranscript(String jobId);

    Mono<Void> updateTranscript(Audio audio);
}
