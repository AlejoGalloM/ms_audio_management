package co.com.example.websocket.util;

import co.com.example.usecase.audio.AudioUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FragmentUtil {
    private final AudioUseCase audioUseCase;
    private final Map<String, String[]> fragmentosCache = new HashMap<>();

    public Mono<Void> procesarFragmento(String[] partes, WebSocketSession session) {
        try {
            String idAudio = partes[1];
            int indiceFragmento = Integer.parseInt(partes[2]);
            int totalFragmentos = Integer.parseInt(partes[3]);
            String contenidoFragmento = partes[4];

            fragmentosCache.computeIfAbsent(idAudio, key -> new String[totalFragmentos]);

            fragmentosCache.get(idAudio)[indiceFragmento - 1] = contenidoFragmento;
            log.info("Fragmento {} recibido para audio: {}", indiceFragmento, idAudio);

            if (estaCompleto(fragmentosCache.get(idAudio))) {
                log.info("Todos los fragmentos recibidos para audio: {}", idAudio);

                String contenidoCompletoBase64 = ensamblarFragmentos(fragmentosCache.get(idAudio));
                log.info("Archivo ensamblado correctamente. Longitud en Base64: {}", contenidoCompletoBase64.length());

                try {
                    byte[] archivoFinal = decodificarYDescomprimir(contenidoCompletoBase64);
                    log.info("Archivo descomprimido con éxito. Tamaño: {} bytes", archivoFinal.length);

                    return audioUseCase.uploadAudio(idAudio, archivoFinal)
                            .flatMap(result -> {
                                log.info("Archivo procesado exitosamente para audio: {}", idAudio);
                                fragmentosCache.remove(idAudio);
                                return session.send(Mono.just(session.textMessage("Archivo recibido y procesado")));
                            });
                } catch (Exception ex) {
                    log.error("Error descomprimiendo o procesando el archivo: {}", ex.getMessage());
                    return session.send(Mono.just(session.textMessage("Error procesando archivo: " + ex.getMessage())));
                }
            }
            return session.send(Mono.just(session.textMessage("Fragmento recibido con éxito.")));
        } catch (Exception e) {
            log.error("Error procesando fragmento: {}", e.getMessage());
            return session.send(Mono.just(session.textMessage("Error procesando fragmento: " + e.getMessage())));
        }
    }

    private boolean estaCompleto(String[] fragmentos) {
        for (int i = 0; i < fragmentos.length; i++) {
            if (fragmentos[i] == null) {
                log.info("Falta el fragmento número: {}", i);
                return false;
            }
        }
        log.info("Todos los fragmentos han sido recibidos.");
        return true;
    }

    private String ensamblarFragmentos(String[] fragmentos) {
        StringBuilder sb = new StringBuilder();
        for (String fragmento : fragmentos) {
            sb.append(fragmento);
        }
        return sb.toString();
    }

    private byte[] decodificarYDescomprimir(String base64String) throws Exception {
        byte[] contenidoCodificado = Base64.getDecoder().decode(base64String);
        try (GZIPInputStream gis = new GZIPInputStream(new java.io.ByteArrayInputStream(contenidoCodificado))) {
            return gis.readAllBytes();
        }
    }
}
