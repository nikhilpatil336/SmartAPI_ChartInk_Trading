package com.onepercentgrowth.local_to_smartapi.websocket.test;

import com.onepercentgrowth.local_to_smartapi.websocket.IOrderWebSocketConnector;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderWebSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("test")
@Component
public class DummyOrderWebSocketConnector implements IOrderWebSocketConnector {

    private static final Logger log =
            LoggerFactory.getLogger(DummyOrderWebSocketConnector.class);

    private boolean connected = false;

    @Override
    public void connectWithAuth(String jwt, String apiKey, String clientCode) {
        log.info("🧪 Dummy connector: skipping real WebSocket connection");
        connected = true;
    }

    @Override
    public void stop() {
        log.info("🛑 Dummy connector stopped");
        connected = false;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }
}
