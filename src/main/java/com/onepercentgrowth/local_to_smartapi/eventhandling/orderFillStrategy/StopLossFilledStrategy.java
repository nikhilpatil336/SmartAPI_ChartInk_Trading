package com.onepercentgrowth.local_to_smartapi.eventhandling.orderFillStrategy;

import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StopLossFilledStrategy implements OrderFillStrategy {

    private static final Logger log = LoggerFactory.getLogger(StopLossFilledStrategy.class);

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return response.getOrderStatusData().getTransactiontype().equals("SELL")
                && ctx.getStopLossOrderId().equals(
                response.getOrderStatusData().getOrderid()
        );
    }

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

//        ctx.markStoppedOut();

        log.warn(
                "STOP LOSS HIT for symbol={}, buyOrder={}",
                ctx.getTradingSymbol(),
                ctx.getBuyOrderId()
        );
    }
}

