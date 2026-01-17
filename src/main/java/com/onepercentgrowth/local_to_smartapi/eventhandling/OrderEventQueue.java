package com.onepercentgrowth.local_to_smartapi.eventhandling;

import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class OrderEventQueue {

    private static final Logger log = LoggerFactory.getLogger(OrderEventQueue.class);

    private final BlockingQueue<OrderStatusResponse> queue =
            new LinkedBlockingQueue<>(10_000);

    public void publish(OrderStatusResponse event) {
        if (!queue.offer(event)) {
            log.error("OrderEventQueue full. Dropping event: {}", event);
        }
    }


    public OrderStatusResponse take() throws InterruptedException {
        return queue.take(); // blocks if empty
    }
}

