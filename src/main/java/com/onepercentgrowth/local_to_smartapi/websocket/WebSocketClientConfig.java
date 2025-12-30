package com.onepercentgrowth.local_to_smartapi.websocket;

//import com.yourapp.websocket.handler.OrderStatusWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.http.HttpHeaders;

@Configuration
public class WebSocketClientConfig {

    public static final String ORDER_STATUS_WS_URL =
            "wss://tns.angelone.in/smart-order-update";

//    public String getORDER_STATUS_WS_URL() {
//        return ORDER_STATUS_WS_URL;
//    }

    @Bean
    public StandardWebSocketClient standardWebSocketClient() {
        return new StandardWebSocketClient();
    }

    @Bean
    public WebSocketHandler orderStatusWebSocketHandler() {
        return new OrderStatusWebSocketHandler();
    }

//    @Bean
//    public WebSocketConnectionManager orderStatusConnectionManager(
//            StandardWebSocketClient client,
//            WebSocketHandler orderStatusWebSocketHandler
//    ) {
//
//        WebSocketConnectionManager manager =
//                new WebSocketConnectionManager(
//                        client,
//                        orderStatusWebSocketHandler,
//                        ORDER_STATUS_WS_URL
//                );
//
//        manager.setAutoStartup(false); // controlled manually
//        manager.setHeaders(authHeaders());
//
//        return manager;
//    }
//
//    private HttpHeaders authHeaders() {
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Authorization", "Bearer YOUR_AUTH_TOKEN");
//        return headers;
//    }
}
