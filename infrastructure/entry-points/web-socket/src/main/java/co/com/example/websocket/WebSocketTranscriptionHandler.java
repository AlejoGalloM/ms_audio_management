package co.com.example.websocket;

import co.com.example.usecase.audio.AudioUseCase;
import co.com.example.websocket.util.FragmentUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketTranscriptionHandler implements WebSocketHandler {

    private final AudioUseCase audioUseCase;
    private final FragmentUtil fragmentUtil;

    private final Map<String, String[]> fragmentosCache = new HashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Mono<Void> receiveFlow = session.receive()
                .flatMap(msg -> {
                    var payload = msg.getPayloadAsText();
                    String[] partes = payload.split(":");

                    return switch (partes[0]) {
                        case "upload" -> fragmentUtil.procesarFragmento(partes, session);
                        case "transcript" -> audioUseCase.transcribe(partes[1])
                                .flatMap(result -> session.send(Mono.just(session.textMessage(result))));
                        case "status" -> audioUseCase.getTranscribeStatus(partes[1])
                                .flatMap(result -> session.send(Mono.just(session.textMessage(result))));
                        case "obtain" -> audioUseCase.getTranscribe(partes[1])
                                .flatMap(result -> session.send(Mono.just(session.textMessage(result))));
                        default -> session.send(Mono.just(session.textMessage("Error: Comando no válido.")));
                    };
                })
                .then();

        Flux<Void> keepAliveFlow = Flux.interval(Duration.ofSeconds(5))
                .flatMap(interval -> session.send(Mono.just(session.textMessage("Conexión activa"))));

        return Mono.when(receiveFlow, keepAliveFlow);
    }
}