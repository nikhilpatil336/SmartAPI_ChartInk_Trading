package com.onepercentgrowth.local_to_smartapi.controller;

import com.onepercentgrowth.local_to_smartapi.model.OrderResponse;
import com.onepercentgrowth.local_to_smartapi.model.OrderStatusResponse;
import com.onepercentgrowth.local_to_smartapi.model.WebhookRequest;
import com.onepercentgrowth.local_to_smartapi.service.OrderService;
import com.onepercentgrowth.local_to_smartapi.service.OrderService_v2;
import com.onepercentgrowth.local_to_smartapi.service.OrderStatusWebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final OrderService_v2 orderService_v2;

    public OrderController(OrderService orderService, OrderService_v2 orderServiceV2) {
        this.orderService = orderService;
        this.orderService_v2 = orderServiceV2;
    }

    @PostMapping("/webhook-order")
    public Mono<OrderResponse> placeWebhookOrder(@RequestBody WebhookRequest webhookRequest) {
        log.info("placeWebhookOrder request: {}", webhookRequest);
        return orderService.placeWebhookOrder(webhookRequest);
    }

    @PostMapping("/buy")
    public Mono<OrderResponse> chartinkBuyOrder(@RequestBody WebhookRequest webhookRequest) {
        log.info("placeWebhookOrder request: {}", webhookRequest);
        return orderService_v2.chartinkBuyOrder(webhookRequest);
    }

    @PostMapping("/buy/webhookstatus")
    public Mono<OrderResponse> chartinkSimpleBuyOrder(@RequestBody WebhookRequest webhookRequest) {
        return orderService_v2.chartinkSimpleBuyOrder(webhookRequest);
    }

    @PostMapping("/modify")
    public Mono<OrderResponse> chartinkModifyOrder(@RequestBody WebhookRequest webhookRequest) {
        return orderService.placeWebhookOrder(webhookRequest);
    }

    @PostMapping("/cancel")
    public Mono<OrderResponse> chartinkCancelOrder(@RequestBody WebhookRequest webhookRequest) {
        return orderService.placeWebhookOrder(webhookRequest);
    }

    @GetMapping(value = "/{orderId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> getOrderStatus(@PathVariable String orderId) {
        return orderService.getOrderStatus(orderId);
    }
}

