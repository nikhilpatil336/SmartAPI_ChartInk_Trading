package com.onepercentgrowth.local_to_smartapi.eventhandling.orderstatushandler;

import com.onepercentgrowth.local_to_smartapi.eventhandling.PendingOrderEventStore;
import com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy.IOpenOrderStrategy;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpenOrderHandler implements IOrderStatusHandler {

    private static final Logger log = LoggerFactory.getLogger(OpenOrderHandler.class);

    private final OrderRegistry orderRegistry;
    private final List<IOpenOrderStrategy> strategies;
    private final PendingOrderEventStore pendingOrderEventStore;

    public OpenOrderHandler(OrderRegistry orderRegistry,
                            List<IOpenOrderStrategy> strategies,
                            PendingOrderEventStore pendingOrderEventStore) {
        this.orderRegistry = orderRegistry;
        this.strategies = strategies;
        this.pendingOrderEventStore = pendingOrderEventStore;
    }

    @Override
    public String status() {
        return "OPEN";
    }

    @Override
    public void handle(OrderStatusResponse response) {

        String orderId = response.getOrderStatusData().getOrderid();

        orderRegistry.getByAnyOrderId(orderId)
                .ifPresentOrElse(ctx -> {

                    strategies.stream()
                            .filter(s -> s.supports(ctx, response))
                            .findFirst()
//                            .ifPresent(s -> s.onFilled(ctx, response));
                            .ifPresent(strategy ->
                                    strategy.onFilled(ctx, response)
                                            .doOnError(e ->
                                                    log.error("Error processing OPEN event | orderId={} | error: {}",
                                                            orderId, e.getMessage())
                                            )
                                            .subscribe() // 🔥 REQUIRED
//                                            .block()
                            );

                },
//                        () -> log.warn("OPEN event ignored, no context for orderId={}", orderId));

                        () -> {

                            pendingOrderEventStore.add(orderId, response);

                            log.warn("Context missing, buffering event | orderId={}",
                                    orderId);
                        });
    }
}

