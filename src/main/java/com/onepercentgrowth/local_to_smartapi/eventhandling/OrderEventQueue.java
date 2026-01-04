package com.onepercentgrowth.local_to_smartapi.eventhandling;

import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class OrderEventQueue {

    private final BlockingQueue<OrderStatusResponse> queue =
            new LinkedBlockingQueue<>(10_000);

    public void publish(OrderStatusResponse event) {
        queue.offer(event); // wakes ONE waiting thread
    }

    public OrderStatusResponse take() throws InterruptedException {
        return queue.take(); // blocks if empty
    }
}

