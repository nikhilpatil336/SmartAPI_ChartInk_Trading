package com.onepercentgrowth.local_to_smartapi.eventhandling;

import com.onepercentgrowth.local_to_smartapi.eventhandling.orderStatusHandler.OrderStatusHandler;
import com.onepercentgrowth.local_to_smartapi.service.OrderService_v2;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
public class OrderEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventDispatcher.class);

    private final OrderEventQueue queue;
    private final ExecutorService workerPool;

    private final Map<String, OrderStatusHandler> handlers;

    @Autowired
    public OrderEventDispatcher(
            OrderEventQueue queue,
            List<OrderStatusHandler> handlerList
    ) {
        this.queue = queue;
        this.workerPool = Executors.newFixedThreadPool(10);

        // Build lookup map once
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        h -> h.status().toUpperCase(),
                        h -> h
                ));
    }

    @PostConstruct
    public void start() {
        Thread dispatcher = new Thread(this::dispatchLoop);
        dispatcher.setDaemon(true);
        dispatcher.start();
    }

    private void dispatchLoop() {
        while (true) {
            try {
                // BLOCKS here if queue is empty
                OrderStatusResponse event = queue.take();

                // wakes up ONLY when data arrives
                workerPool.submit(() -> process(event));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void process(OrderStatusResponse event) {
        String status = event.getOrderStatusData().getOrderstatus();

        OrderStatusHandler handler =
                handlers.get(status.toUpperCase());

        if (handler == null) {
            log.error("No handler for status: {}", status);
            return;
        }

        try {
            handler.handle(event);
        } catch (Exception e) {
            log.error("Handler failed for status: {} due to the error: {}", status, e.getMessage());
        }
    }
}

