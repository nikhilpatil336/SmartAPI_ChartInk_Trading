package com.onepercentgrowth.local_to_smartapi.websocket.test;

import com.onepercentgrowth.local_to_smartapi.eventhandling.OrderEventQueue;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("test")
public class TestWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TestWebSocketHandler.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final OrderEventQueue eventQueue;

    public TestWebSocketHandler(OrderEventQueue eventQueue) {
        this.eventQueue = eventQueue;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {

        String payload = message.getPayload();
//        log.info("📩 WS payload received: {}", payload);

        try {
            OrderStatusResponse response =
                    mapper.readValue(payload, OrderStatusResponse.class);

            if (response.getOrderStatusData() != null) {
                log.info("📥 Pushing event to queue");
                eventQueue.publish(response);
            }

        } catch (Exception e) {
            log.error("❌ Failed to parse WS message", e);
        }
    }
}