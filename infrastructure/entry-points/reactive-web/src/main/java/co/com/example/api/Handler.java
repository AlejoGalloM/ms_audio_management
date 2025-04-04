package co.com.example.api;

import co.com.example.extractdata.ExtractData;
import co.com.example.usecase.audio.AudioUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class Handler {
    private final AudioUseCase audioUseCase;

    public Mono<ServerResponse> transcribe(ServerRequest serverRequest) {
        return audioUseCase.transcribe(serverRequest.pathVariable("audioName"))
                .flatMap(ServerResponse.ok()::bodyValue);
    }

    public Mono<ServerResponse> getTranscribe(ServerRequest serverRequest) {
        return audioUseCase.getTranscribe(serverRequest.pathVariable("jobId"))
                .flatMap(ServerResponse.ok()::bodyValue);
    }

    public Mono<ServerResponse> transcribeStatus(ServerRequest serverRequest) {
        return audioUseCase.getTranscribeStatus(serverRequest.pathVariable("jobId"))
                .flatMap(ServerResponse.ok()::bodyValue);
    }
}