package com.onepercentgrowth.local_to_smartapi.eventhandling.orderFillStrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.model.OrderResponse;
import com.onepercentgrowth.local_to_smartapi.model.StopLossPrice;
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
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
public class BuyFilledStrategy implements OrderFillStrategy {

    private static final Logger log = LoggerFactory.getLogger(BuyFilledStrategy.class);

    private final OrderRegistry orderRegistry;
    private final OrderExecutionService executionService;
    private final OrderCalculationService calculationService;
    private final TokenManager tokenManager;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties applicationProperties;

    public BuyFilledStrategy(
            OrderExecutionService executionService,
            OrderCalculationService calculationService,
            TokenManager tokenManager,
            OrderRegistry orderRegistry,
            BalanceService balanceService,
            LeverageService leverageService,
            ApplicationProperties applicationProperties
    ) {
        this.executionService = executionService;
        this.calculationService = calculationService;
        this.tokenManager = tokenManager;
        this.orderRegistry = orderRegistry;
        this.balanceService = balanceService;
        this.leverageService = leverageService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return response.getOrderStatusData().getTransactiontype().equals("BUY")
                && ctx.getBuyOrderId().equals(
                response.getOrderStatusData().getOrderid()
        );
    }


    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

//        double executedPrice =
//                Double.parseDouble(response.getOrderStatusData().getPrice());
        BigDecimal executedPrice =
                new BigDecimal(response.getOrderStatusData().getPrice());

        ctx.setBuyPrice(executedPrice);

        log.info(
                "BUY filled | stock={} | orderId={} | qty={} | executedPrice={}",
                ctx.getTradingSymbol(),
                ctx.getBuyOrderId(),
                ctx.getQuantity(),
                executedPrice
        );

        String jwtToken = tokenManager.getValidJwtToken();

//        double sellPrice =
//                calculationService.calculateProfitPrice(executedPrice);

        BigDecimal sellPrice =
                calculationService.calculateProfitPrice(executedPrice);


//        double slPrice =
//                calculationService.calculateStopLossPrice(executedPrice);

        StopLossPrice slPrice =
                calculationService.calculateStopLossPrice(
                        ctx.getBuyPrice(),
                        BigDecimal.valueOf(applicationProperties.getTradingStoplossPercent()),
                        BigDecimal.valueOf(applicationProperties.getTradingStoplossBufferPercent())
                );

        log.info(
                "TP/SL calculated | stock={} | TP={} | SL={}",
                ctx.getTradingSymbol(),
                sellPrice,
                slPrice
        );

        Mono<OrderResponse> sellMono =
                executionService.placeSellOrder(
                                ctx.getTradingSymbol(),
                                ctx.getSymbolToken(),
                                ctx.getQuantity(),
                                sellPrice.doubleValue(),
                                jwtToken
                        )
                        .retry(3)
                        .doOnSuccess(resp -> {

                            ctx.setSellOrderId(
                                    resp.getData().getOrderid()
                            );
                            ctx.setSellVariety("NORMAL");

                            orderRegistry.registerSell(ctx);

                            log.info("SELL order registered: {}", ctx.getSellOrderId());
                        });

        Mono<OrderResponse> slMono =
                executionService.placeStopLossOrder(
                                ctx.getTradingSymbol(),
                                ctx.getSymbolToken(),
                                ctx.getQuantity(),
                                slPrice.triggerPrice().doubleValue(),
                                slPrice.limitPrice().doubleValue(),
                                jwtToken
                        )
                        .retry(3)
                        .doOnSuccess(resp -> {

                            ctx.setStopLossOrderId(
                                    resp.getData().getOrderid()
                            );
                            ctx.setStopLossVariety("STOPLOSS");

                            orderRegistry.registerStopLoss(ctx);

                            log.info("SL order registered: {}", ctx.getStopLossOrderId());
                        });

        // Fire both independently
//        sellMono.subscribe();
//        slMono.subscribe();

        Mono.when(sellMono, slMono).subscribe();

//        int quantity = ctx.getQuantity();
        int quantity = Integer.parseInt(response.getOrderStatusData().getFilledshares());

        String normalizedSymbol =
                Utility.normalize(ctx.getTradingSymbol());

        // 🔑 BALANCE UPDATE
        balanceService.onBuy(
                executedPrice,
                quantity,
                applicationProperties.getLeverageMultiplierToUse(),
                balanceService.getUsableBalance(),
                leverageService.get(normalizedSymbol).multiplier()
        );

        log.info(
                "Balance updated after BUY | stock={} | price={} | qty={} | leveragedUsed={} | maxleverage={}",
                ctx.getTradingSymbol(),
                executedPrice,
                quantity,
                applicationProperties.getLeverageMultiplierToUse(),
                leverageService.get(normalizedSymbol).multiplier()
        );

//        log.info("Balance updated after BUY: price={}, qty={}",
//                executedPrice, quantity);
    }

}

