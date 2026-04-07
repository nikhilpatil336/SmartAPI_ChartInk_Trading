package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy;

import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import reactor.core.publisher.Mono;

public interface IOpenOrderStrategy {
    boolean supports(OrderContext ctx, OrderStatusResponse response);

//    void onFilled(OrderContext ctx, OrderStatusResponse response);
    Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response);
}
