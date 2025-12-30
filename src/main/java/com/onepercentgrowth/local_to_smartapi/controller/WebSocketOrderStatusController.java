package com.onepercentgrowth.local_to_smartapi.controller;

//import com.yourapp.websocket.service.OrderStatusWebSocketService;
import com.onepercentgrowth.local_to_smartapi.service.OrderStatusWebSocketService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order-status/ws")
public class WebSocketOrderStatusController {

    private final OrderStatusWebSocketService service;

    public WebSocketOrderStatusController(OrderStatusWebSocketService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public String start() {
        service.start();
        return "Order Status WebSocket Started";
    }

    @PostMapping("/stop")
    public String stop() {
        service.stop();
        return "Order Status WebSocket Stopped";
    }
}
