package co.com.example.model.unzipdecode;

import reactor.core.publisher.Mono;

import java.io.InputStream;

public interface ZipDecodeGateway {
    Mono<String> decodeFile(String file);

    Mono<InputStream> unzipFile(byte[] file);
}
