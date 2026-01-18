package com.onepercentgrowth.local_to_smartapi.eventhandling.orderstatushandler;

import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RejectedOrderHandler implements IOrderStatusHandler {

    private static final Logger log = LoggerFactory.getLogger(RejectedOrderHandler.class);

    @Override
    public String status() {
        return "REJECTED";
    }

    @Override
    public void handle(OrderStatusResponse response) {
        log.error("Order Rejected: " +
                response.getErrorMessage());
    }
}

