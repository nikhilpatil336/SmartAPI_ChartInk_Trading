package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.execution.OrderActionExecutor;
import com.onepercentgrowth.local_to_smartapi.exit.AggressiveExitManager;
import com.onepercentgrowth.local_to_smartapi.exit.ExitType;
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
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SellOpenStrategy implements IOpenOrderStrategy {

    private static final Logger log = LoggerFactory.getLogger(SellOpenStrategy.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties properties;
    private final OrderActionExecutor orderActionExecutor;
    private final AggressiveExitManager aggressiveExitManager;

    public SellOpenStrategy(OrderExecutionService executionService,
                            TokenManager tokenManager,
                            BalanceService balanceService,
                            LeverageService leverageService,
                            ApplicationProperties properties,
                            OrderActionExecutor orderActionExecutor,
                            AggressiveExitManager aggressiveExitManager) {
        this.executionService = executionService;
        this.tokenManager = tokenManager;
        this.balanceService = balanceService;
        this.leverageService = leverageService;
        this.properties = properties;
        this.orderActionExecutor = orderActionExecutor;
        this.aggressiveExitManager = aggressiveExitManager;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return ctx.isLong()
                &&"SELL".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getSellOrderId());
    }

//    @Override
//    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
//        int lastSellFilled = ctx.getLastSellFilledQty().get();
//        int delta = filledQty - lastSellFilled;
//
//        int delta = filledQty - ctx.getLastSellFilledQty();
//        ctx.getNetPositionQty().addAndGet(-delta);
//        ctx.setLastSellFilledQty(filledQty);
//
//        if (delta <= 0) return;
//
////        if (delta > 0 && ctx.isBuyOpen()) {
////            cancelRemainingBuy(ctx);
////        }
//
//        Mono<Void> cancelMono = Mono.empty();
//
//        if (delta > 0 && ctx.isBuyOpen()) {
//            cancelMono = cancelRemainingBuy(ctx);
//        }
//
//        return cancelMono.then(
//                // rest of your logic
//        );
//
////        int remainingQty = Math.max(0, ctx.getLastBuyFilledQty() - filledQty);
//        int remainingQty = ctx.getNetPositionQty().get();
//
////        if (remainingQty > 0 && ctx.getSellOrderId() != null) {
//        if (remainingQty > 0 && ctx.isSlPlaced() && ctx.isSLOpen() && !ctx.isTradeCompleted()) {
//
//        log.info(
//                "SELL partial fill | stock={} | delta={} | totalFilled={}",
//                ctx.getTradingSymbol(), delta, filledQty
//        );
//
//        // ===== MODIFY STOP LOSS =====
////        int remainingQty = ctx.getQuantity() - filledQty;
////        int remainingQty = ctx.getLastBuyFilledQty() - filledQty;
////
////        if (remainingQty > 0 && ctx.getStopLossOrderId() != null) {
//
//            String jwt = tokenManager.getValidJwtToken();
//
////            executionService.modifyStopLossOrder(
////                    ctx.getTradingSymbol(),
////                    ctx.getSymbolToken(),
////                    remainingQty,
////                    ctx.getStoplossTriggerPrice().doubleValue(),
////                    ctx.getStoplossLimitPrice().doubleValue(),
////                    ctx.getStopLossOrderId(),
////                    jwt
////            )
////            .doOnError(e -> log.error("Failed to modify Stoploss order", e.getMessage()))
////            .subscribe();
//
//            orderActionExecutor.safeModifyOrReplace(
//                    executionService.modifyStopLossOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            remainingQty,
//                            ctx.getStoplossTriggerPrice().doubleValue(),
//                            ctx.getStoplossLimitPrice().doubleValue(),
//                            ctx.getStopLossOrderId(),
//                            tokenManager.getValidJwtToken()
//                    ),
//                    () -> aggressiveExitManager.placeAggressiveExit(
//                            ctx,
//                            remainingQty,
//                            ExitType.MODIFY_FAILED
//                    )
//            ).subscribe();
//
//            BigDecimal executedPrice =
//                    new BigDecimal(response.getOrderStatusData().getAverageprice());
//
//            String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());
//
//            // ===== BALANCE UPDATE (DELTA ONLY) =====
//            balanceService.onSell(
//                    executedPrice,
//                    ctx.getBuyPrice(),
//                    delta,
//                    properties.getLeverageMultiplierToUseForLong(),
//                    balanceService.getUsableBalance(),
//                    leverageService.get(normalizedSymbol).multiplier()
//            );
//
//            ctx.setLastSellFilledQty(filledQty);
//        }
//    }

//    private void cancelRemainingBuy(OrderContext ctx) {
//        String jwt = tokenManager.getValidJwtToken();
//
//        executionService.placeCancelOrder(
//                        ctx.getBuyOrderId(),
//                        ctx.getBuyVariety(),
//                        jwt,
//                        "BUY"
//                )
//                .doOnSuccess(resp -> {
//                    ctx.setBuyOpen(false);
//                    log.info("Remaining BUY cancelled as exit started | buyOrderId={}",
//                            ctx.getBuyOrderId());
//                })
//                .subscribe();
//    }

//    @Override
//    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
////        int lastSellFilled = ctx.getLastSellFilledQty().get();
////
////        int delta = filledQty - lastSellFilled;
//
//        OrderContext.SellUpdate update = ctx.reduceSell(filledQty);
//
//        int delta = update.delta;
//        int remainingQty = update.remainingQty;
//
//        if (delta <= 0) return Mono.empty();
//
//        Mono<Void> cancelMono = Mono.empty();
//
//        if (delta > 0 && ctx.isBuyOpen()) {
//            cancelMono = cancelRemainingBuy(ctx);
//        }
//
//        return cancelMono.then(processSellFill(ctx, response, filledQty, delta));
//    }
//
//    private Mono<Void> processSellFill(
//            OrderContext ctx,
//            OrderStatusResponse response,
//            int filledQty,
//            int delta
//    ) {
//
////        ctx.getNetPositionQty().addAndGet(-delta);
////
////        int remainingQty = ctx.getNetPositionQty().get(); // 🔥 IMPORTANT CHANGE
////
////        if (!(remainingQty > 0 && ctx.isSlPlaced() && ctx.isSLOpen() && !ctx.isTradeCompleted())) {
////            return Mono.empty();
////        }
////
////        log.info("SELL partial fill | stock={} | delta={} | totalFilled={}",
////                ctx.getTradingSymbol(), delta, filledQty);
////
////        String jwt = tokenManager.getValidJwtToken();
////
////        Mono<Void> modifySLMono = orderActionExecutor.safeModifyOrReplace(
////                executionService.modifyStopLossOrder(
////                        ctx.getTradingSymbol(),
////                        ctx.getSymbolToken(),
////                        remainingQty,
////                        ctx.getStoplossTriggerPrice().doubleValue(),
////                        ctx.getStoplossLimitPrice().doubleValue(),
////                        ctx.getStopLossOrderId(),
////                        jwt
////                ),
////                () -> aggressiveExitManager.placeAggressiveExit(
////                        ctx,
////                        remainingQty,
////                        ExitType.MODIFY_FAILED
////                )
////        );
////
////        BigDecimal executedPrice =
////                new BigDecimal(response.getOrderStatusData().getAverageprice());
////
////        String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());
////
////        Mono<Void> balanceUpdateMono = Mono.fromRunnable(() ->
////                balanceService.onSell(
////                        executedPrice,
////                        ctx.getBuyPrice(),
////                        delta,
////                        properties.getLeverageMultiplierToUseForLong(),
////                        balanceService.getUsableBalance(),
////                        leverageService.get(normalizedSymbol).multiplier()
////                )
////        );
////
////        return modifySLMono
////                .then(balanceUpdateMono)
////                .doOnSuccess(v -> {
////                    ctx.getLastSellFilledQty().set(filledQty);
////                    ctx.getNetPositionQty().addAndGet(-delta); // 🔥 KEY FIX
////                });
//
//        return Mono.defer(() -> {
//
//            // ✅ STEP 1: update state FIRST
////            ctx.getNetPositionQty().addAndGet(-delta);
////            ctx.getLastSellFilledQty().set(filledQty);
//
//            int remainingQty = ctx.getNetPositionQty().get();
//
//            if (!(remainingQty > 0 && ctx.isSlPlaced() && ctx.isSLOpen() && !ctx.isTradeCompleted())) {
//                return Mono.empty();
//            }
//
//            log.info("SELL partial fill | stock={} | delta={} | remainingQty={}",
//                    ctx.getTradingSymbol(), delta, remainingQty);
//
//            String jwt = tokenManager.getValidJwtToken();
//
//            Mono<Void> modifySLMono = orderActionExecutor.safeModifyOrReplace(
//                    executionService.modifyStopLossOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            remainingQty,
//                            ctx.getStoplossTriggerPrice().doubleValue(),
//                            ctx.getStoplossLimitPrice().doubleValue(),
//                            ctx.getStopLossOrderId(),
//                            jwt
//                    ),
//                    () -> aggressiveExitManager.placeAggressiveExit(
//                            ctx,
//                            remainingQty,
//                            ExitType.MODIFY_FAILED
//                    )
//            );
//
//            Mono<Void> balanceUpdateMono = Mono.fromRunnable(() ->
//                    balanceService.onSell(
//                            new BigDecimal(response.getOrderStatusData().getAverageprice()),
//                            ctx.getBuyPrice(),
//                            delta,
//                            properties.getLeverageMultiplierToUseForLong(),
//                            balanceService.getUsableBalance(),
//                            leverageService.get(Utility.normalize(ctx.getTradingSymbol())).multiplier()
//                    )
//            );
//
//            return modifySLMono.then(balanceUpdateMono);
//        });
//    }
//
//    private Mono<Void> cancelRemainingBuy(OrderContext ctx) {
//
//        String jwt = tokenManager.getValidJwtToken();
//
//        return executionService.placeCancelOrder(
//                        ctx.getBuyOrderId(),
//                        ctx.getBuyVariety(),
//                        jwt,
//                        "BUY"
//                )
//                .doOnSuccess(resp -> {
//                    ctx.setBuyOpen(false);
//                    log.info("Remaining BUY cancelled as exit started | buyOrderId={}",
//                            ctx.getBuyOrderId());
//                })
//                .doOnError(e ->
//                        log.error("Failed to cancel BUY order {}", ctx.getBuyOrderId(), e)
//                )
//                .then();
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

            AtomicBoolean failed = new AtomicBoolean(false);

            // ===== CANCEL BUY =====
            Mono<Void> cancelMono = Mono.empty();

            if (ctx.isBuyOpen()) {

                cancelMono = Mono.defer(() -> {
                            String jwt = tokenManager.getValidJwtToken();

                            return executionService.placeCancelOrder(
                                    ctx.getBuyOrderId(),
                                    ctx.getBuyVariety(),
                                    jwt,
                                    "BUY"
                            );
                        })
                        .timeout(Duration.ofSeconds(5))
                        .retryWhen(Retry.backoff(3, Duration.ofMillis(200)))
                        .doOnSuccess(resp -> {
                            ctx.setBuyOpen(false);
                            log.info("Remaining BUY cancelled | orderId={}", ctx.getBuyOrderId());
                        })
                        .onErrorResume(e -> {
                            failed.set(true);
                            ctx.markInconsistent();
                            log.error("Failed to cancel BUY", e);
                            return Mono.empty();
                        })
                        .then();
            }

            return cancelMono.then(processSellFill(ctx, response, delta, failed, update));
        });
    }

    private Mono<Void> processSellFill(
            OrderContext ctx,
            OrderStatusResponse response,
            int delta,
            AtomicBoolean failed,
            OrderContext.SellUpdate update
    ) {

        return Mono.defer(() -> {

            // ✅ UPDATE POSITION FIRST (CRITICAL)
//            ctx.getNetPositionQty().addAndGet(-delta);
//
//            int remainingQty = ctx.getNetPositionQty().get();
            int remainingQty = update.remainingQty;

            if (!(remainingQty > 0 && ctx.isSlPlaced() && ctx.isSLOpen() && !ctx.isTradeCompleted())) {
                return Mono.empty();
            }

            log.info("SELL partial fill | stock={} | delta={} | remainingQty={}",
                    ctx.getTradingSymbol(), delta, remainingQty);

            // ===== MODIFY SL =====
            Mono<Void> modifySLMono = Mono.defer(() -> {
                        String jwt = tokenManager.getValidJwtToken();

                        return orderActionExecutor.safeModifyOrReplace(
                                executionService.modifyStopLossOrder(
                                        ctx.getTradingSymbol(),
                                        ctx.getSymbolToken(),
                                        remainingQty,
                                        ctx.getStoplossTriggerPrice().doubleValue(),
                                        ctx.getStoplossLimitPrice().doubleValue(),
                                        ctx.getStopLossOrderId(),
                                        jwt,
                                        "SELL"
                                ),
                                () -> aggressiveExitManager.placeAggressiveExit(
                                        ctx,
                                        remainingQty,
                                        ExitType.MODIFY_FAILED
                                )
                        );
                    })
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(200)))
                    .onErrorResume(e -> {
                        failed.set(true);
                        ctx.markInconsistent();
                        log.error("SL modify failed", e);
                        return Mono.empty();
                    });

            // ===== BALANCE =====
            BigDecimal executedPrice =
                    new BigDecimal(response.getOrderStatusData().getAverageprice());

            String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());

            Mono<Void> balanceMono = Mono.fromRunnable(() ->
                    balanceService.onSell(
                            executedPrice,
                            ctx.getBuyPrice(),
                            delta,
                            properties.getLeverageMultiplierToUseForLong(),
                            balanceService.getUsableBalance(),
                            leverageService.get(normalizedSymbol).multiplier()
                    )
            );

            // ===== FINAL FLOW =====
            return modifySLMono
                    .then(Mono.defer(() -> {

                        if (failed.get()) {
                            log.warn("Skipping balance due to SL failure");
                            return Mono.empty();
                        }

                        return balanceMono
                                .onErrorResume(e -> {
                                    log.error("Balance update failed", e);
                                    return Mono.empty();
                                });
                    }));
        });
    }


}