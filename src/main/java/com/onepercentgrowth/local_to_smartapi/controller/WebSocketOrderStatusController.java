package com.onepercentgrowth.local_to_smartapi.controller;

import com.onepercentgrowth.local_to_smartapi.service.OrderStatusWebSocketService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order-status/ws")
public class WebSocketOrderStatusController {

    private final OrderStatusWebSocketService service;

    public WebSocketOrderStatusController(OrderStatusWebSocketService service) {
        this.service = service;
    }

//    @PostMapping("/start")
//    public String start() {
//        service.start();
//        return "Order Status WebSocket Started";
//    }
//
//    @PostMapping("/stop")
//    public String stop() {
//        service.stop();
//        return "Order Status WebSocket Stopped";
//    }

    @Operation(summary = "Start WebSocket connection")
    @GetMapping("/start")
    public String startWebSocket() {
        service.start();
        return "WebSocket started";
    }

    @Operation(summary = "Stop WebSocket connection")
    @GetMapping("/stop")
    public String stopWebSocket() {
        service.stop();
        return "WebSocket stopped";
    }

    @Operation(summary = "Check WebSocket status")
    @GetMapping("/status")
    public String status() {
        return service.status() ? "CONNECTED" : "DISCONNECTED";
    }
}
