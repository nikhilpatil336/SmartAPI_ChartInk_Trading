package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.config.TokenManagerService;
import com.onepercentgrowth.local_to_smartapi.websocket.WebSocketClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Service
public class OrderStatusWebSocketService {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusWebSocketService.class);

    private final StandardWebSocketClient client;
    private final WebSocketHandler handler;
    private final TokenManagerService tokenManagerService;

    private WebSocketConnectionManager manager;

//    private WebSocketClientConfig webSocketClientConfig;

    public OrderStatusWebSocketService(
            StandardWebSocketClient client,
            WebSocketHandler handler,
            TokenManagerService tokenManagerService
    ) {
        this.client = client;
        this.handler = handler;
        this.tokenManagerService = tokenManagerService;
    }

    public synchronized void start() {

        if (manager != null && manager.isRunning()) {
            log.info("Order Status WebSocket already running");
            return;
        }

        String jwt = tokenManagerService.getValidJwtToken();
        log.info("Using JWT (first 10 chars): {}...", jwt.substring(0, 10));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jwt);

        manager = new WebSocketConnectionManager(
                client,
                handler,
                WebSocketClientConfig.ORDER_STATUS_WS_URL
        );

        manager.setHeaders(headers);
        manager.setAutoStartup(false);

        log.info("Starting Order Status WebSocket connection...");
        manager.start();
    }

    public synchronized void stop() {
        if (manager != null) {
            manager.stop();
            manager = null;
        }
    }
}