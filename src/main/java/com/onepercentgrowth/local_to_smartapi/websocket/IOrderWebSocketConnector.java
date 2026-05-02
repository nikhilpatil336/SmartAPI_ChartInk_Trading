package com.onepercentgrowth.local_to_smartapi.websocket;

public interface IOrderWebSocketConnector {
    void connectWithAuth(String jwt, String apiKey, String clientCode);

    void stop();

    boolean isConnected();
}
