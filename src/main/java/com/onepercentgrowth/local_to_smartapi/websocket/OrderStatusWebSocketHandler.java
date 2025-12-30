package com.onepercentgrowth.local_to_smartapi.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
//import com.yourapp.websocket.model.OrderStatusResponse;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.*;

public class OrderStatusWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private ScheduledExecutorService heartbeatExecutor;
    private WebSocketSession session;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.session = session;
        System.out.println("✅ Connected to Order Status WebSocket");

        startHeartbeat();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {

        try {
            String payload = message.getPayload();

            if ("pong".equalsIgnoreCase(payload)) {
                return;
            }

            OrderStatusResponse response =
                    mapper.readValue(payload, OrderStatusResponse.class);

            // 1️⃣ Connection / handshake
            if ("AB00".equals(response.getOrderStatus())) {
                System.out.println("🔐 Order WS authenticated successfully");
                return;
            }

            // 2️⃣ Actual order update
            if (response.getOrderStatusData() != null) {
                System.out.println("📦 Order Update Received");
                System.out.println("   Order ID: " +
                        response.getOrderStatusData().getOrderid());
                System.out.println("   Status: " +
                        response.getOrderStatusData().getStatus());
                return;
            }

            // 3️⃣ Error case
            if (response.getErrorMessage() != null) {
                System.err.println("❌ WS Error: " + response.getErrorMessage());
            }

        } catch (Exception e) {
            // NEVER crash the socket
            System.err.println("⚠ Failed to parse WS message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("❌ WebSocket closed: " + status);
        stopHeartbeat();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.err.println("🚨 WebSocket error: " + exception.getMessage());
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
                System.err.println("Heartbeat failed: " + e.getMessage());
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }
    }
}
