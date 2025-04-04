package co.com.example.model.transcribe;

import reactor.core.publisher.Mono;

public interface TranscribeGateway {
    Mono<String> transcribe(String jobNumber, String audioName);

    Mono<String> getTranscriptionJobStatus(String jobId);
}
