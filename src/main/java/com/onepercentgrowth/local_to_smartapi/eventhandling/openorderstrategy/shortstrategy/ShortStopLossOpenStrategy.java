package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy;

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
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ShortStopLossOpenStrategy implements IOpenOrderStrategy {

    private static final Logger log =
            LoggerFactory.getLogger(ShortStopLossOpenStrategy.class);

    private final BalanceService balanceService;
    private final TokenManager tokenManager;
    private final OrderExecutionService executionService;
    private final ApplicationProperties properties;
    private final LeverageService leverageService;

    public ShortStopLossOpenStrategy(
            BalanceService balanceService,
            TokenManager tokenManager,
            OrderExecutionService executionService,
            ApplicationProperties properties,
            LeverageService leverageService) {

        this.balanceService = balanceService;
        this.tokenManager = tokenManager;
        this.executionService = executionService;
        this.properties = properties;
        this.leverageService = leverageService;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return ctx.isShort()
                && "BUY".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getStopLossOrderId());
    }

//    @Override
//    public void onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
////        int lastStoplossFilled = ctx.getLastStoplossFilledQty();
//        int delta = filledQty - ctx.getLastStoplossFilledQty();
//
//        if (delta <= 0) return;
//
//        if (delta > 0 && ctx.isSellOpen()) {
//            cancelRemainingEntrySell(ctx);
//        }
//
//        int remainingQty = Math.max(0, ctx.getLastSellFilledQty() - filledQty);
//
//        if (remainingQty > 0 && ctx.isBuyPlaced() && ctx.isBuyOpen() && !ctx.isTradeCompleted()) {
//
//            log.warn(
//                    "STOPLOSS partial hit | stock={} | delta={} | totalFilled={}",
//                    ctx.getTradingSymbol(), delta, filledQty
//            );
//
//            String jwt = tokenManager.getValidJwtToken();
//
//            executionService.modifyBuyOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            remainingQty,
//                            ctx.getBuyPrice().toString(),
//                            ctx.getBuyOrderId(),
//                            jwt
//                    )
//                    .doOnError(e -> log.error("Failed to modify Buy order", e.getMessage()))
//                    .subscribe();
//
//            BigDecimal executedPrice =
//                    new BigDecimal(response.getOrderStatusData().getAverageprice());
//
//            String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());
//
//            // ===== BOOK LOSS =====
//            balanceService.onShortCover(
//                    executedPrice,
//                    ctx.getSellPrice(),
//                    delta,
//                    properties.getLeverageMultiplierToUseForShort(),
//                    balanceService.getUsableBalance(),
//                    leverageService.get(normalizedSymbol).multiplier()
//            );
//
//    //        cancelRemainingEntrySell(ctx);
//
//            ctx.setLastStoplossFilledQty(filledQty);
//
//            log.info("SHORT STOPLOSS HIT | orderId={}", ctx.getStopLossOrderId());
//        }
//    }
//
//    private void cancelRemainingEntrySell(OrderContext ctx) {
//
//        if (!ctx.isSellOpen()) return;
//
//        String jwt = tokenManager.getValidJwtToken();
//
//        executionService.placeCancelOrder(
//                        ctx.getSellOrderId(),
//                        ctx.getSellVariety(),
//                        jwt,
//                        "SELL"
//                )
//                .doOnSuccess(resp -> {
//                    ctx.setSellOpen(false);
//                    log.info("Remaining BUY cancelled as exit started | buyOrderId={}",
//                            ctx.getSellOrderId());
//                }).subscribe();
//
////        ctx.setSellOpen(false);
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
//        if (delta > 0 && ctx.isSellOpen()) {
//            cancelMono = cancelRemainingEntrySell(ctx);
//        }
//
//        return cancelMono.then(processShortStopLoss(ctx, response, delta, remainingQty));
//    }
//
//
//    private Mono<Void> processShortStopLoss(
//            OrderContext ctx,
//            OrderStatusResponse response,
//            int delta,
//            int remainingQty
//    ) {
//
//        if (!(remainingQty > 0 && ctx.isBuyPlaced() && ctx.isBuyOpen() && !ctx.isTradeCompleted())) {
//            return Mono.empty();
//        }
//
//        log.warn("SHORT STOPLOSS partial hit | stock={} | delta={} | remainingQty={}",
//                ctx.getTradingSymbol(), delta, remainingQty);
//
//        String jwt = tokenManager.getValidJwtToken();
//
//        // ===== MODIFY TARGET BUY =====
//        Mono<Void> modifyBuyMono = executionService.modifyBuyOrder(
//                        ctx.getTradingSymbol(),
//                        ctx.getSymbolToken(),
//                        remainingQty, // 🔥 FIXED
//                        ctx.getBuyPrice().toString(),
//                        ctx.getBuyOrderId(),
//                        jwt
//                )
//                .doOnSuccess(resp ->
//                        log.info("TARGET BUY modified after SL | orderId={}", ctx.getBuyOrderId())
//                )
//                .doOnError(e ->
//                        log.error("Failed to modify BUY after SL | orderId={}",
//                                ctx.getBuyOrderId(), e)
//                )
//                .onErrorResume(e -> Mono.empty())
//                .then();
//
//        // ===== BALANCE UPDATE (LOSS) =====
//        BigDecimal executedPrice =
//                new BigDecimal(response.getOrderStatusData().getAverageprice());
//
//        String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());
//
//        Mono<Void> balanceMono = Mono.fromRunnable(() ->
//                balanceService.onShortCover(
//                        executedPrice,
//                        ctx.getSellPrice(),
//                        delta,
//                        properties.getLeverageMultiplierToUseForShort(),
//                        balanceService.getUsableBalance(),
//                        leverageService.get(normalizedSymbol).multiplier()
//                )
//        );
//
//        return modifyBuyMono
//                .then(balanceMono)
//                .doOnSuccess(v ->
//                        log.info("SHORT STOPLOSS processed | orderId={}", ctx.getStopLossOrderId())
//                )
//                .onErrorResume(e -> {
//                    log.error("Short stoploss flow failed | orderId={}", ctx.getStopLossOrderId(), e);
//                    return Mono.empty();
//                });
//    }
//
//    private Mono<Void> cancelRemainingEntrySell(OrderContext ctx) {
//
//        if (!ctx.isSellOpen()) return Mono.empty();
//
//        String jwt = tokenManager.getValidJwtToken();
//
//        return executionService.placeCancelOrder(
//                        ctx.getSellOrderId(),
//                        ctx.getSellVariety(),
//                        jwt,
//                        "SELL"
//                )
//                .doOnSuccess(resp -> {
//                    ctx.setSellOpen(false);
//                    log.info("Remaining SELL cancelled | orderId={}", ctx.getSellOrderId());
//                })
//                .doOnError(e ->
//                        log.error("Failed to cancel SELL | orderId={}", ctx.getSellOrderId(), e)
//                )
//                .onErrorResume(e -> Mono.empty())
//                .then();
//    }


    @Override
    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {

        return Mono.defer(() -> {

            // ✅ ATOMIC GUARD
//            if (!ctx.tryStartShortSLFill()) {
//                return Mono.empty();
//            }

            int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());

            OrderContext.StopLossUpdate update = ctx.reduceStopLoss(filledQty);

            int delta = update.delta;

            if (delta <= 0) return Mono.empty();

            AtomicBoolean failed = new AtomicBoolean(false);

            // ===== CANCEL SELL =====
            Mono<Void> cancelMono = Mono.empty();

            if (ctx.isSellOpen()) {

                cancelMono = Mono.defer(() -> {
                            String jwt = tokenManager.getValidJwtToken();

                            return executionService.placeCancelOrder(
                                    ctx.getSellOrderId(),
                                    ctx.getSellVariety(),
                                    jwt,
                                    "SELL"
                            );
                        })
                        .timeout(Duration.ofSeconds(5))
                        .retryWhen(Retry.backoff(3, Duration.ofMillis(200)))
                        .doOnSuccess(resp -> {
                            ctx.setSellOpen(false);
                            log.info("Remaining SELL cancelled | orderId={}", ctx.getSellOrderId());
                        })
                        .onErrorResume(e -> {
                            failed.set(true);
                            ctx.markInconsistent();
                            log.error("Failed to cancel SELL", e);
                            return Mono.empty();
                        })
                        .then();
            }

            return cancelMono.then(processShortStopLoss(ctx, response, delta, failed, update));
        });
    }

    private Mono<Void> processShortStopLoss(
            OrderContext ctx,
            OrderStatusResponse response,
            int delta,
            AtomicBoolean failed,
            OrderContext.StopLossUpdate update
    ) {

        return Mono.defer(() -> {

            // ✅ UPDATE POSITION FIRST (CRITICAL)
//            ctx.getNetPositionQty().addAndGet(-delta);
//
//            int remainingQty = ctx.getNetPositionQty().get();
            int remainingQty = update.remainingQty;


            if (!(remainingQty > 0 && ctx.isBuyPlaced() && ctx.isBuyOpen() && !ctx.isTradeCompleted())) {
                return Mono.empty();
            }

            log.warn("SHORT STOPLOSS partial hit | stock={} | delta={} | remainingQty={}",
                    ctx.getTradingSymbol(), delta, remainingQty);

            // ===== MODIFY TARGET BUY =====
            Mono<Void> modifyBuyMono = Mono.defer(() -> {
                        String jwt = tokenManager.getValidJwtToken();

                        return executionService.modifyBuyOrder(
                                ctx.getTradingSymbol(),
                                ctx.getSymbolToken(),
                                remainingQty,
                                ctx.getBuyPrice().toString(),
                                ctx.getBuyOrderId(),
                                jwt
                        );
                    })
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(200)))
                    .doOnSuccess(resp ->
                            log.info("TARGET BUY modified after SL | orderId={}", ctx.getBuyOrderId())
                    )
                    .onErrorResume(e -> {
                        failed.set(true);
                        ctx.markInconsistent();
                        log.error("Failed to modify BUY after SL", e);
                        return Mono.empty();
                    })
                    .then();

            // ===== BALANCE =====
            BigDecimal executedPrice =
                    new BigDecimal(response.getOrderStatusData().getAverageprice());

            String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());

            Mono<Void> balanceMono = Mono.fromRunnable(() ->
                    balanceService.onShortCover(
                            executedPrice,
                            ctx.getSellPrice(),
                            delta,
                            properties.getLeverageMultiplierToUseForShort(),
                            balanceService.getUsableBalance(),
                            leverageService.get(normalizedSymbol).multiplier()
                    )
            );

            // ===== FINAL FLOW =====
            return modifyBuyMono
                    .then(Mono.defer(() -> {

                        if (failed.get()) {
                            log.warn("Skipping balance due to BUY modify failure");
                            return Mono.empty();
                        }

                        return balanceMono
                                .onErrorResume(e -> {
                                    log.error("Balance update failed", e);
                                    return Mono.empty();
                                });
                    }))
                    .doOnSuccess(v ->
                            log.info("SHORT STOPLOSS processed | orderId={}", ctx.getStopLossOrderId())
                    );
        });
    }
}


