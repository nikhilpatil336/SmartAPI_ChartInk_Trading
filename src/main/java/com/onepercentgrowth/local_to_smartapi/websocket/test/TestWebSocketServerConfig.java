package com.onepercentgrowth.local_to_smartapi.websocket.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Profile("test")
@Configuration
@EnableWebSocket
public class TestWebSocketServerConfig implements WebSocketConfigurer {

    private final TestWebSocketHandler handler;

    public TestWebSocketServerConfig(TestWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/test")
                .setAllowedOrigins("*");
    }
}