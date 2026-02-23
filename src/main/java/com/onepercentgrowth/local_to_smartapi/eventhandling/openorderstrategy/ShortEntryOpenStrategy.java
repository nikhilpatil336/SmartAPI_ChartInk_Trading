package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.service.BalanceService;
import com.onepercentgrowth.local_to_smartapi.service.LeverageService;
import com.onepercentgrowth.local_to_smartapi.service.OrderCalculationService;
import com.onepercentgrowth.local_to_smartapi.service.OrderExecutionService;
import com.onepercentgrowth.local_to_smartapi.utility.Utility;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ShortEntryOpenStrategy implements IOpenOrderStrategy {

    private static final Logger log =
            LoggerFactory.getLogger(ShortEntryOpenStrategy.class);

    private final OrderExecutionService executionService;
    private final OrderCalculationService calculationService;
    private final TokenManager tokenManager;
    private final OrderRegistry orderRegistry;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties properties;

    public ShortEntryOpenStrategy(
            OrderExecutionService executionService,
            OrderCalculationService calculationService,
            TokenManager tokenManager,
            OrderRegistry orderRegistry,
            BalanceService balanceService,
            LeverageService leverageService,
            ApplicationProperties properties) {

        this.executionService = executionService;
        this.calculationService = calculationService;
        this.tokenManager = tokenManager;
        this.orderRegistry = orderRegistry;
        this.balanceService = balanceService;
        this.leverageService = leverageService;
        this.properties = properties;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return ctx.isShort()
                && "SELL".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getSellOrderId());
    }

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
        int delta = filledQty - ctx.getLastSellFilledQty();

        if (!ctx.isSellOpen()) {
            ctx.setSellOpen(true);
            log.info("SHORT ENTRY SELL OPEN | orderId={}", ctx.getSellOrderId());
        }

        if (delta <= 0) return;

        BigDecimal executedPrice =
                new BigDecimal(response.getOrderStatusData().getAverageprice());

        ctx.setSellPrice(executedPrice);

        String normalizedSymbol =
                Utility.normalize(ctx.getTradingSymbol());

        // ===== BALANCE UPDATE (SELL ENTRY) =====
        balanceService.onSell(
                executedPrice,
                BigDecimal.ZERO,
                delta,
                properties.getLeverageMultiplierToUseForShort(),
                balanceService.getUsableBalance(),
                leverageService.get(normalizedSymbol).multiplier()
        );

        // ===== CALCULATE TARGET & SL =====
        BigDecimal buyTargetPrice =
                calculationService.calculateSellProfitPrice(executedPrice);

        BigDecimal buyStopLossPrice =
                calculationService.calculateSellStopLossPrice(executedPrice);

        String jwt = tokenManager.getValidJwtToken();

        // ===== TARGET BUY =====
        if (!ctx.isBuyPlaced()) {

            executionService.placeBuyOrder(
                            ctx.getTradingSymbol(),
                            ctx.getSymbolToken(),
                            filledQty,
                            buyTargetPrice.toString(),
                            jwt
                    )
                    .doOnSuccess(resp -> {
                        ctx.setBuyOrderId(resp.getData().getOrderid());
                        ctx.setBuyPlaced(true);
                        ctx.setBuyOpen(true);
                        ctx.setBuyPrice(buyTargetPrice);
                        orderRegistry.registerBuy(ctx);
                        log.info("SHORT TARGET BUY placed | {}", ctx.getBuyOrderId());
                    })
                    .subscribe();
        }

        // ===== STOPLOSS BUY =====
        if (!ctx.isSlPlaced()) {

            executionService.placeStopLossOrder(
                            ctx.getTradingSymbol(),
                            ctx.getSymbolToken(),
                            filledQty,
                            buyStopLossPrice.doubleValue(),
                            buyStopLossPrice.doubleValue(),
                            jwt
                    )
                    .doOnSuccess(resp -> {
                        ctx.setStopLossOrderId(resp.getData().getOrderid());
                        ctx.setSlPlaced(true);
                        ctx.setSLOpen(true);
                        orderRegistry.registerStopLoss(ctx);
                        log.info("SHORT STOPLOSS BUY placed | {}", ctx.getStopLossOrderId());
                    })
                    .subscribe();
        }

        ctx.setLastSellFilledQty(filledQty);
    }
}
