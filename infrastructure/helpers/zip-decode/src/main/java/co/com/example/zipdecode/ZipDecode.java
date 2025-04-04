package co.com.example.zipdecode;

import co.com.example.model.unzipdecode.ZipDecodeGateway;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

@Component
public class ZipDecode implements ZipDecodeGateway {
    @Override
    public Mono<String> decodeFile(String file) {
        byte[] decodedBytes = Base64.getDecoder().decode(file);
        return Mono.just(new String(decodedBytes));
    }

    @Override
    public Mono<InputStream> unzipFile(byte[] file) {
        return Mono.fromCallable(() -> {
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(file))) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                return new ByteArrayInputStream(outputStream.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException("Error al descomprimir el archivo GZIP", e);
            }
        });

    }
}
