package com.onepercentgrowth.local_to_smartapi.eventhandling;

import com.onepercentgrowth.local_to_smartapi.eventhandling.orderstatushandler.IOrderStatusHandler;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class OrderEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventDispatcher.class);

    private final OrderEventQueue queue;
    private final ExecutorService workerPool;
    private final Map<String, IOrderStatusHandler> handlers;

    private volatile boolean running = true;
    private Thread dispatcherThread;

    @Autowired
    public OrderEventDispatcher(
            OrderEventQueue queue,
            List<IOrderStatusHandler> handlerList
    ) {
        this.queue = queue;
        this.workerPool = Executors.newFixedThreadPool(10);

        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        h -> h.status().toUpperCase(),
                        h -> h
                ));
    }

    @PostConstruct
    public void start() {
        dispatcherThread = new Thread(this::dispatchLoop, "order-event-dispatcher");
        dispatcherThread.start();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down OrderEventDispatcher...");
        running = false;

        dispatcherThread.interrupt();

        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Forcing worker pool shutdown...");
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    private void dispatchLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Blocks if queue is empty
                OrderStatusResponse event = queue.take();

                workerPool.submit(() -> process(event));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("OrderEventDispatcher stopped.");
    }

    private void process(OrderStatusResponse event) {

        if (event == null || event.getOrderStatusData() == null) {
            log.error("Invalid OrderStatusResponse: {}", event);
            return;
        }

        String status = event.getOrderStatusData().getStatus();
        if (status == null) {
            log.error("Order status is null: {}", event);
            return;
        }

        IOrderStatusHandler handler = handlers.get(status.toUpperCase());

        if (handler == null) {
            log.error("No handler for status: {}", status);
            return;
        }

        try {
            handler.handle(event);
        } catch (Exception e) {
            log.error("Handler failed for status: {} due to error", status, e);
        }
    }
}
