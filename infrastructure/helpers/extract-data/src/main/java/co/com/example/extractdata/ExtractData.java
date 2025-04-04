package co.com.example.extractdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

public class ExtractData {
    public static Mono<String> extractTranscript(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonString);
            JsonNode transcriptNode = rootNode.at("/results/transcripts/0/transcript");
            return Mono.just(transcriptNode.asText());
        } catch (Exception e) {
            throw new RuntimeException("Error procesando JSON");
        }
    }
}
