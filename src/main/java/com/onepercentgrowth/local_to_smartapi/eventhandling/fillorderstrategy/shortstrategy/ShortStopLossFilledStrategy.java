package com.onepercentgrowth.local_to_smartapi.eventhandling.fillorderstrategy.shortstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.eventhandling.fillorderstrategy.IFillOrderStrategy;
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
public class ShortStopLossFilledStrategy implements IFillOrderStrategy {

    private static final Logger log =
            LoggerFactory.getLogger(ShortStopLossFilledStrategy.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties applicationProperties;

    public ShortStopLossFilledStrategy(
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
                && response.getOrderStatusData().getOrderid().equals(ctx.getStopLossOrderId());
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
//        log.warn("SHORT STOPLOSS HIT | stock={}", ctx.getTradingSymbol());
//
//        cancelTarget(ctx);
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
//
//    private void cancelTarget(OrderContext ctx) {
//        if (ctx.getBuyOrderId() == null) return;
//
//        String jwt = tokenManager.getValidJwtToken();
//
//        executionService.placeCancelOrder(
//                ctx.getBuyOrderId(),
//                ctx.getBuyVariety(),
//                jwt,
//                "BUY"
//        ).subscribe();
//    }

//    @Override
//    public void onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        if (ctx.isTradeCompleted()) {
//            log.warn("Duplicate SHORT SL completion ignored | orderId={}",
//                    response.getOrderStatusData().getOrderid());
//            return;
//        }
//
//        ctx.setTradeCompleted(true);
//
//        BigDecimal executedPrice =
//                new BigDecimal(response.getOrderStatusData().getAverageprice());
//
//        int qty =
//                Integer.parseInt(response.getOrderStatusData().getFilledshares());
//
//        log.warn("SHORT STOPLOSS HIT | stock={} | sellOrderId={} | slOrderId={} |  qty={}",
//                ctx.getTradingSymbol(),
//                ctx.getSellOrderId(),
//                ctx.getStopLossOrderId(),
//                ctx.getQuantity()
//        );
//
//        String buyOrderId = ctx.getBuyOrderId();
//        String buyVariety = ctx.getBuyVariety();
//
//        if (buyOrderId == null) {
//            log.warn(
//                    "No SELL to cancel after SL | stock={} | buyOrderId={}",
//                    ctx.getTradingSymbol(),
//                    ctx.getBuyOrderId()
//            );
//            return;
//        }
//
//        /* ================= CANCEL TARGET BUY ================= */
//
//        if (ctx.getBuyOrderId() != null && ctx.isBuyOpen()) {
//
//            String jwt = tokenManager.getValidJwtToken();
//
//            executionService.placeCancelOrder(
//                            ctx.getBuyOrderId(),
//                            ctx.getBuyVariety(),
//                            jwt,
//                            "BUY"
//                    )
//                    .retry(3)
//                    .doOnSuccess(resp -> {
//                        log.info(
//                            "SHORT TARGET cancelled after SL | stock={} | sellOrderId={} | buyOrderId={}",
//                            ctx.getTradingSymbol(),
//                            ctx.getSellOrderId(),
//                            ctx.getBuyOrderId()
//                    );
//                    ctx.setBuyOpen(false);
//                })
//                .subscribe();
//        }
//
//        ctx.setSLOpen(false);
//
//        /* ================= BALANCE UPDATE ================= */
//
////        int quantity = ctx.getQuantity();
//        int quantity = Integer.parseInt(response.getOrderStatusData().getFilledshares());
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
//                "Balance updated after SHORT SL | stock={} | price={} | qty={} | leveragedUsed={} | leverage={}",
//                ctx.getTradingSymbol(),
//                executedPrice,
//                qty,
//                applicationProperties.getLeverageMultiplierToUseForLong(),
//                leverageService.get(normalizedSymbol).multiplier()
//        );
//
//        /* ================= CLEANUP (Optional but recommended) ================= */
//
//        // orderRegistry.remove(ctx);
//    }

    @Override
    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {

        return Mono.defer(() -> {

            // ✅ EXIT GUARD
//            if (!ctx.tryStartExit()) {
//                log.warn("Duplicate SHORT SL completion ignored | orderId={}",
//                        response.getOrderStatusData().getOrderid());
//                return Mono.empty();
//            }

            if (ctx.getTradeCompleted().get()) {
                log.warn("Duplicate SL completion ignored | orderId={}",
                        response.getOrderStatusData().getOrderid());
                return Mono.empty();
            }

            if(!ctx.isSLOpen())
            {
                return Mono.empty();
            }

            log.warn(
                    "SHORT STOPLOSS HIT | stock={} | sellOrderId={} | slOrderId={} | qty={}",
                    ctx.getTradingSymbol(),
                    ctx.getSellOrderId(),
                    ctx.getStopLossOrderId(),
                    ctx.getQuantity()
            );

            int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());

//            OrderContext.SellUpdate update = ctx.reduceSell(filledQty);
//            int delta = update.delta;
            int delta = filledQty - ctx.getLastStoplossFilledQty().get();

            if (delta <= 0) return Mono.empty();

            ctx.getLastStoplossFilledQty().set(filledQty);

            log.info(
                    "SHORT STOPLOSS hit | stock={} | buyOrderId={} | slOrderId={} | qty={} | delta={}",
                    ctx.getTradingSymbol(),
                    ctx.getBuyOrderId(),
                    ctx.getStopLossOrderId(),
                    ctx.getQuantity(),
                    delta
            );

            AtomicBoolean failed = new AtomicBoolean(false);

            String buyOrderId = ctx.getBuyOrderId();
//            String buyVariety = ctx.getBuyVariety();

            String jwt = tokenManager.getValidJwtToken();

            /* ================= CANCEL TARGET BUY ================= */

            Mono<Void> cancelBuyMono = Mono.empty();

            if (ctx.getBuyOrderId() != null && ctx.isBuyOpen()) {

                cancelBuyMono = executionService
                        .placeCancelOrder(
                                ctx.getBuyOrderId(),
                                ctx.getBuyVariety(),
                                jwt,
                                "BUY"
                        )
                        .timeout(Duration.ofSeconds(5))
                        .retryWhen(
                                Retry.backoff(3, Duration.ofMillis(200))
                                        .doBeforeRetry(rs ->
                                                log.warn("Retrying SHORT BUY cancel... attempt={}", rs.totalRetries())
                                        )
                        )
                        .doOnSuccess(resp -> {
                            log.info(
                                    "SHORT TARGET cancelled after SL | stock={} | buyOrderId={} | sellOrderId={}",
                                    ctx.getTradingSymbol(),
                                    ctx.getBuyOrderId(),
                                    ctx.getSellOrderId()
                            );
                            ctx.setBuyOpen(false);
                        })
//                        .doOnError(e ->
//                                log.error("Failed to cancel BUY | orderId={}",
//                                        ctx.getBuyOrderId(), e)
//                        )
//                        .onErrorResume(e -> Mono.empty())
                        .onErrorResume(e -> {
                            failed.set(true);
                            ctx.markInconsistent();
                            log.error("Failed to cancel SHORT BUY - marking inconsistent | orderId={}", buyOrderId, e);
                            return Mono.empty();
                        })
                        .then();
            }

//            ctx.setSLOpen(false);
//            ctx.setTradeCompleted(true);

            /* ================= BALANCE ================= */

            BigDecimal executedPrice =
                    new BigDecimal(response.getOrderStatusData().getAverageprice());

//            int quantity =
//                    Integer.parseInt(response.getOrderStatusData().getFilledshares());

            String normalizedSymbol =
                    Utility.normalize(ctx.getTradingSymbol());

            Mono<Void> balanceMono = Mono.fromRunnable(() ->
                    balanceService.onBuy(
                            executedPrice,
                            delta,
                            applicationProperties.getLeverageMultiplierToUseForShort(),
                            balanceService.getUsableBalance(),
                            leverageService.get(normalizedSymbol).multiplier()
                    )
            );

            /* ================= FINAL FLOW ================= */

//            return cancelBuyMono
//                    .then(balanceMono)
//                    .onErrorResume(e -> {
//                        log.error("Error in SHORT SL flow", e);
//                        return Mono.empty();
//                    });
            return cancelBuyMono
                    .then(Mono.defer(() -> {

                        if (failed.get()) {
                            log.warn("Skipping balance due to cancel failure");
                            return Mono.empty();
                        }

                        // ✅ SAFE STATE UPDATE
                        ctx.setSLOpen(false);
                        ctx.getTradeCompleted().set(true);

                        return balanceMono
                                .onErrorResume(e -> {
                                    log.error("Balance update failed", e);
                                    return Mono.empty();
                                });
                    }));
        });
    }
}