package co.com.example.websocket.config;

import co.com.example.websocket.WebSocketTranscriptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

@Slf4j
@Configuration
public class WebSocketConfig {
    @Bean
    public SimpleUrlHandlerMapping webSocketMapping(WebSocketTranscriptionHandler handler) {
        log.info("WebSocket route '/ws/transcribe' registered!");
        return new SimpleUrlHandlerMapping(Map.of(
                "/ws/transcribe", handler
        ), 1);
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

}
