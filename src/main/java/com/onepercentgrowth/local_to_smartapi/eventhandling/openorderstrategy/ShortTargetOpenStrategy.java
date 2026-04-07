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
public class ShortTargetOpenStrategy implements IOpenOrderStrategy {

    private static final Logger log =
            LoggerFactory.getLogger(ShortTargetOpenStrategy.class);

    private final BalanceService balanceService;
    private final TokenManager tokenManager;
    private final OrderExecutionService executionService;
    private final ApplicationProperties properties;
    private final LeverageService leverageService;
    private final OrderActionExecutor orderActionExecutor;
    private final AggressiveExitManager aggressiveExitManager;

    public ShortTargetOpenStrategy(
            BalanceService balanceService,
            TokenManager tokenManager,
            OrderExecutionService executionService,
            ApplicationProperties properties,
            LeverageService leverageService,
            OrderActionExecutor orderActionExecutor,
            AggressiveExitManager aggressiveExitManager) {

        this.balanceService = balanceService;
        this.tokenManager = tokenManager;
        this.executionService = executionService;
        this.properties = properties;
        this.leverageService = leverageService;
        this.orderActionExecutor = orderActionExecutor;
        this.aggressiveExitManager = aggressiveExitManager;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return ctx.isShort()
                && "BUY".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getBuyOrderId());
    }

//    @Override
//    public void onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
//        int lastBuyFilled = ctx.getLastBuyFilledQty();
//        int delta = filledQty - ctx.getLastBuyFilledQty();
//
//        if (delta <= 0) return;
//
//        if (delta > 0 && ctx.isSellOpen()) {
//            cancelRemainingEntrySell(ctx);
//        }
//
//        int remainingQty = Math.max(0, ctx.getLastBuyFilledQty() - filledQty);
//
//        if (remainingQty > 0 && ctx.isSlPlaced() && ctx.isSLOpen() && !ctx.isTradeCompleted()) {
//
//            log.info(
//                    "BUY partial fill | stock={} | delta={} | totalFilled={}",
//                    ctx.getTradingSymbol(), delta, filledQty
//            );
//
//            String jwt = tokenManager.getValidJwtToken();
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
//            // ===== BOOK PROFIT =====
//            balanceService.onBuy(
//                    executedPrice,
//                    delta,
//                    properties.getLeverageMultiplierToUseForShort(),
//                    balanceService.getUsableBalance(),
//                    leverageService.get(
//                            Utility.normalize(ctx.getTradingSymbol())
//                    ).multiplier()
//            );
//
////        cancelRemainingEntrySell(ctx);
//
//            ctx.setLastBuyFilledQty(filledQty);
//
//            log.info("SHORT TARGET HIT | orderId={}", ctx.getBuyOrderId());
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
//                ctx.getSellOrderId(),
//                ctx.getSellVariety(),
//                jwt,
//                "SELL"
//        )
//        .doOnSuccess(resp -> {
//            ctx.setSellOpen(false);
//            log.info("Remaining SELL cancelled as exit started | sellOrderId={}",
//                    ctx.getSellOrderId());
//        }).subscribe();
//
//        ctx.setSellOpen(false);
//    }

//    @Override
//    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
//
//        OrderContext.BuyUpdate update = ctx.reduceBuy(filledQty);
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
//        return cancelMono.then(processShortTarget(ctx, response, delta, remainingQty));
//    }
//
//    private Mono<Void> processShortTarget(
//            OrderContext ctx,
//            OrderStatusResponse response,
//            int delta,
//            int remainingQty
//    ) {
//
//        if (!(remainingQty > 0 && ctx.isSlPlaced() && ctx.isSLOpen() && !ctx.isTradeCompleted())) {
//            return Mono.empty();
//        }
//
//        log.info("SHORT TARGET partial hit | stock={} | delta={} | remainingQty={}",
//                ctx.getTradingSymbol(), delta, remainingQty);
//
//        String jwt = tokenManager.getValidJwtToken();
//
//        // ===== MODIFY SL =====
//        Mono<Void> modifySLMono = orderActionExecutor.safeModifyOrReplace(
//                executionService.modifyStopLossOrder(
//                        ctx.getTradingSymbol(),
//                        ctx.getSymbolToken(),
//                        remainingQty, // 🔥 FIXED
//                        ctx.getStoplossTriggerPrice().doubleValue(),
//                        ctx.getStoplossLimitPrice().doubleValue(),
//                        ctx.getStopLossOrderId(),
//                        jwt
//                ),
//                () -> aggressiveExitManager.placeAggressiveExit(
//                        ctx,
//                        remainingQty,
//                        ExitType.MODIFY_FAILED
//                )
//        );
//
//        // ===== BALANCE UPDATE =====
//        BigDecimal executedPrice =
//                new BigDecimal(response.getOrderStatusData().getAverageprice());
//
//        String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());
//
//        Mono<Void> balanceMono = Mono.fromRunnable(() ->
//                balanceService.onBuy(
//                        executedPrice,
//                        delta,
//                        properties.getLeverageMultiplierToUseForShort(),
//                        balanceService.getUsableBalance(),
//                        leverageService.get(normalizedSymbol).multiplier()
//                )
//        );
//
//        return modifySLMono
//                .then(balanceMono)
//                .doOnSuccess(v ->
//                        log.info("SHORT TARGET processed | orderId={}", ctx.getBuyOrderId())
//                )
//                .onErrorResume(e -> {
//                    log.error("Short target flow failed | orderId={}", ctx.getBuyOrderId(), e);
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
//            if (!ctx.tryStartBuyFill()) {
//                return Mono.empty();
//            }

            int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());

            OrderContext.BuyUpdate update = ctx.reduceBuy(filledQty);

            int delta = update.delta;
            int remainingQty = update.remainingQty;

            if (delta <= 0) return Mono.empty();

            AtomicBoolean failed = new AtomicBoolean(false);

            // ===== CANCEL ENTRY SELL =====
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

            return cancelMono.then(processShortTarget(ctx, response, delta, remainingQty, failed));
        });
    }

    private Mono<Void> processShortTarget(
            OrderContext ctx,
            OrderStatusResponse response,
            int delta,
            int remainingQty,
            AtomicBoolean failed
    ) {

        if (!(remainingQty > 0 && ctx.isSlPlaced() && ctx.isSLOpen() && !ctx.isTradeCompleted())) {
            return Mono.empty();
        }

        log.info("SHORT TARGET partial hit | stock={} | delta={} | remainingQty={}",
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
                                    jwt
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
                balanceService.onBuy(
                        executedPrice,
                        delta,
                        properties.getLeverageMultiplierToUseForShort(),
                        balanceService.getUsableBalance(),
                        leverageService.get(normalizedSymbol).multiplier()
                )
        );

        // ===== FINAL FLOW =====
        return modifySLMono
                .then(Mono.defer(() -> {

                    if (failed.get()) {
                        log.warn("Skipping balance due to failure");
                        return Mono.empty();
                    }

                    return balanceMono
                            .onErrorResume(e -> {
                                log.error("Balance update failed", e);
                                return Mono.empty();
                            });
                }))
                .doOnSuccess(v ->
                        log.info("SHORT TARGET processed | orderId={}", ctx.getBuyOrderId())
                );
    }
}
