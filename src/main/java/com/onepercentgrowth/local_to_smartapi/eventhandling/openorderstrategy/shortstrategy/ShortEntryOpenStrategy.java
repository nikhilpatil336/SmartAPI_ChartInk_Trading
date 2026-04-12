package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
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
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

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

//    @Override
//    public void onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
////        int lastSellFilled = ctx.getLastSellFilledQty();
//        int delta = filledQty - ctx.getLastSellFilledQty();
//
//        if (!ctx.isSellOpen()) {
//            ctx.setSellOpen(true);
//            log.info("SHORT ENTRY SELL OPEN | orderId={}", ctx.getSellOrderId());
//        }
//
//        if (delta <= 0) return;
//
//        BigDecimal intenedPrice =
//                new BigDecimal(response.getOrderStatusData().getPrice());
//
//        BigDecimal executedPrice =
//                new BigDecimal(response.getOrderStatusData().getAverageprice());
//
//        ctx.setSellPrice(executedPrice);
//
//        log.info(
//                "SELL for short partial fill | stock={} | delta={} | totalFilled={}",
//                ctx.getTradingSymbol(), delta, filledQty
//        );
//
//        String normalizedSymbol =
//                Utility.normalize(ctx.getTradingSymbol());
//
//        // ===== BALANCE UPDATE (SELL ENTRY) =====
//        balanceService.onShortSell(
//                executedPrice,
//                delta,
//                properties.getLeverageMultiplierToUseForShort(),
//                balanceService.getUsableBalance(),
//                leverageService.get(normalizedSymbol).multiplier()
//        );
//
//        // ===== CALCULATE TARGET & SL =====
////        BigDecimal buyTargetPrice =
////                calculationService.calculateSellProfitPrice(executedPrice);
////
////        BigDecimal buyStopLossPrice =
////                calculationService.calculateSellStopLossPrice(executedPrice);
//        BigDecimal buyPrice =
//                calculationService.calculateSellProfitPrice(intenedPrice);
//
//        StopLossPrice slPrice =
//                calculationService.calculateShortStopLossPrice(
//                        intenedPrice,
//                        BigDecimal.valueOf(properties.getTradingStoplossPercent()),
//                        BigDecimal.valueOf(properties.getTradingStoplossBufferPercent())
//                );
//
//        String jwt = tokenManager.getValidJwtToken();
//
//        // ===== TARGET BUY =====
//        if (!ctx.isBuyPlaced() && !ctx.isBuyOpen()) {
//
//            executionService.placeBuyOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            filledQty,
//                            buyPrice.toString(),
//                            jwt
//                    )
//                    .doOnSuccess(resp -> {
//                        ctx.setBuyOrderId(resp.getData().getOrderid());
//                        ctx.setBuyPlaced(true);
//                        ctx.setBuyOpen(true);
//                        ctx.setBuyPrice(buyPrice);
//                        ctx.setBuyVariety("NORMAL");
//                        orderRegistry.registerBuy(ctx);
//                        log.info("SHORT TARGET BUY placed | {}", ctx.getBuyOrderId());
//                    })
//                    .doOnError(err -> {
//                        log.error("BUY order placement failed sellOrderId={}",
//                                ctx.getSellOrderId(), err);
//                    })
//                    .onErrorResume(err -> Mono.empty())
//                    .subscribe();
//        }else {
//            executionService.modifyBuyOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            filledQty,
//                            buyPrice.toString(),
//                            ctx.getBuyOrderId(),
//                            jwt
//                    )
//                    .doOnSuccess(resp ->
//                            log.info("BUY order modified successfully buyOrderId={}",
//                                    ctx.getBuyOrderId())
//                    )
//                    .doOnError(err ->
//                            log.error("BUY order modification failed buyOrderId={}",
//                                    ctx.getBuyOrderId(), err)
//                    )
//                    .onErrorResume(err -> Mono.empty())
//                    .subscribe();
//        }
//
//        // ===== STOPLOSS BUY =====
//        if (!ctx.isSlPlaced()  && !ctx.isSLOpen()) {
//
//            executionService.placeStopLossOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            filledQty,
//                            slPrice.triggerPrice().doubleValue(),
//                            slPrice.limitPrice().doubleValue(),
//                            jwt
//                    )
//                    .doOnSuccess(resp -> {
//                        ctx.setStopLossOrderId(resp.getData().getOrderid());
//                        ctx.setSlPlaced(true);
//                        ctx.setSLOpen(true);
//                        ctx.setStopLossVariety("STOPLOSS");
//                        ctx.setStoplossLimitPrice(slPrice.limitPrice());
//                        ctx.setStoplossTriggerPrice(slPrice.triggerPrice());
//                        orderRegistry.registerStopLoss(ctx);
//                        log.info("SHORT STOPLOSS BUY placed | {}", ctx.getStopLossOrderId());
//                    })
//                    .subscribe();
//        }else {
//            executionService.modifyStopLossOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            filledQty,
//                            slPrice.triggerPrice().doubleValue(),
//                            slPrice.limitPrice().doubleValue(),
//                            ctx.getStopLossOrderId(),
//                            jwt
//                    )
//                    .doOnSuccess(resp ->
//                            log.info("STOP LOSS modified successfully slOrderId={}",
//                                    ctx.getStopLossOrderId())
//                    )
//                    .doOnError(err ->
//                            log.error("STOP LOSS modification failed slOrderId={}",
//                                    ctx.getStopLossOrderId(), err)
//                    )
//                    .onErrorResume(err -> Mono.empty())
//                    .subscribe();
//        }
//
//        ctx.setLastSellFilledQty(filledQty);
//    }

