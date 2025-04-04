package co.com.example.usecase.audio;

import co.com.example.model.s3.S3Gateway;
import co.com.example.model.transcribe.Audio;
import co.com.example.model.transcribe.DynamoGateway;
import co.com.example.model.transcribe.TranscribeGateway;
import co.com.example.model.unzipdecode.ZipDecodeGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class AudioUseCase {
    private final DynamoGateway dynamoGateway;
    private final S3Gateway s3OperationGateway;
    private final ZipDecodeGateway zipDecodeGateway;
    private final TranscribeGateway transcribeGateway;

    public Mono<String> transcribe(String audioName) {
        var number = (int) (Math.random() * Integer.MAX_VALUE);

        return transcribeGateway.transcribe(String.valueOf(number), audioName)
                .flatMap(data -> dynamoGateway.saveTranscript(Audio.builder()
                        .jobId(number)
                        .fileName(audioName)
                        .transcript("")
                        .build()))
                .map(data -> "Transcription job started with number: " + number);
    }

    public Mono<String> getTranscribe(String jobId) {
        return dynamoGateway.getTranscript(jobId)
                .flatMap(audio -> {
                    if (!"".equals(audio.getTranscript())) {
                        return Mono.just(audio.getTranscript());
                    } else {
                        return s3OperationGateway.obtainFile(jobId)
                                .flatMap(data -> dynamoGateway.updateTranscript(audio.toBuilder().transcript(data).build())
                                        .thenReturn(data));
                    }
                });
    }

    public Mono<String> getTranscribeStatus(String jobId) {
        return transcribeGateway.getTranscriptionJobStatus(jobId);
    }

    public Mono<String> uploadAudio(String fileName, byte[] fileZip) {
        return s3OperationGateway.uploadFile(fileZip, fileName)
                .then(Mono.fromCallable(() -> "File uploaded successfully"));
    }
}