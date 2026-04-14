package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.properties.AngelApiProperties;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusWebSocketHandler;
import com.onepercentgrowth.local_to_smartapi.websocket.WebSocketClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderWebSocketConnector;

//@Service
//public class OrderStatusWebSocketService {
//
//    private static final Logger log = LoggerFactory.getLogger(OrderStatusWebSocketService.class);
//
//    private final StandardWebSocketClient client;
//    private final WebSocketHandler handler;
//    private final TokenManager tokenManager;
//
//    private WebSocketConnectionManager manager;
//
////    private WebSocketClientConfig webSocketClientConfig;
//
//    public OrderStatusWebSocketService(
//            StandardWebSocketClient client,
//            WebSocketHandler handler,
//            TokenManager tokenManager
//    ) {
//        this.client = client;
//        this.handler = handler;
//        this.tokenManager = tokenManager;
//    }
//
//    public void start() {
//
//        if (manager != null && manager.isRunning()) {
//            log.info("Order Status WebSocket already running");
//            return;
//        }
//
//        String jwt = tokenManager.getValidJwtToken();
//        log.info("Using JWT (first 10 chars): {}...", jwt.substring(0, 10));
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Authorization", "Bearer " + jwt);
//
//        manager = new WebSocketConnectionManager(
//                client,
//                handler,
//                WebSocketClientConfig.ORDER_STATUS_WS_URL
//        );
//
//        manager.setHeaders(headers);
//        manager.setAutoStartup(false);
//
//        log.info("Starting Order Status WebSocket connection...");
//        manager.start();
//    }
//
//    public synchronized void stop() {
//        if (manager != null) {
//            manager.stop();
//            manager = null;
//        }
//    }
//}

//@Service
//public class OrderStatusWebSocketService {
//
//    private static final Logger log =
//            LoggerFactory.getLogger(OrderStatusWebSocketService.class);
//
//    private final StandardWebSocketClient client;
//    private final OrderStatusWebSocketHandler handler;
//    private final TokenManager tokenManager;
//    private final ApplicationProperties props;
//    private final AngelApiProperties angelApiProperties;
//
//    private WebSocketConnectionManager manager;
//
//    public OrderStatusWebSocketService(
//            StandardWebSocketClient client,
//            OrderStatusWebSocketHandler handler,
//            TokenManager tokenManager,
//            ApplicationProperties props,
//            AngelApiProperties angelApiProperties
//    ) {
//        this.client = client;
//        this.handler = handler;
//        this.tokenManager = tokenManager;
//        this.props = props;
//        this.angelApiProperties = angelApiProperties;
//    }
//
//    public void start() {
//
//        if (manager != null && manager.isRunning()) {
//            log.info("Order Status WebSocket already running");
//            return;
//        }
//
//        // 🔥 NON-BLOCKING TOKEN FETCH
//        tokenManager.getValidJwtTokenAsync()
//                .doOnSuccess(jwt -> {
//
//                    log.info("Starting Order Status WebSocket");
//
//                    HttpHeaders headers = new HttpHeaders();
//                    headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
//                    headers.add("x-api-key", angelApiProperties.getPrivateKey());
//                    headers.add("x-client-code", angelApiProperties.getClientId());
//
//                    manager = new WebSocketConnectionManager(
//                            client,
//                            handler,
//                            WebSocketClientConfig.ORDER_STATUS_WS_URL
//                    );
//
//                    manager.setHeaders(headers);
//                    manager.setAutoStartup(false);
//                    manager.start();
//
//                })
//                .doOnError(e ->
//                        log.error("WebSocket NOT started due to auth failure", e)
//                )
//                .subscribe();
//    }
//
//    public void stop() {
//        if (manager != null) {
//            manager.stop();
//            manager = null;
//        }
//    }
//}




@Service
public class OrderStatusWebSocketService {

    private static final Logger log =
            LoggerFactory.getLogger(OrderStatusWebSocketService.class);

    private final TokenManager tokenManager;
    private final AngelApiProperties angelApiProperties;
    private final OrderWebSocketConnector connector;

    public OrderStatusWebSocketService(
            TokenManager tokenManager,
            AngelApiProperties angelApiProperties,
            OrderWebSocketConnector connector
    ) {
        this.tokenManager = tokenManager;
        this.angelApiProperties = angelApiProperties;
        this.connector = connector;
    }

    public void start() {

        log.info("Initializing Order Status WebSocket...");

        tokenManager.getValidJwtTokenAsync()
                .doOnSuccess(jwt -> {

                    log.info("✅ Token received, starting WebSocket");

                    connector.connectWithAuth(
                            jwt,
                            angelApiProperties.getPrivateKey(),
                            angelApiProperties.getClientId()
                    );

                })
                .doOnError(e ->
                        log.error("❌ WebSocket NOT started due to auth failure", e)
                )
                .subscribe();
    }

    public void stop() {
        log.info("Stopping WebSocket (handled by reactive lifecycle)");
        connector.stop();
    }

    public boolean status() {
        return connector.isConnected();
    }
}