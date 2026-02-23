package com.onepercentgrowth.local_to_smartapi.eventhandling.fillorderstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.service.BalanceService;
import com.onepercentgrowth.local_to_smartapi.service.LeverageService;
import com.onepercentgrowth.local_to_smartapi.service.OrderExecutionService;
import com.onepercentgrowth.local_to_smartapi.utility.Utility;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ShortBuyStopLossFilledStrategy implements IFillOrderStrategy {

    private static final Logger log =
            LoggerFactory.getLogger(ShortBuyStopLossFilledStrategy.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties applicationProperties;

    public ShortBuyStopLossFilledStrategy(
            OrderExecutionService executionService,
            TokenManager tokenManager,
            BalanceService balanceService,
            LeverageService leverageService,
            ApplicationProperties applicationProperties
    ) {
        this.executionService = executionService;
        this.tokenManager = tokenManager;
        this.balanceService = balanceService;
        this.leverageService = leverageService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return ctx.isShort()
                && "BUY".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getStopLossOrderId());
    }

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

        if (ctx.isTradeCompleted()) return;
        ctx.setTradeCompleted(true);

        BigDecimal executedPrice =
                new BigDecimal(response.getOrderStatusData().getAverageprice());

        int qty =
                Integer.parseInt(response.getOrderStatusData().getFilledshares());

        log.warn("SHORT STOPLOSS HIT | stock={}", ctx.getTradingSymbol());

        cancelTarget(ctx);

        String normalizedSymbol =
                Utility.normalize(ctx.getTradingSymbol());

        balanceService.onBuy(
                executedPrice,
                qty,
                applicationProperties.getLeverageMultiplierToUseForShort(),
                balanceService.getUsableBalance(),
                leverageService.get(normalizedSymbol).multiplier()
        );
    }

    private void cancelTarget(OrderContext ctx) {
        if (ctx.getBuyOrderId() == null) return;

        String jwt = tokenManager.getValidJwtToken();

        executionService.placeCancelOrder(
                ctx.getBuyOrderId(),
                ctx.getBuyVariety(),
                jwt,
                "BUY"
        ).subscribe();
    }
}
