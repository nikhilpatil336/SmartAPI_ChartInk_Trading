package com.onepercentgrowth.local_to_smartapi.websocket;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderWebSocketConnector {

    private static final Logger log = LoggerFactory.getLogger(OrderWebSocketConnector.class);

    private final ReactorNettyWebSocketClient client;
    private final OrderStatusWebSocketHandler handler;
    private final TokenManager tokenManager;

    private static final String WS_URL = "wss://tns.angelone.in/smart-order-update";

    private final int maxRetries = 10;
    private final long retryDelayMs = 10000;

    private final AtomicInteger retryCount = new AtomicInteger(0);

    private volatile boolean connected = false;
    private Disposable connectionDisposable;

    public OrderWebSocketConnector(
            OrderStatusWebSocketHandler handler,
            TokenManager tokenManager
    ) {
        this.client = new ReactorNettyWebSocketClient();
        this.handler = handler;
        this.tokenManager = tokenManager;
    }

//    public void connect() {
//        log.info("Attempting WebSocket connection... Attempt: {}", retryCount.get() + 1);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Authorization", "Bearer YOUR_AUTH_TOKEN");
//
//        client.execute(
//                        URI.create(WS_URL),
//                        headers,
//                        session -> {
//
//                            log.info("✅ WebSocket connected successfully");
//                            retryCount.set(0);
//
//                            // Delegate handling to your existing handler
//                            return handler.handle(session)
//                                    .doOnError(ex -> log.error("Handler error", ex))
//                                    .doOnTerminate(() -> {
//                                        log.warn("WebSocket session terminated");
//                                        scheduleReconnect(apiKey, clientCode);
//                                    });
//                        }
//                )
//                .doOnError(ex -> {
//                    log.error("❌ WebSocket connection failed", ex);
//                    scheduleReconnect(apiKey, clientCode);
//                })
//                .subscribe();
//    }

    public void connectWithAuth(String jwt, String apiKey, String clientCode) {

        log.info("Attempting WebSocket connection... Attempt: {}", retryCount.get() + 1);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jwt);
        headers.add("x-api-key", apiKey);
        headers.add("x-client-code", clientCode);

        connectionDisposable = client.execute(
                        URI.create(WS_URL),
                        headers,
                        session -> {

                            connected = true;
                            retryCount.set(0);

                            log.info("✅ WebSocket connected successfully");

                            return handler.handle(session)
                                    .doOnError(ex -> {
                                        connected = false;
                                        log.error("❌ WebSocket runtime error", ex);
                                        scheduleReconnect(apiKey, clientCode);
                                    })
                                    .doFinally(signal -> {
                                        connected = false;
                                        log.warn("⚠ WebSocket session ended: {}", signal);
                                    });
                        }
                )
                .doOnError(ex -> {
                    connected = false;
                    log.error("❌ WebSocket connection failed", ex);
                    scheduleReconnect(apiKey, clientCode);
                })
                .subscribe(
                        null,
                        ex -> log.error("❌ Final WS error (onErrorDropped fix)", ex)
                );

    }

//    private void scheduleReconnect() {
//        if (retryCount.incrementAndGet() > maxRetries) {
//            log.error("🚫 Max retry attempts reached. Stopping reconnect.");
//            return;
//        }
//
//        log.info("🔁 Reconnecting in {} ms (Attempt {})", retryDelayMs, retryCount.get());
//
//        Mono.delay(Duration.ofMillis(retryDelayMs))
//                .doOnTerminate(this::connect)
//                .subscribe();
//    }

    private void scheduleReconnect(String apiKey, String clientCode) {

        if (retryCount.incrementAndGet() > maxRetries) {
            log.error("🚫 Max retry attempts reached. Stopping reconnect.");
            return;
        }

        log.info("🔁 Reconnecting in {} ms (Attempt {})", retryDelayMs, retryCount.get());

        Mono.delay(Duration.ofMillis(retryDelayMs))
                .flatMap(i -> tokenManager.getValidJwtTokenAsync())
                .doOnNext(newJwt -> {
                    log.info("🔐 Refreshed JWT for reconnect");
                    connectWithAuth(newJwt, apiKey, clientCode);
                })
                .subscribe();
    }

    private void scheduleReconnectWithFreshToken(String apiKey, String clientCode) {

        if (retryCount.incrementAndGet() > maxRetries) {
            log.error("Max retry attempts reached");
            return;
        }

        Mono.delay(Duration.ofMillis(retryDelayMs))
                .flatMap(i -> tokenManager.getValidJwtTokenAsync())
                .doOnNext(newJwt -> {
                    log.info("🔐 Refreshed JWT for reconnect");
                    connectWithAuth(newJwt, apiKey, clientCode);
                })
                .subscribe();
    }

    public void stop() {
        if (connectionDisposable != null && !connectionDisposable.isDisposed()) {
            connectionDisposable.dispose();
            connected = false;
            log.info("🛑 WebSocket manually stopped");
        }
    }

    public boolean isConnected() {
        return connected;
    }

//    public void reconnect() {
//        log.info("🔄 Manual reconnect triggered");
//        scheduleReconnect();
//    }
}