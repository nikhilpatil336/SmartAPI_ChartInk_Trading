package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy.longstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy.IOpenOrderStrategy;
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
public class StopLossOpenStrategy implements IOpenOrderStrategy {

    private static final Logger log = LoggerFactory.getLogger(StopLossOpenStrategy.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties properties;

    public StopLossOpenStrategy(OrderExecutionService executionService,
                                TokenManager tokenManager,
                                BalanceService balanceService,
                                LeverageService leverageService,
                                ApplicationProperties properties) {
        this.executionService = executionService;
        this.tokenManager = tokenManager;
        this.balanceService = balanceService;
        this.leverageService = leverageService;
        this.properties = properties;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return ctx.isLong()
                &&"SELL".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getStopLossOrderId());
    }

//    @Override
//    public void onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
//        int lastStoplossFilled = ctx.getLastStoplossFilledQty();
//        int delta = filledQty - lastStoplossFilled;
//
//        if (delta <= 0) return;
//
//        if (delta > 0 && ctx.isBuyOpen()) {
//            cancelRemainingBuy(ctx);
//        }
//
//        int remainingQty = Math.max(0, ctx.getLastBuyFilledQty() - filledQty);
//
////        if (remainingQty > 0 && ctx.getSellOrderId() != null) {
//        if (remainingQty > 0 && ctx.isSellPlaced() && ctx.isSellOpen() && !ctx.isTradeCompleted()) {
//
////            BigDecimal executedPrice =
////                    new BigDecimal(response.getOrderStatusData().getPrice());
//
//            log.warn(
//                    "STOPLOSS partial hit | stock={} | delta={} | totalFilled={}",
//                    ctx.getTradingSymbol(), delta, filledQty
//            );
//
//
////        int remainingQty = ctx.getQuantity() - filledQty;
//
//            String jwt = tokenManager.getValidJwtToken();
//
//            executionService.modifySellOrder(
//                    ctx.getTradingSymbol(),
//                    ctx.getSymbolToken(),
//                    remainingQty,
//                    ctx.getSellPrice().doubleValue(),
//                    ctx.getSellOrderId(),
//                    jwt
//            )
//            .doOnError(e -> log.error("Failed to modify Sell order", e.getMessage()))
//            .subscribe();
//
//            BigDecimal executedPrice =
//                    new BigDecimal(response.getOrderStatusData().getAverageprice());
//
//            String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());
//
//            balanceService.onSell(
//                    executedPrice,
//                    ctx.getBuyPrice(),
//                    delta,
//                    properties.getLeverageMultiplierToUseForLong(),
//                    balanceService.getUsableBalance(),
//                    leverageService.get(normalizedSymbol).multiplier()
//            );
//
//            ctx.setLastStoplossFilledQty(filledQty);
//        }
//    }


//    @Override
//    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
//        int lastStoplossFilled = ctx.getLastStoplossFilledQty().get();
//
//        int delta = filledQty - lastStoplossFilled;
//
//        if (delta <= 0) return Mono.empty();
//
//        Mono<Void> cancelMono = Mono.empty();
//
//        if (delta > 0 && ctx.isBuyOpen()) {
//            cancelMono = cancelRemainingBuy(ctx);
//        }
//
//        return cancelMono.then(processStopLossFill(ctx, response, filledQty, delta));
//    }

//    @Override
//    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
//
//        OrderContext.StopLossUpdate update = ctx.reduceStopLoss(filledQty);
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
//        return cancelMono.then(processStopLossFill(ctx, response, delta, remainingQty));
//    }
//
////    private Mono<Void> processStopLossFill(
////            OrderContext ctx,
////            OrderStatusResponse response,
////            int filledQty,
////            int delta
////    ) {
////
////        int remainingQty = ctx.getNetPositionQty().get(); // 🔥 FIXED
////
////        if (!(remainingQty > 0 && ctx.isSellPlaced() && ctx.isSellOpen() && !ctx.isTradeCompleted())) {
////            return Mono.empty();
////        }
////
////        log.warn("STOPLOSS partial hit | stock={} | delta={} | totalFilled={}",
////                ctx.getTradingSymbol(), delta, filledQty);
////
////        String jwt = tokenManager.getValidJwtToken();
////
////        Mono<Void> modifySellMono = executionService.modifySellOrder(
////                        ctx.getTradingSymbol(),
////                        ctx.getSymbolToken(),
////                        remainingQty, // 🔥 FIXED
////                        ctx.getSellPrice().doubleValue(),
////                        ctx.getSellOrderId(),
////                        jwt
////                )
////                .doOnSuccess(resp ->
////                        log.info("SELL modified after SL hit | orderId={}", ctx.getSellOrderId())
////                )
////                .doOnError(e ->
////                        log.error("Failed to modify SELL after SL hit | orderId={}",
////                                ctx.getSellOrderId(), e)
////                )
////                .onErrorResume(e -> Mono.empty())
////                .then();
////
////        BigDecimal executedPrice =
////                new BigDecimal(response.getOrderStatusData().getAverageprice());
////
////        String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());
////
////        Mono<Void> balanceMono = Mono.fromRunnable(() ->
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
////        return modifySellMono
////                .then(balanceMono)
////                .doOnSuccess(v -> {
////                    ctx.getLastStoplossFilledQty().set(filledQty);
////                    ctx.getNetPositionQty().addAndGet(-delta); // 🔥 CRITICAL FIX
////                });
////    }
//
//    private Mono<Void> processStopLossFill(
//            OrderContext ctx,
//            OrderStatusResponse response,
//            int delta,
//            int remainingQty
//    ) {
//
//        if (!(remainingQty > 0 && ctx.isSellPlaced() && ctx.isSellOpen() && !ctx.isTradeCompleted())) {
//            return Mono.empty();
//        }
//
//        log.warn("STOPLOSS partial hit | stock={} | delta={} | remainingQty={}",
//                ctx.getTradingSymbol(), delta, remainingQty);
//
//        String jwt = tokenManager.getValidJwtToken();
//
//        Mono<Void> modifySellMono = executionService.modifySellOrder(
//                        ctx.getTradingSymbol(),
//                        ctx.getSymbolToken(),
//                        remainingQty, // ✅ CORRECT NOW
//                        ctx.getSellPrice().doubleValue(),
//                        ctx.getSellOrderId(),
//                        jwt
//                )
//                .then();
//
//        Mono<Void> balanceMono = Mono.fromRunnable(() ->
//                balanceService.onSell(
//                        new BigDecimal(response.getOrderStatusData().getAverageprice()),
//                        ctx.getBuyPrice(),
//                        delta,
//                        properties.getLeverageMultiplierToUseForLong(),
//                        balanceService.getUsableBalance(),
//                        leverageService.get(Utility.normalize(ctx.getTradingSymbol())).multiplier()
//                )
//        );
//
//        return modifySellMono.then(balanceMono);
//    }
//
////    private void cancelRemainingBuy(OrderContext ctx) {
////        String jwt = tokenManager.getValidJwtToken();
////
////        executionService.placeCancelOrder(
////                        ctx.getBuyOrderId(),
////                        ctx.getBuyVariety(),
////                        jwt,
////                        "BUY"
////                )
////                .doOnSuccess(resp -> {
////                    ctx.setBuyOpen(false);
////                    log.info("Remaining BUY cancelled as exit started | buyOrderId={}",
////                            ctx.getBuyOrderId());
////                })
////                .subscribe();
////    }
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
//                    log.info("Remaining BUY cancelled | orderId={}", ctx.getBuyOrderId());
//                })
//                .doOnError(e ->
//                        log.error("Failed to cancel BUY | orderId={}", ctx.getBuyOrderId(), e)
//                )
//                .onErrorResume(e -> Mono.empty())
//                .then();
//    }

