package com.onepercentgrowth.local_to_smartapi.eventhandling.fillorderstrategy;

import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import reactor.core.publisher.Mono;

public interface IFillOrderStrategy {
    boolean supports(OrderContext ctx, OrderStatusResponse response);
//    void onFilled(OrderContext ctx, OrderStatusResponse response);
    Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response);
}

