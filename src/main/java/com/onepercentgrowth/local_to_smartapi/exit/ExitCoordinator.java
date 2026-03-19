package com.onepercentgrowth.local_to_smartapi.exit;

import com.onepercentgrowth.local_to_smartapi.execution.OrderActionExecutor;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import org.springframework.stereotype.Component;

@Component
public class ExitCoordinator {

    private final OrderActionExecutor actionExecutor;
    private final ApplicationProperties properties;

    public ExitCoordinator(OrderActionExecutor actionExecutor,
                           ApplicationProperties properties) {
        this.actionExecutor = actionExecutor;
        this.properties = properties;
    }

    public void onExitStarted(OrderContext ctx, ExitType type) {

        if (!properties.isTradingAllowDoubleExit()) {
            if (type == ExitType.SELL && ctx.getStopLossOrderId() != null) {
                actionExecutor.cancelOrder(
                        ctx.getStopLossOrderId(),
                        ctx.getStopLossVariety(),
                        "SELL"
                ).subscribe();
            }

            if (type == ExitType.STOPLOSS && ctx.getSellOrderId() != null) {
                actionExecutor.cancelOrder(
                        ctx.getSellOrderId(),
                        ctx.getSellVariety(),
                        "STOPLOSS"
                ).subscribe();
            }
        }

        // Always cancel remaining BUY
        if (ctx.isBuyOpen()) {
            actionExecutor.cancelOrder(
                    ctx.getBuyOrderId(),
                    ctx.getBuyVariety(),
                    "BUY"
            ).subscribe();
        }
    }
}

