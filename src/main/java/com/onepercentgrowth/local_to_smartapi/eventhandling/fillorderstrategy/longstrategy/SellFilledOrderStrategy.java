package com.onepercentgrowth.local_to_smartapi.eventhandling.fillorderstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
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
public class SellFilledOrderStrategy implements IFillOrderStrategy {

    private static final Logger log = LoggerFactory.getLogger(SellFilledOrderStrategy.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties applicationProperties;

    public SellFilledOrderStrategy(
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
        return ctx.isLong()
                && "SELL".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getSellOrderId());
    }

//    @Override
//    public void onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        if (ctx.isTradeCompleted()) {
//            log.warn("Duplicate SELL completion ignored | orderId={}",
//                    response.getOrderStatusData().getOrderid());
//            return;
//        }
//        ctx.setTradeCompleted(true);
//
//        log.info(
//                "SELL filled | stock={} | buyOrderId={} | sellOrderId={} | qty={}",
//                ctx.getTradingSymbol(),
//                ctx.getBuyOrderId(),
//                ctx.getSellOrderId(),
//                ctx.getQuantity()
//        );
//
//        String stopLossOrderId = ctx.getStopLossOrderId();
//        String stopLossVariety = ctx.getStopLossVariety();
//
//        if (stopLossOrderId == null) {
//            log.warn(
//                    "No SL to cancel | stock={} | buyOrderId={}",
//                    ctx.getTradingSymbol(),
//                    ctx.getBuyOrderId()
//            );
//            return;
//        }
//
//        String jwtToken = tokenManager.getValidJwtToken();
//
//        executionService
//                .placeCancelOrder(stopLossOrderId, stopLossVariety, jwtToken, "StopLoss")
//                .retry(3)
//                .doOnSuccess(resp -> {
//                    log.info(
//                            "SL cancelled | stock={} | buyOrderId={} | slOrderId={}",
//                            ctx.getTradingSymbol(),
//                            ctx.getBuyOrderId(),
//                            stopLossOrderId
//                    );
//
//                    ctx.setSLOpen(false);
////                    orderRegistry.remove(ctx); // trade complete
//                })
//                .subscribe();
//
//        ctx.setSellOpen(false);
//
////        double executedPrice =
////                Double.parseDouble(response.getOrderStatusData().getPrice());
//
//        BigDecimal executedPrice =
//                new BigDecimal(response.getOrderStatusData().getPrice());
//
////        int quantity = ctx.getQuantity();
//        int quantity = Integer.parseInt(response.getOrderStatusData().getFilledshares());
//
//        String normalizedSymbol =
//                Utility.normalize(ctx.getTradingSymbol());
//
//        // 🔑 BALANCE UPDATE
//        balanceService.onSell(
//                executedPrice,
//                ctx.getBuyPrice(),
//                quantity,
//                applicationProperties.getLeverageMultiplierToUseForLong(),
//                balanceService.getUsableBalance(),
//                leverageService.get(normalizedSymbol).multiplier()
//        );
//
//        log.info(
//                "Balance updated after SELL | stock={} | price={} | qty={} | leveragedUsed={} | leverage={}",
//                ctx.getTradingSymbol(),
//                executedPrice,
//                quantity,
//                applicationProperties.getLeverageMultiplierToUseForLong(),
//                leverageService.get(normalizedSymbol).multiplier()
//        );
//
////        log.info("Balance updated after SELL: price={}, qty={}",
////                executedPrice, quantity);
//    }

    @Override
    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {

        return Mono.defer(() -> {

            // ✅ EXIT GUARD (VERY IMPORTANT)
//            if (!ctx.tryStartExit()) {
//                log.warn("Duplicate LONG SELL completion ignored | orderId={}",
//                        response.getOrderStatusData().getOrderid());
//                return Mono.empty();
//            }

            if(ctx.isSellOpen())
            {
                return Mono.empty();
            }

            int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());

//            OrderContext.SellUpdate update = ctx.reduceSell(filledQty);
//            int delta = update.delta;
            int delta = filledQty - ctx.getLastSellFilledQty().get();

            if (delta <= 0) return Mono.empty();

            ctx.getLastSellFilledQty().set(filledQty);

            log.info("LONG SELL COMPLETE | stock={} | delta={}",
                    ctx.getTradingSymbol(), delta);

//            ctx.setSellOpen(false);
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
//                    balanceService.onSell(
//                            executedPrice,
//                            ctx.getBuyPrice(),
//                            delta,
//                            applicationProperties.getLeverageMultiplierToUseForLong(),
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
                                                log.warn("Retrying LONG SL cancel... attempt={}", rs.totalRetries())
                                        )
                        )
                        .doOnSuccess(resp -> {
                            ctx.setSLOpen(false);

                            log.info("LONG SL cancelled | orderId={}", slOrderId);
                        })
//                        .doOnError(e ->
//                                log.error("Failed to cancel SL | orderId={}", slOrderId, e)
//                        )
//                        .onErrorResume(e -> Mono.empty())
                        .onErrorResume(e -> {
                            failed.set(true);
                            ctx.markInconsistent();
                            log.error("Failed to cancel LONG SL - marking inconsistent | orderId={}", slOrderId, e);
                            return Mono.empty();
                        })
                        .then();
            }

            // ✅ FINAL CHAIN
//            return balanceMono
//                    .onErrorResume(e -> {
//                        log.error("Balance update failed", e);
//                        return Mono.empty();
//                    })
//                    .then(cancelSLMono);

            // ===== BALANCE =====
            Mono<Void> balanceMono = Mono.fromRunnable(() ->
                    balanceService.onSell(
                            executedPrice,
                            ctx.getBuyPrice(),
                            delta,
                            applicationProperties.getLeverageMultiplierToUseForLong(),
                            balanceService.getUsableBalance(),
                            leverageService.get(normalizedSymbol).multiplier()
                    )
            );

            // ===== FINAL FLOW =====
            return cancelSLMono
                    .then(Mono.defer(() -> {

                        if (failed.get()) {
                            log.warn("Skipping balance due to LONG SL cancel failure");
                            return Mono.empty();
                        }

                        // ✅ SAFE STATE UPDATE
                        ctx.setSellOpen(false);
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

