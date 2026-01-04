package com.onepercentgrowth.local_to_smartapi.eventhandling.orderStatusHandler;

import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;

public interface OrderStatusHandler {
    String status();
    void handle(OrderStatusResponse response);
}