    @Override
    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {

        return Mono.defer(() -> {

            // ✅ ATOMIC GUARD
//            if (!ctx.tryStartLongSLFill()) {
//                return Mono.empty();
//            }

            int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());

//            OrderContext.StopLossUpdate update = ctx.reduceStopLoss(filledQty);
//
//            int delta = update.delta;
//
//            if (delta <= 0) return Mono.empty();

            int delta = filledQty - ctx.getLastStoplossFilledQty().get();

            if (delta <= 0) return Mono.empty();

            ctx.getLastStoplossFilledQty().set(filledQty);

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

            return cancelMono.then(processStopLossFill(ctx, response, delta, failed, ctx.stopLossRemainingQty(delta)));
        });
    }

    private Mono<Void> processStopLossFill(
            OrderContext ctx,
            OrderStatusResponse response,
            int delta,
            AtomicBoolean failed,
            int remainingQty
    ) {

        return Mono.defer(() -> {

            // ✅ UPDATE POSITION FIRST (CRITICAL)
//            ctx.getNetPositionQty().addAndGet(-delta);
//
//            int remainingQty = ctx.getNetPositionQty().get();
//            int remainingQty = update.remainingQty;

            if (!(remainingQty > 0 && ctx.isSellPlaced() && ctx.isSellOpen() && !ctx.getTradeCompleted().get())) {
                return Mono.empty();
            }

            log.warn("STOPLOSS partial hit | stock={} | delta={} | remainingQty={}",
                    ctx.getTradingSymbol(), delta, remainingQty);

            // ===== MODIFY SELL =====
            Mono<Void> modifySellMono = Mono.defer(() -> {
                        String jwt = tokenManager.getValidJwtToken();

                        return executionService.modifySellOrder(
                                ctx.getTradingSymbol(),
                                ctx.getSymbolToken(),
                                remainingQty,
                                ctx.getSellPrice().doubleValue(),
                                ctx.getSellOrderId(),
                                jwt
                        );
                    })
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(200)))
                    .doOnSuccess(resp ->
                            log.info("SELL modified after SL | orderId={}", ctx.getSellOrderId())
                    )
                    .onErrorResume(e -> {
                        failed.set(true);
                        ctx.markInconsistent();
                        log.error("Failed to modify SELL after SL", e);
                        return Mono.empty();
                    })
                    .then();

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
            return modifySellMono
                    .then(Mono.defer(() -> {

                        if (failed.get()) {
                            log.warn("Skipping balance due to SELL modify failure");
                            return Mono.empty();
                        }

                        return balanceMono
                                .onErrorResume(e -> {
                                    log.error("Balance update failed", e);
                                    return Mono.empty();
                                });
                    }))
                    .doOnSuccess(v ->
                            log.info("STOPLOSS processed | orderId={}", ctx.getStopLossOrderId())
                    );
        });
    }

//    private Mono<Void> cancelRemainingBuy(OrderContext ctx) {
//
//        return Mono.defer(() -> {
//                    String jwt = tokenManager.getValidJwtToken();
//
//                    return executionService.placeCancelOrder(
//                            ctx.getBuyOrderId(),
//                            ctx.getBuyVariety(),
//                            jwt,
//                            "BUY"
//                    );
//                })
//                .timeout(Duration.ofSeconds(5))
//                .retryWhen(Retry.backoff(3, Duration.ofMillis(200)))
//                .doOnSuccess(resp -> {
//                    ctx.setBuyOpen(false);
//                    log.info("Remaining BUY cancelled | orderId={}", ctx.getBuyOrderId());
//                })
//                .onErrorResume(e -> {
//                    ctx.markInconsistent();
//                    log.error("Failed to cancel BUY", e);
//                    return Mono.empty();
//                })
//                .then();
//    }

}
