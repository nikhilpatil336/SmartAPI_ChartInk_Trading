package com.onepercentgrowth.local_to_smartapi.eventhandling.fillorderstrategy.longstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.eventhandling.fillorderstrategy.IFillOrderStrategy;
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

//@Component
//public class BuyFilledOrderStrategy implements IFillOrderStrategy {
//
//    private static final Logger log = LoggerFactory.getLogger(BuyFilledOrderStrategy.class);
//
//    private final OrderRegistry orderRegistry;
//    private final OrderExecutionService executionService;
//    private final OrderCalculationService calculationService;
//    private final TokenManager tokenManager;
//    private final BalanceService balanceService;
//    private final LeverageService leverageService;
//    private final ApplicationProperties applicationProperties;
//
//    public BuyFilledOrderStrategy(
//            OrderExecutionService executionService,
//            OrderCalculationService calculationService,
//            TokenManager tokenManager,
//            OrderRegistry orderRegistry,
//            BalanceService balanceService,
//            LeverageService leverageService,
//            ApplicationProperties applicationProperties
//    ) {
//        this.executionService = executionService;
//        this.calculationService = calculationService;
//        this.tokenManager = tokenManager;
//        this.orderRegistry = orderRegistry;
//        this.balanceService = balanceService;
//        this.leverageService = leverageService;
//        this.applicationProperties = applicationProperties;
//    }
//
//    @Override
//    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
//        return ctx.isLong()
//                && response.getOrderStatusData().getTransactiontype().equals("BUY")
//                && ctx.getBuyOrderId().equals(
//                response.getOrderStatusData().getOrderid()
//        );
//    }
//
//
//    @Override
//    public void onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        if (!ctx.isBuyOpen()) {
//            return;
//        }
//
////        double executedPrice =
////                Double.parseDouble(response.getOrderStatusData().getPrice());
//        BigDecimal intenedePrice =
//                new BigDecimal(response.getOrderStatusData().getPrice());
//
//        BigDecimal executedPrice =
//                new BigDecimal(response.getOrderStatusData().getAverageprice());
//
//        int filledQty =
//                Integer.parseInt(response.getOrderStatusData().getFilledshares());
//
//        ctx.setBuyPrice(executedPrice);
//        int lastBuyFilledQty = ctx.getLastBuyFilledQty();
//        ctx.setLastBuyFilledQty(filledQty);
//        ctx.setBuyOpen(false);
//
//        log.info(
//                "BUY filled | stock={} | orderId={} | qty={} | executedPrice={}",
//                ctx.getTradingSymbol(),
//                ctx.getBuyOrderId(),
//                ctx.getQuantity(),
//                executedPrice
//        );
//
//        String jwtToken = tokenManager.getValidJwtToken();
//
////        double sellPrice =
////                calculationService.calculateProfitPrice(executedPrice);
//
//        BigDecimal sellPrice =
//                calculationService.calculateBuyProfitPrice(executedPrice);
//
//
////        double slPrice =
////                calculationService.calculateStopLossPrice(executedPrice);
//
//        StopLossPrice slPrice =
//                calculationService.calculateStopLossPrice(
//                        ctx.getBuyPrice(),
//                        BigDecimal.valueOf(applicationProperties.getTradingStoplossPercent()),
//                        BigDecimal.valueOf(applicationProperties.getTradingStoplossBufferPercent())
//                );
//
//        log.info(
//                "TP/SL calculated | stock={} | TP={} | SL={}",
//                ctx.getTradingSymbol(),
//                sellPrice,
//                slPrice
//        );
//
////        Mono<OrderResponse> sellMono =
////                executionService.placeSellOrder(
////                                ctx.getTradingSymbol(),
////                                ctx.getSymbolToken(),
////                                ctx.getQuantity(),
////                                sellPrice.doubleValue(),
////                                jwtToken
////                        )
////                        .retry(3)
////                        .doOnSuccess(resp -> {
////
////                            ctx.setSellOrderId(
////                                    resp.getData().getOrderid()
////                            );
////                            ctx.setSellVariety("NORMAL");
////
////                            orderRegistry.registerSell(ctx);
////
////                            log.info("SELL order registered: {}", ctx.getSellOrderId());
////                        });
////
////        Mono<OrderResponse> slMono =
////                executionService.placeStopLossOrder(
////                                ctx.getTradingSymbol(),
////                                ctx.getSymbolToken(),
////                                ctx.getQuantity(),
////                                slPrice.triggerPrice().doubleValue(),
////                                slPrice.limitPrice().doubleValue(),
////                                jwtToken
////                        )
////                        .retry(3)
////                        .doOnSuccess(resp -> {
////
////                            ctx.setStopLossOrderId(
////                                    resp.getData().getOrderid()
////                            );
////                            ctx.setStopLossVariety("STOPLOSS");
////
////                            orderRegistry.registerStopLoss(ctx);
////
////                            log.info("SL order registered: {}", ctx.getStopLossOrderId());
////                        });
//
//        /* =======================
//           SELL ORDER
//        ======================= */
//
//        if (!ctx.isSellPlaced()) {
//
//            executionService.placeSellOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            filledQty,
//                            sellPrice.doubleValue(),
//                            jwtToken
//                    )
//                    .retry(3)
//                    .doOnSuccess(resp -> {
//                        ctx.setSellOrderId(resp.getData().getOrderid());
//                        ctx.setSellVariety("NORMAL");
//                        ctx.setSellPlaced(true);
//                        ctx.setSellOpen(true);
//                        ctx.setSellPrice(sellPrice);
//                        orderRegistry.registerSell(ctx);
//
//                        log.info("SELL placed on BUY complete | sellOrderId={}",
//                                ctx.getSellOrderId());
//                    })
//                    .subscribe();
//
//        } else if (ctx.isSellOpen()) {
//
//            executionService.modifySellOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            filledQty,
//                            sellPrice.doubleValue(),
//                            ctx.getSellOrderId(),
//                            jwtToken
//                    )
//                    .retry(3)
//                    .doOnSuccess(resp ->
//                            log.info("SELL modified on BUY complete | sellOrderId={}",
//                                    ctx.getSellOrderId())
//                    )
//                    .subscribe();
//        }
//
//        /* =======================
//           STOP LOSS ORDER
//        ======================= */
//
//        if (!ctx.isSlPlaced()) {
//
//            executionService.placeStopLossOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            filledQty,
//                            slPrice.triggerPrice().doubleValue(),
//                            slPrice.limitPrice().doubleValue(),
//                            jwtToken
//                    )
//                    .retry(3)
//                    .doOnSuccess(resp -> {
//                        ctx.setStopLossOrderId(resp.getData().getOrderid());
//                        ctx.setStopLossVariety("STOPLOSS");
//                        ctx.setSlPlaced(true);
//                        ctx.setSLOpen(true);
//                        ctx.setStoplossTriggerPrice(slPrice.triggerPrice());
//                        ctx.setStoplossLimitPrice(slPrice.limitPrice());
//                        orderRegistry.registerStopLoss(ctx);
//
//                        log.info("SL placed on BUY complete | slOrderId={}",
//                                ctx.getStopLossOrderId());
//                    })
//                    .subscribe();
//
//        } else if (ctx.isSLOpen()) {
//
//            executionService.modifyStopLossOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            filledQty,
//                            slPrice.triggerPrice().doubleValue(),
//                            slPrice.limitPrice().doubleValue(),
//                            ctx.getStopLossOrderId(),
//                            jwtToken
//                    )
//                    .retry(3)
//                    .doOnSuccess(resp ->
//                            log.info("SL modified on BUY complete | slOrderId={}",
//                                    ctx.getStopLossOrderId())
//                    )
//                    .subscribe();
//        }
//
//        // Fire both independently
////        sellMono.subscribe();
////        slMono.subscribe();
//
////        Mono.when(sellMono, slMono).subscribe();
//
////        int quantity = ctx.getQuantity();
////        int quantity = Integer.parseInt(response.getOrderStatusData().getFilledshares());
//
//        String normalizedSymbol =
//                Utility.normalize(ctx.getTradingSymbol());
//
//        // 🔑 BALANCE UPDATE
//        balanceService.onBuy(
//                executedPrice,
//                filledQty - lastBuyFilledQty,
//                applicationProperties.getLeverageMultiplierToUseForLong(),
//                balanceService.getUsableBalance(),
//                leverageService.get(normalizedSymbol).multiplier()
//        );
//
//        log.info(
//                "Balance updated after BUY | stock={} | price={} | qty={} | leveragedUsed={} | maxleverage={}",
//                ctx.getTradingSymbol(),
//                executedPrice,
//                filledQty,
//                applicationProperties.getLeverageMultiplierToUseForLong(),
//                leverageService.get(normalizedSymbol).multiplier()
//        );
//
////        log.info("Balance updated after BUY: price={}, qty={}",
////                executedPrice, quantity);
//    }


