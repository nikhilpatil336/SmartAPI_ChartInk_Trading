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
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ShortTargetFilledStrategy implements IFillOrderStrategy {

    private static final Logger log =
            LoggerFactory.getLogger(ShortTargetFilledStrategy.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties applicationProperties;

    public ShortTargetFilledStrategy(
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
                && response.getOrderStatusData().getOrderid().equals(ctx.getBuyOrderId());
    }

//    @Override
//    public void onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        if (ctx.isTradeCompleted()) return;
//        ctx.setTradeCompleted(true);
//
//        BigDecimal executedPrice =
//                new BigDecimal(response.getOrderStatusData().getAverageprice());
//
//        int qty =
//                Integer.parseInt(response.getOrderStatusData().getFilledshares());
//
//        log.info("SHORT TARGET HIT | stock={}", ctx.getTradingSymbol());
//
//        cancelStopLoss(ctx);
//
//        String normalizedSymbol =
//                Utility.normalize(ctx.getTradingSymbol());
//
//        balanceService.onBuy(
//                executedPrice,
//                qty,
//                applicationProperties.getLeverageMultiplierToUseForShort(),
//                balanceService.getUsableBalance(),
//                leverageService.get(normalizedSymbol).multiplier()
//        );
//    }

//    @Override
//    public void onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        if (ctx.isTradeCompleted()) {
//            log.warn("Duplicate SHORT TARGET completion ignored | orderId={}",
//                    response.getOrderStatusData().getOrderid());
//            return;
//        }
//
//        ctx.setTradeCompleted(true);
//
////        BigDecimal executedPrice =
////                new BigDecimal(response.getOrderStatusData().getAverageprice());
//        BigDecimal executedPrice =
//                new BigDecimal(response.getOrderStatusData().getPrice());
//
//        int qty =
//                Integer.parseInt(response.getOrderStatusData().getFilledshares());
//
//        log.info(
//                "SHORT TARGET filled | stock={} | sellOrderId={} | buyOrderId={} | qty={}",
//                ctx.getTradingSymbol(),
//                ctx.getSellOrderId(),
//                ctx.getBuyOrderId(),
//                qty
//        );
//
//        /* ========= CANCEL STOPLOSS ========= */
//
//        if (ctx.getStopLossOrderId() != null && ctx.isSLOpen()) {
//
//            String jwt = tokenManager.getValidJwtToken();
//
//            executionService.placeCancelOrder(
//                            ctx.getStopLossOrderId(),
//                            ctx.getStopLossVariety(),
//                            jwt,
//                            "StopLoss"
//                    )
//                    .retry(3)
//                    .doOnSuccess(resp -> {
//                        log.info(
//                                "SHORT SL cancelled | stock={} | slOrderId={} | sellOrderId={}",
//                                ctx.getTradingSymbol(),
//                                ctx.getStopLossOrderId(),
//                                ctx.getSellOrderId()
//                                );
//                        ctx.setSLOpen(false);
//                    })
//                    .subscribe();
//        }
//
//        ctx.setBuyOpen(false);
//
//        /* ========= BALANCE UPDATE ========= */
//
//        String normalizedSymbol =
//                Utility.normalize(ctx.getTradingSymbol());
//
//        balanceService.onBuy(
//                executedPrice,
//                qty,
//                applicationProperties.getLeverageMultiplierToUseForShort(),
//                balanceService.getUsableBalance(),
//                leverageService.get(normalizedSymbol).multiplier()
//        );
//
//        log.info(
//                "Balance updated after SHORT TARGET | stock={} | price={} | qty={} | leveragedUsed={} | leverage={}",
//                ctx.getTradingSymbol(),
//                executedPrice,
//                qty,
//                applicationProperties.getLeverageMultiplierToUseForLong(),
//                leverageService.get(normalizedSymbol).multiplier()
//        );
//
//        // orderRegistry.remove(ctx); // optional cleanup
//    }
//
////    private void cancelStopLoss(OrderContext ctx) {
////        if (ctx.getStopLossOrderId() == null) return;
////
////        String jwt = tokenManager.getValidJwtToken();
////
////        executionService.placeCancelOrder(
////                ctx.getStopLossOrderId(),
////                ctx.getStopLossVariety(),
////                jwt,
////                "StopLoss"
////        ).subscribe();
////    }

    @Override
    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {

        return Mono.defer(() -> {

            // ✅ EXIT GUARD
//            if (!ctx.tryStartExit()) {
//                log.warn("Duplicate SHORT TARGET completion ignored | orderId={}",
//                        response.getOrderStatusData().getOrderid());
//                return Mono.empty();
//            }

            if(ctx.isBuyOpen())
            {
                return Mono.empty();
            }

            int filledQty =
                    Integer.parseInt(response.getOrderStatusData().getFilledshares());

            // 🔴 IMPORTANT: you need this method
//            OrderContext.BuyUpdate update = ctx.reduceBuy(filledQty);
//            int delta = update.delta;

            int delta = filledQty - ctx.getLastBuyFilledQty().get();

            if (delta <= 0) return Mono.empty();

            ctx.getLastBuyFilledQty().set(filledQty);

            log.info("SHORT TARGET COMPLETE | stock={} | delta={}",
                    ctx.getTradingSymbol(), delta);

//            ctx.setBuyOpen(false);
//            ctx.setTradeCompleted(true);

            BigDecimal executedPrice =
                    new BigDecimal(response.getOrderStatusData().getAverageprice());

            String normalizedSymbol =
                    Utility.normalize(ctx.getTradingSymbol());

            String slOrderId = ctx.getStopLossOrderId();
            String slVariety = ctx.getStopLossVariety();

            AtomicBoolean failed = new AtomicBoolean(false);

            // ===== BALANCE FIRST =====
//            Mono<Void> balanceMono = Mono.fromRunnable(() ->
//                    balanceService.onBuy(
//                            executedPrice,
//                            delta,
//                            applicationProperties.getLeverageMultiplierToUseForShort(),
//                            balanceService.getUsableBalance(),
//                            leverageService.get(normalizedSymbol).multiplier()
//                    )
//            );

            // ===== CANCEL SL =====
            Mono<Void> cancelSLMono = Mono.empty();

//            String slOrderId = ctx.getStopLossOrderId();
//            String slVariety = ctx.getStopLossVariety();

            if (slOrderId != null && ctx.isSLOpen()) {

                String jwt = tokenManager.getValidJwtToken();

                cancelSLMono = executionService
                        .placeCancelOrder(slOrderId, slVariety, jwt, "STOPLOSS")
                        .timeout(Duration.ofSeconds(5))
                        .retryWhen(
                                Retry.backoff(3, Duration.ofMillis(200))
                                        .doBeforeRetry(rs ->
                                                log.warn("Retrying SHORT SL cancel... attempt={}", rs.totalRetries())
                                        )
                        )
                        .doOnSuccess(resp -> {
                            ctx.setSLOpen(false);

                            log.info("SHORT SL cancelled | orderId={}", slOrderId);
                        })
//                        .doOnError(e ->
//                                log.error("Failed to cancel SHORT SL | orderId={}", slOrderId, e)
//                        )
//                        .onErrorResume(e -> Mono.empty())
                        .onErrorResume(e -> {
                            failed.set(true);
                            ctx.markInconsistent();
                            log.error("Failed to cancel SHORT SL - marking inconsistent | orderId={}", slOrderId, e);
                            return Mono.empty();
                        })
                        .then();
            }

            // ✅ FINAL FLOW
//            return balanceMono
//                    .onErrorResume(e -> {
//                        log.error("Balance update failed (SHORT)", e);
//                        return Mono.empty();
//                    })
//                    .then(cancelSLMono);

            // ===== BALANCE FIRST =====
            Mono<Void> balanceMono = Mono.fromRunnable(() ->
                    balanceService.onBuy(
                            executedPrice,
                            delta,
                            applicationProperties.getLeverageMultiplierToUseForShort(),
                            balanceService.getUsableBalance(),
                            leverageService.get(normalizedSymbol).multiplier()
                    )
            );

            // ===== FINAL FLOW =====
            return cancelSLMono
                    .then(Mono.defer(() -> {

                        if (failed.get()) {
                            log.warn("Skipping balance due to SHORT SL cancel failure");
                            return Mono.empty();
                        }

                        // ✅ SAFE STATE UPDATE
                        ctx.setBuyOpen(false);
                        ctx.setTradeCompleted(true);

                        return balanceMono
                                .onErrorResume(e -> {
                                    log.error("Balance update failed", e);
                                    return Mono.empty();
                                });
                    }));
        });
    }
}
