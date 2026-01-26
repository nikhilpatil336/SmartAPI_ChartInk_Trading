package com.onepercentgrowth.local_to_smartapi.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onepercentgrowth.local_to_smartapi.eventhandling.OrderEventQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.*;

@Component
public class OrderStatusWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusWebSocketHandler.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private ScheduledExecutorService heartbeatExecutor;
    private WebSocketSession session;

    @Autowired
    private OrderEventQueue eventQueue;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.session = session;
        log.info("Connected to Order Status WebSocket");

        startHeartbeat();
    }

//    @Override
//    protected void handleTextMessage(WebSocketSession session, TextMessage message)
//            throws Exception {
//
//        try {
//            String payload = message.getPayload();
//
//            if ("pong".equalsIgnoreCase(payload)) {
//                return;
//            }
//
//            OrderStatusResponse response =
//                    mapper.readValue(payload, OrderStatusResponse.class);
//
//            // 1️⃣ Connection / handshake
//            if ("AB00".equals(response.getOrderStatus())) {
//                System.out.println("🔐 Order WS authenticated successfully");
//                return;
//            }
//
//            // 2️⃣ Actual order update
//            if (response.getOrderStatusData() != null) {
//                System.out.println("📦 Order Update Received");
//                System.out.println("   Order ID: " +
//                        response.getOrderStatusData().getOrderid());
//                System.out.println("   Status: " +
//                        response.getOrderStatusData().getStatus());
//                return;
//            }
//
//            // 3️⃣ Error case
//            if (response.getErrorMessage() != null) {
//                System.err.println("❌ WS Error: " + response.getErrorMessage());
//            }
//
//        } catch (Exception e) {
//            // NEVER crash the socket
//            System.err.println("⚠ Failed to parse WS message: " + e.getMessage());
//        }
//    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {

        try {

            String payload = message.getPayload();

            if ("pong".equalsIgnoreCase(payload)) return;

            log.info("websocket raw text payload: {}", payload);

            OrderStatusResponse response =
                    mapper.readValue(payload, OrderStatusResponse.class);

//            log.info("websocket response: {}", response);
//            log.info("websocket response status: {}", response.getOrderStatusData().getStatus());

            // Handshake / auth
            if ("AB00".equals(response.getOrderStatus())) {
                log.info("Order WS authenticated");
                return;
            }

            // Only enqueue real order events
            if (response.getOrderStatusData() != null) {
                eventQueue.publish(response);
            }

        } catch (Exception e) {
            log.error("Failed WS message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket closed: {}", status);
        stopHeartbeat();
        this.session = null;
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket error: " + exception.getMessage());
    }

    /* ---------------- Heartbeat ---------------- */

    private void startHeartbeat() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                if (session != null && session.isOpen()) {
                    session.sendMessage(new TextMessage("ping"));
                }
            } catch (Exception e) {
                log.error("Heartbeat failed: " + e.getMessage());
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }
    }
}