//    @Override
//    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
//
//        OrderContext.SellUpdate update = ctx.reduceSell(filledQty);
//
//        int delta = update.delta;
//        int remainingQty = update.remainingQty;
//
//        if (!ctx.isSellOpen()) {
//            ctx.setSellOpen(true);
//            log.info("SHORT ENTRY SELL OPEN | orderId={}", ctx.getSellOrderId());
//        }
//
//        if (delta <= 0) return Mono.empty();
//
//        return processShortEntry(ctx, response, delta);
//    }
//
//    private Mono<Void> processShortEntry(
//            OrderContext ctx,
//            OrderStatusResponse response,
//            int delta
//    ) {
//
//        BigDecimal intendedPrice =
//                new BigDecimal(response.getOrderStatusData().getPrice());
//
//        BigDecimal executedPrice =
//                new BigDecimal(response.getOrderStatusData().getAverageprice());
//
//        ctx.setSellPrice(executedPrice);
//
//        log.info("SHORT SELL partial fill | stock={} | delta={}",
//                ctx.getTradingSymbol(), delta);
//
//        String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());
//
//        // ===== UPDATE BALANCE =====
//        Mono<Void> balanceMono = Mono.fromRunnable(() ->
//                balanceService.onShortSell(
//                        executedPrice,
//                        delta,
//                        properties.getLeverageMultiplierToUseForShort(),
//                        balanceService.getUsableBalance(),
//                        leverageService.get(normalizedSymbol).multiplier()
//                )
//        );
//
//        // ===== CALCULATIONS =====
//        BigDecimal buyPrice =
//                calculationService.calculateSellProfitPrice(intendedPrice);
//
//        StopLossPrice slPrice =
//                calculationService.calculateShortStopLossPrice(
//                        intendedPrice,
//                        BigDecimal.valueOf(properties.getTradingStoplossPercent()),
//                        BigDecimal.valueOf(properties.getTradingStoplossBufferPercent())
//                );
//
//        String jwt = tokenManager.getValidJwtToken();
//
//        // ===== TARGET BUY =====
//        Mono<Void> buyMono;
//
//        if (!ctx.isBuyPlaced() && !ctx.isBuyOpen()) {
//
//            buyMono = executionService.placeBuyOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            ctx.getNetPositionQty().get(), // 🔥 FIXED
//                            buyPrice.toString(),
//                            jwt
//                    )
//                    .doOnSuccess(resp -> {
//                        ctx.setBuyOrderId(resp.getData().getOrderid());
//                        ctx.setBuyPlaced(true);
//                        ctx.setBuyOpen(true);
//                        ctx.setBuyPrice(buyPrice);
//                        ctx.setBuyVariety("NORMAL");
//                        orderRegistry.registerBuy(ctx);
//
//                        log.info("SHORT TARGET BUY placed | orderId={}", ctx.getBuyOrderId());
//                    })
//                    .then();
//
//        } else {
//
//            buyMono = executionService.modifyBuyOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            ctx.getNetPositionQty().get(), // 🔥 FIXED
//                            buyPrice.toString(),
//                            ctx.getBuyOrderId(),
//                            jwt
//                    )
//                    .doOnSuccess(resp ->
//                            log.info("SHORT TARGET BUY modified | orderId={}", ctx.getBuyOrderId())
//                    )
//                    .then();
//        }
//
//        // ===== STOPLOSS BUY =====
//        Mono<Void> slMono;
//
//        if (!ctx.isSlPlaced() && !ctx.isSLOpen()) {
//
//            slMono = executionService.placeStopLossOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            ctx.getNetPositionQty().get(), // 🔥 FIXED
//                            slPrice.triggerPrice().doubleValue(),
//                            slPrice.limitPrice().doubleValue(),
//                            jwt
//                    )
//                    .doOnSuccess(resp -> {
//                        ctx.setStopLossOrderId(resp.getData().getOrderid());
//                        ctx.setSlPlaced(true);
//                        ctx.setSLOpen(true);
//                        ctx.setStopLossVariety("STOPLOSS");
//                        ctx.setStoplossLimitPrice(slPrice.limitPrice());
//                        ctx.setStoplossTriggerPrice(slPrice.triggerPrice());
//
//                        orderRegistry.registerStopLoss(ctx);
//
//                        log.info("SHORT SL BUY placed | orderId={}", ctx.getStopLossOrderId());
//                    })
//                    .then();
//
//        } else {
//
//            slMono = executionService.modifyStopLossOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            ctx.getNetPositionQty().get(), // 🔥 FIXED
//                            slPrice.triggerPrice().doubleValue(),
//                            slPrice.limitPrice().doubleValue(),
//                            ctx.getStopLossOrderId(),
//                            jwt
//                    )
//                    .doOnSuccess(resp ->
//                            log.info("SHORT SL modified | orderId={}", ctx.getStopLossOrderId())
//                    )
//                    .then();
//        }
//
//        // ✅ FINAL CHAIN
//        return balanceMono
//                .onErrorResume(e -> {
//                    log.error("Short balance update failed", e);
//                    return Mono.empty();
//                })
//                .then(
//                        buyMono.onErrorResume(e -> {
//                            log.error("SHORT BUY flow failed | orderId={}", ctx.getSellOrderId(), e);
//                            return Mono.empty();
//                        })
//                )
//                .then(
//                        slMono.onErrorResume(e -> {
//                            log.error("SHORT SL flow failed | orderId={}", ctx.getSellOrderId(), e);
//                            return Mono.empty();
//                        })
//                );
//    }

    @Override
    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {

        return Mono.defer(() -> {

            // ✅ ATOMIC GUARD
//            if (!ctx.tryStartSellFill()) {
//                return Mono.empty();
//            }

            int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());

            OrderContext.SellUpdate update = ctx.reduceSell(filledQty);

            int delta = update.delta;

            if (delta <= 0) return Mono.empty();

            if (!ctx.isSellOpen()) {
                ctx.setSellOpen(true);
                log.info("SHORT ENTRY SELL OPEN | orderId={}", ctx.getSellOrderId());
            }

            BigDecimal intendedPrice =
                    new BigDecimal(response.getOrderStatusData().getPrice());

            BigDecimal executedPrice =
                    new BigDecimal(response.getOrderStatusData().getAverageprice());

            ctx.setSellPrice(executedPrice);

            log.info("SHORT SELL partial fill | stock={} | delta={}",
                    ctx.getTradingSymbol(), delta);

            String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());

            // ✅ UPDATE POSITION (CRITICAL)
            ctx.getNetPositionQty().addAndGet(delta);
            ctx.getLastSellFilledQty().set(filledQty);

            // ===== CALCULATIONS =====
            BigDecimal buyPrice =
                    calculationService.calculateSellProfitPrice(intendedPrice);

            StopLossPrice slPrice =
                    calculationService.calculateShortStopLossPrice(
                            intendedPrice,
                            BigDecimal.valueOf(properties.getTradingStoplossPercent()),
                            BigDecimal.valueOf(properties.getTradingStoplossBufferPercent())
                    );

            AtomicBoolean failed = new AtomicBoolean(false);

            String jwt = tokenManager.getValidJwtToken();

            // ===== BUY FLOW =====
            Mono<Void> buyMono = Mono.defer(() -> {
//                        String jwt = tokenManager.getValidJwtToken();

                        if (!ctx.isBuyPlaced() && !ctx.isBuyOpen()) {

                            return executionService.placeBuyOrder(
                                            ctx.getTradingSymbol(),
                                            ctx.getSymbolToken(),
//                                            ctx.getNetPositionQty().get(),
                                            update.remainingQty,
                                            buyPrice.toString(),
                                            jwt
                                    )
                                    .doOnSuccess(resp -> {
                                        ctx.setBuyOrderId(resp.getData().getOrderid());
                                        ctx.setBuyPlaced(true);
                                        ctx.setBuyOpen(true);
                                        ctx.setBuyPrice(buyPrice);
                                        ctx.setBuyVariety("NORMAL");
                                        orderRegistry.registerBuy(ctx);

                                        log.info("SHORT TARGET BUY placed | orderId={}", ctx.getBuyOrderId());
                                    });

                        } else {

                            return executionService.modifyBuyOrder(
                                            ctx.getTradingSymbol(),
                                            ctx.getSymbolToken(),
//                                            ctx.getNetPositionQty().get(),
                                            update.remainingQty,
                                            buyPrice.toString(),
                                            ctx.getBuyOrderId(),
                                            jwt
                                    )
                                    .doOnSuccess(resp ->
                                            log.info("SHORT TARGET BUY modified | orderId={}", ctx.getBuyOrderId())
                                    );
                        }
                    })
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(200)))
                    .onErrorResume(e -> {
                        failed.set(true);
                        ctx.markInconsistent();
                        log.error("SHORT BUY failed", e);
                        return Mono.empty();
                    })
                    .then();

            // ===== SL FLOW =====
            Mono<Void> slMono = Mono.defer(() -> {
//                        String jwt = tokenManager.getValidJwtToken();

                        if (!ctx.isSlPlaced() && !ctx.isSLOpen()) {

                            return executionService.placeStopLossOrder(
                                            ctx.getTradingSymbol(),
                                            ctx.getSymbolToken(),
//                                            ctx.getNetPositionQty().get(),
                                            update.remainingQty,
                                            slPrice.triggerPrice().doubleValue(),
                                            slPrice.limitPrice().doubleValue(),
                                            jwt,
                                    "BUY"
                                    )
                                    .doOnSuccess(resp -> {
                                        ctx.setStopLossOrderId(resp.getData().getOrderid());
                                        ctx.setSlPlaced(true);
                                        ctx.setSLOpen(true);
                                        ctx.setStopLossVariety("STOPLOSS");
                                        ctx.setStoplossLimitPrice(slPrice.limitPrice());
                                        ctx.setStoplossTriggerPrice(slPrice.triggerPrice());

                                        orderRegistry.registerStopLoss(ctx);

                                        log.info("SHORT SL placed | orderId={}", ctx.getStopLossOrderId());
                                    });

                        } else {

                            return executionService.modifyStopLossOrder(
                                            ctx.getTradingSymbol(),
                                            ctx.getSymbolToken(),
//                                            ctx.getNetPositionQty().get(),
                                            update.remainingQty,
                                            slPrice.triggerPrice().doubleValue(),
                                            slPrice.limitPrice().doubleValue(),
                                            ctx.getStopLossOrderId(),
                                            jwt,
                                    "BUY"
                                    )
                                    .doOnSuccess(resp ->
                                            log.info("SHORT SL modified | orderId={}", ctx.getStopLossOrderId())
                                    );
                        }
                    })
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(200)))
                    .onErrorResume(e -> {
                        failed.set(true);
                        ctx.markInconsistent();
                        log.error("SHORT SL failed", e);
                        return Mono.empty();
                    })
                    .then();

            // ===== PARALLEL EXECUTION =====
            Mono<Void> ordersMono = Mono.when(buyMono, slMono);

            // ===== BALANCE =====
            Mono<Void> balanceMono = Mono.fromRunnable(() ->
                    balanceService.onShortSell(
                            executedPrice,
                            delta,
                            properties.getLeverageMultiplierToUseForShort(),
                            balanceService.getUsableBalance(),
                            leverageService.get(normalizedSymbol).multiplier()
                    )
            );

            // ===== FINAL FLOW =====
            return ordersMono
                    .then(Mono.defer(() -> {

                        if (failed.get()) {
                            log.warn("Skipping balance due to failure");
                            return Mono.empty();
                        }

                        return balanceMono
                                .onErrorResume(e -> {
                                    log.error("Short balance update failed", e);
                                    return Mono.empty();
                                });
                    }));
        });
    }
}
