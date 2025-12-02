package com.onepercentgrowth.local_to_smartapi.controller;

import com.onepercentgrowth.local_to_smartapi.model.OrderResponse;
import com.onepercentgrowth.local_to_smartapi.model.WebhookRequest;
import com.onepercentgrowth.local_to_smartapi.service.OrderService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // BUY endpoint
//    @PostMapping("/buy")
//    public Mono<OrderResponse> placeBuyOrder(
//            @RequestParam String symbol,
//            @RequestParam int quantity,
//            @RequestHeader("Authorization") String token
//    ) {
//        // token should be: "Bearer <jwt>"
//        String authToken = token.replace("Bearer ", "").trim();
//        return orderService.placeOrder(symbol, quantity, "BUY", authToken);
//    }

    // SELL endpoint
//    @PostMapping("/sell")
//    public Mono<OrderResponse> placeSellOrder(
//            @RequestParam String symbol,
//            @RequestParam int quantity,
//            @RequestHeader("Authorization") String token
//    ) {
//        String authToken = token.replace("Bearer ", "").trim();
//        return orderService.placeOrder(symbol, quantity, "SELL", authToken);
//    }

    @PostMapping("/webhook-order")
    public Mono<OrderResponse> placeWebhookOrder(@RequestBody WebhookRequest webhookRequest) {
        return orderService.placeWebhookOrder(webhookRequest);
    }
}