@Component
public class BuyFilledOrderStrategy implements IFillOrderStrategy {

    private static final Logger log = LoggerFactory.getLogger(BuyFilledOrderStrategy.class);

    private final OrderRegistry orderRegistry;
    private final OrderExecutionService executionService;
    private final OrderCalculationService calculationService;
    private final TokenManager tokenManager;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties applicationProperties;

    public BuyFilledOrderStrategy(
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
        return ctx.isLong()
                && "BUY".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && ctx.getBuyOrderId().equals(response.getOrderStatusData().getOrderid())
                && ctx.getSellOrderId() == null && ctx.getStopLossOrderId() == null;
    }

    @Override
    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {

        return Mono.defer(() -> {

            if (ctx.getTradeCompleted().get()) {
                log.warn("Duplicate BUY completion ignored | orderId={}",
                        response.getOrderStatusData().getOrderid());
                return Mono.empty();
            }

            if (!ctx.isBuyPartiallyFilled() && ctx.isBuyOpen()) {
                return Mono.empty();
            }

            int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());

//            OrderContext.BuyUpdate update = ctx.reduceBuy(filledQty);
//            int delta = update.delta;
            int delta = filledQty - ctx.getLastBuyFilledQty().get();

            if (delta <= 0) return Mono.empty();

            ctx.getLastBuyFilledQty().set(filledQty);

            BigDecimal executedPrice =
                    new BigDecimal(response.getOrderStatusData().getAverageprice());

            ctx.setBuyPrice(executedPrice);
//            ctx.setBuyOpen(false);

            log.info("BUY COMPLETE | stock={} | qty={} | price={}",
                    ctx.getTradingSymbol(), filledQty, executedPrice);

            String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());

            // ✅ BALANCE FIRST
//            Mono<Void> balanceMono = Mono.fromRunnable(() ->
//                    balanceService.onBuy(
//                            executedPrice,
//                            delta,
//                            applicationProperties.getLeverageMultiplierToUseForLong(),
//                            balanceService.getUsableBalance(),
//                            leverageService.get(normalizedSymbol).multiplier()
//                    )
//            );

            // ===== CALCULATIONS =====
            BigDecimal sellPrice =
                    calculationService.calculateBuyProfitPrice(executedPrice);

            StopLossPrice slPrice =
                    calculationService.calculateStopLossPrice(
                            executedPrice,
                            BigDecimal.valueOf(applicationProperties.getTradingStoplossPercent()),
                            BigDecimal.valueOf(applicationProperties.getTradingStoplossBufferPercent())
                    );

            String jwt = tokenManager.getValidJwtToken();

            int remainingQty = ctx.longBuyRemainingQty(delta);

            // ===== SELL FLOW =====
            Mono<Void> sellMono;

            if (!ctx.isSellPlaced()) {

                sellMono = executionService.placeSellOrder(
                                ctx.getTradingSymbol(),
                                ctx.getSymbolToken(),
//                                ctx.getNetPositionQty().get(),
//                                update.remainingQty,
                                remainingQty,
                                sellPrice.doubleValue(),
                                jwt
                        )
                        .retryWhen(
                                Retry.backoff(3, Duration.ofMillis(200))
                                        .doBeforeRetry(rs ->
                                                log.warn("Retrying LONG SELL... attempt={}", rs.totalRetries())
                                        )
                        )
                        .doOnSuccess(resp -> {
                            ctx.setSellOrderId(resp.getData().getOrderid());
                            ctx.setSellPlaced(true);
                            ctx.setSellOpen(true);
                            ctx.setSellVariety("NORMAL");
                            ctx.setSellPrice(sellPrice);
                            orderRegistry.registerSell(ctx);

                            log.info("LONG SELL placed | orderId={}", ctx.getSellOrderId());
                        })
                        .then();

            } else {

                sellMono = executionService.modifySellOrder(
                                ctx.getTradingSymbol(),
                                ctx.getSymbolToken(),
//                                ctx.getNetPositionQty().get(),
//                                update.remainingQty,
                                remainingQty,
                                sellPrice.doubleValue(),
                                ctx.getSellOrderId(),
                                jwt
                        )
                        .retryWhen(
                                Retry.backoff(3, Duration.ofMillis(200))
                                        .doBeforeRetry(rs ->
                                                log.warn("Retrying MODIFY LONG SELL... attempt={}", rs.totalRetries())
                                        )
                        )
                        .doOnSuccess(resp ->
                                log.info("LONG SELL modified | orderId={}", ctx.getSellOrderId())
                        )
                        .then();
            }

            // ===== SL FLOW =====
            Mono<Void> slMono;

            if (!ctx.isSlPlaced()) {

                slMono = executionService.placeStopLossOrder(
                                ctx.getTradingSymbol(),
                                ctx.getSymbolToken(),
//                                ctx.getNetPositionQty().get(),
//                                update.remainingQty,
                                remainingQty,
                                slPrice.triggerPrice().doubleValue(),
                                slPrice.limitPrice().doubleValue(),
                                jwt,
                                "SELL"
                        )
                        .retryWhen(
                                Retry.backoff(3, Duration.ofMillis(200))
                                        .doBeforeRetry(rs ->
                                                log.warn("Retrying LONG SL... attempt={}", rs.totalRetries())
                                        )
                        )
                        .doOnSuccess(resp -> {
                            ctx.setStopLossOrderId(resp.getData().getOrderid());
                            ctx.setSlPlaced(true);
                            ctx.setSLOpen(true);
                            ctx.setStopLossVariety("STOPLOSS");
                            ctx.setStoplossTriggerPrice(slPrice.triggerPrice());
                            ctx.setStoplossLimitPrice(slPrice.limitPrice());
                            orderRegistry.registerStopLoss(ctx);

                            log.info("LONG SL placed | orderId={}", ctx.getStopLossOrderId());
                        })
                        .then();

            } else {

                slMono = executionService.modifyStopLossOrder(
                                ctx.getTradingSymbol(),
                                ctx.getSymbolToken(),
//                                ctx.getNetPositionQty().get(),
//                                update.remainingQty,
                                remainingQty,
                                slPrice.triggerPrice().doubleValue(),
                                slPrice.limitPrice().doubleValue(),
                                ctx.getStopLossOrderId(),
                                jwt,
                                "SELL"
                        )
                        .retryWhen(
                                Retry.backoff(3, Duration.ofMillis(200))
                                        .doBeforeRetry(rs ->
                                                log.warn("Retrying MODIFY LONG SL... attempt={}", rs.totalRetries())
                                        )
                        )
                        .doOnSuccess(resp ->
                                log.info("LONG SL modified | orderId={}", ctx.getStopLossOrderId())
                        )
                        .then();
            }

            // ✅ FINAL CHAIN
//            return balanceMono
//                    .onErrorResume(e -> {
//                        log.error("Balance update failed", e);
//                        return Mono.empty();
//                    })
//                    .then(
//                            sellMono.onErrorResume(e -> {
//                                log.error("SELL flow failed", e);
//                                return Mono.empty();
//                            })
//                    )
//                    .then(
//                            slMono.onErrorResume(e -> {
//                                log.error("SL flow failed", e);
//                                return Mono.empty();
//                            })
//                    );
//            return sellMono
//                    .onErrorResume(e -> {
//                        ctx.markInconsistent();
//                        log.error("SELL flow failed - marking context inconsistent", e);
//                        return Mono.empty();
//                    })
//                    .then(
//                            slMono.onErrorResume(e -> {
//                                ctx.markInconsistent();
//                                log.error("SL flow failed - marking context inconsistent", e);
//                                return Mono.empty();
//                            })
//                    )
//                    .then(
//                            balanceMono.onErrorResume(e -> {
//                                log.error("Balance update failed", e);
//                                return Mono.empty();
//                            })
//                    );

            // ===== PARALLEL EXECUTION WITH FAILURE TRACKING =====
            AtomicBoolean failed = new AtomicBoolean(false);

            Mono<Void> sellSafe = sellMono.onErrorResume(e -> {
                failed.set(true);
                ctx.markInconsistent();
                log.error("LONG SELL failed - marking inconsistent", e);
                return Mono.empty();
            });

            Mono<Void> slSafe = slMono.onErrorResume(e -> {
                failed.set(true);
                ctx.markInconsistent();
                log.error("LONG SL failed - marking inconsistent", e);
                return Mono.empty();
            });

            Mono<Void> ordersMono = Mono.when(sellSafe, slSafe);

            // ===== BALANCE =====
            Mono<Void> balanceMono = Mono.fromRunnable(() ->
                    balanceService.onBuy(
                            executedPrice,
                            delta,
                            applicationProperties.getLeverageMultiplierToUseForLong(),
                            balanceService.getUsableBalance(),
                            leverageService.get(normalizedSymbol).multiplier()
                    )
            );

            // ===== FINAL FLOW =====
            return ordersMono
                    .then(Mono.defer(() -> {

                        if (failed.get()) {
                            log.warn("Skipping balance due to order failure");
                            return Mono.empty();
                        }

                        // ✅ SAFE STATE UPDATE
                        ctx.setBuyOpen(false);

                        return balanceMono
                                .onErrorResume(e -> {
                                    log.error("Balance update failed", e);
                                    return Mono.empty();
                                });
                    }));
        });
    }

}