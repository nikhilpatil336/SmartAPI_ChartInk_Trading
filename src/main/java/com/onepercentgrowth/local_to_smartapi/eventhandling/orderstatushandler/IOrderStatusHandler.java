package com.onepercentgrowth.local_to_smartapi.eventhandling.orderstatushandler;

import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;

public interface IOrderStatusHandler {
    String status();
    void handle(OrderStatusResponse response);
}

