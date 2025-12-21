package com.onepercentgrowth.local_to_smartapi.controller;

import com.onepercentgrowth.local_to_smartapi.model.OrderBookResponse;
import com.onepercentgrowth.local_to_smartapi.model.OrderBookResponse_v2;
import com.onepercentgrowth.local_to_smartapi.model.OrderStatusItem;
import com.onepercentgrowth.local_to_smartapi.model.TradeBookResponse;
import com.onepercentgrowth.local_to_smartapi.service.OrderBookService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderBookController {

    private final OrderBookService orderBookService;

    public OrderBookController(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
    }

    @GetMapping("/orderbook")
    public Mono<List<OrderStatusItem>> getOrderBook() {
        return orderBookService.fetchOrderBook();
    }

    @GetMapping("/tradebook")
    public Mono<TradeBookResponse> getTradeBook() {
        return orderBookService.fetchTradeBook();
    }
}
