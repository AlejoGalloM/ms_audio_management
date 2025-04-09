package co.com.example.transcribe;

import co.com.example.model.transcribe.TranscribeGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.transcribe.TranscribeClient;
import software.amazon.awssdk.services.transcribe.model.*;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;

import java.net.URI;

@Slf4j
@Component
public class Transcribe implements TranscribeGateway {
    private final static String transcriptionName = "transcription";
    private final String bucketName;
    private final String endpoint;

    public Transcribe(@Value("${adapter.aws.s3.storageBucketName}") String bucketName,
                      @Value("${adapter.aws.transcribe.endpoint}") String endpoint) {
        this.bucketName = bucketName;
        this.endpoint = endpoint;
    }

    @Override
    public Mono<String> transcribe(String jobNumber, String audioName) {
        try (TranscribeClient transcribeClient = TranscribeClient.builder()
                .credentialsProvider(ProfileCredentialsProvider.create("default"))
                .endpointOverride(URI.create(endpoint))
                .build()) {

            String mediaFileUri = "s3://" + bucketName + "/" + audioName;

            StartTranscriptionJobRequest request = StartTranscriptionJobRequest.builder()
                    .transcriptionJobName(transcriptionName.concat(jobNumber))
                    .languageCode(String.valueOf(LanguageCode.ES_ES))
                    .media(Media.builder().mediaFileUri(mediaFileUri).build())
                    .outputBucketName(bucketName)
                    .build();

            StartTranscriptionJobResponse response = transcribeClient.startTranscriptionJob(request);

            log.info("Trabajo de transcripción iniciado con éxito.");
            log.info("ID del trabajo de transcripción: {}", response.transcriptionJob().transcriptionJobName());

            return Mono.just(response.transcriptionJob().transcriptionJobName());
        } catch (TranscribeException e) {
            log.error("Error al iniciar el trabajo de transcripción: {}", e.awsErrorDetails().errorMessage());
            return Mono.error(new RuntimeException("Error al iniciar el trabajo de transcripción.", e));
        }
    }

    @Override
    public Mono<String> getTranscriptionJobStatus(String jobId) {
        try (TranscribeClient transcribeClient = TranscribeClient.builder()
                .credentialsProvider(ProfileCredentialsProvider.create("default"))
                .endpointOverride(URI.create(endpoint))
                .build()) {
            GetTranscriptionJobRequest request = GetTranscriptionJobRequest.builder()
                    .transcriptionJobName(transcriptionName + jobId)
                    .build();

            GetTranscriptionJobResponse response = transcribeClient.getTranscriptionJob(request);

            TranscriptionJobStatus status = response.transcriptionJob().transcriptionJobStatus();

            log.info("Estado del trabajo de transcripción {}: {}", jobId, status);

            return Mono.just(status.toString());
        } catch (TranscribeException e) {
            log.error("Error al obtener el estado del trabajo de transcripción: {}", e.awsErrorDetails().errorMessage());
            return Mono.error(new RuntimeException("Error al obtener el estado del trabajo de transcripción.", e));
        }
    }

}