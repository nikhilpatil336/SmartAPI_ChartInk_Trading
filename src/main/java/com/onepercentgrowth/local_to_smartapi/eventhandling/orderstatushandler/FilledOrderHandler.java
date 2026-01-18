package com.onepercentgrowth.local_to_smartapi.eventhandling.orderstatushandler;

import com.onepercentgrowth.local_to_smartapi.eventhandling.fillorderstrategy.IFillOrderStrategy;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FilledOrderHandler implements IOrderStatusHandler {

    private static final Logger log = LoggerFactory.getLogger(FilledOrderHandler.class);

    private final OrderRegistry orderRegistry;
    private final List<IFillOrderStrategy> strategies;

    public FilledOrderHandler(OrderRegistry orderRegistry, List<IFillOrderStrategy> strategies) {
        this.orderRegistry = orderRegistry;
        this.strategies = strategies;
    }

    @Override
    public String status() {
        return "COMPLETE";
    }

    @Override
    public void handle(OrderStatusResponse response) {

        log.info("websocket status response: {} ", response);

        String orderId =
                response.getOrderStatusData().getOrderid();

        orderRegistry.getByAnyOrderId(orderId)
                .ifPresentOrElse(ctx -> {

                    log.info("Retrieved object from registry: {}", ctx);

                    strategies.stream()
                            .filter(s -> s.supports(ctx, response))
                            .findFirst()
                            .ifPresent(s -> s.onFilled(ctx, response));

                }, () -> log.warn("No OrderContext found for orderId={}", orderId));

    }
}


