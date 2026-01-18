package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy;

import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;

public interface IOpenOrderStrategy {
    boolean supports(OrderContext ctx, OrderStatusResponse response);

    void onFilled(OrderContext ctx, OrderStatusResponse response);
}
