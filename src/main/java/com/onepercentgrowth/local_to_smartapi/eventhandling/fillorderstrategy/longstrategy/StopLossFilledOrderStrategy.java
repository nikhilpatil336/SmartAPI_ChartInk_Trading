package com.onepercentgrowth.local_to_smartapi.eventhandling.fillorderstrategy.longstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.eventhandling.fillorderstrategy.IFillOrderStrategy;
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
public class StopLossFilledOrderStrategy implements IFillOrderStrategy {

    private static final Logger log = LoggerFactory.getLogger(StopLossFilledOrderStrategy.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final OrderRegistry orderRegistry;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties applicationProperties;

    public StopLossFilledOrderStrategy(
            OrderExecutionService executionService,
            TokenManager tokenManager,
            OrderRegistry orderRegistry,
            BalanceService balanceService,
            LeverageService leverageService,
            ApplicationProperties applicationProperties
    ) {
        this.executionService = executionService;
        this.tokenManager = tokenManager;
        this.orderRegistry = orderRegistry;
        this.balanceService = balanceService;
        this.leverageService = leverageService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return ctx.isLong()
                && "SELL".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getStopLossOrderId());
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
//        log.warn(
//                "STOPLOSS hit | stock={} | buyOrderId={} | slOrderId={} | qty={}",
//                ctx.getTradingSymbol(),
//                ctx.getBuyOrderId(),
//                ctx.getStopLossOrderId(),
//                ctx.getQuantity()
//        );
//
//
//        String sellOrderId = ctx.getSellOrderId();
//        String sellVariety = ctx.getSellVariety();
//
//        if (sellOrderId == null) {
//            log.warn(
//                    "No SELL to cancel after SL | stock={} | buyOrderId={}",
//                    ctx.getTradingSymbol(),
//                    ctx.getBuyOrderId()
//            );
//            return;
//        }
//
//
//        String jwtToken = tokenManager.getValidJwtToken();
//
//        executionService
//                .placeCancelOrder(sellOrderId, sellVariety, jwtToken, "SELL")
//                .retry(3)
//                .doOnSuccess(resp -> {
//                    log.info(
//                            "SELL cancelled after SL | stock={} | buyOrderId={} | sellOrderId={}",
//                            ctx.getTradingSymbol(),
//                            ctx.getBuyOrderId(),
//                            sellOrderId
//                    );
//                    ctx.setSellOpen(false);
////                    orderRegistry.remove(ctx); // trade complete
//                })
//                .subscribe();
//
//        ctx.setSLOpen(false);
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
//                "Balance updated after SL | stock={} | price={} | qty={} | leveragedUsed={} | leverage={}",
//                ctx.getTradingSymbol(),
//                executedPrice,
//                quantity,
//                applicationProperties.getLeverageMultiplierToUseForLong(),
//                leverageService.get(normalizedSymbol).multiplier()
//        );
//
//
////        log.warn("STOPLOSS hit, balance updated: price={}, qty={}",
////                executedPrice, quantity);
//    }

    @Override
    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {

        return Mono.defer(() -> {


        if (ctx.getTradeCompleted().get()) {
            log.warn("Duplicate SL completion ignored | orderId={}",
                    response.getOrderStatusData().getOrderid());
            return Mono.empty();
        }

        if (!ctx.isSLOpen()) {
            return Mono.empty();
        }

            int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());

//            OrderContext.SellUpdate update = ctx.reduceSell(filledQty);
//            int delta = update.delta;
            int delta = filledQty - ctx.getLastStoplossFilledQty().get();

            if (delta <= 0) return Mono.empty();

            ctx.getLastStoplossFilledQty().set(filledQty);

//        ctx.setTradeCompleted(true);

            log.info(
                    "STOPLOSS hit | stock={} | buyOrderId={} | slOrderId={} | qty={} | delta={}",
                    ctx.getTradingSymbol(),
                    ctx.getBuyOrderId(),
                    ctx.getStopLossOrderId(),
                    ctx.getQuantity(),
                    delta
            );

            AtomicBoolean failed = new AtomicBoolean(false);

            String sellOrderId = ctx.getSellOrderId();
//        String sellVariety = ctx.getSellVariety();

            String jwtToken = tokenManager.getValidJwtToken();

            Mono<Void> cancelSellMono = Mono.empty();

            if (ctx.getSellOrderId() != null && ctx.isSellOpen()) {
                cancelSellMono = executionService
                        .placeCancelOrder(
                                ctx.getSellOrderId(),
                                ctx.getSellVariety(),
                                jwtToken,
                                "SELL"
                        )
                        .timeout(Duration.ofSeconds(5))
                        .retryWhen(
                                Retry.backoff(3, Duration.ofMillis(200))
                                        .doBeforeRetry(rs ->
                                                log.warn("Retrying BUY cancel... attempt={}", rs.totalRetries())
                                        )
                        )
                        .doOnSuccess(resp -> {
                            log.info(
                                    "SELL cancelled after SL | stock={} | buyOrderId={} | sellOrderId={}",
                                    ctx.getTradingSymbol(),
                                    ctx.getBuyOrderId(),
                                    ctx.getSellOrderId()
                            );
                            ctx.setSellOpen(false);
                        })
//                    .doOnError(e ->
//                            log.error("Failed to cancel SELL | orderId={}",
//                                    ctx.getSellOrderId(), e)
//                    )
//                    .onErrorResume(e -> Mono.empty())
                        .onErrorResume(e -> {
                            failed.set(true);
                            ctx.markInconsistent();
                            log.error("Failed to cancel BUY - marking inconsistent | orderId={}", sellOrderId, e);
                            return Mono.empty();
                        })
                        .then();
            }

//        ctx.setSLOpen(false);

            BigDecimal executedPrice =
                    new BigDecimal(response.getOrderStatusData().getPrice());

//            int quantity =
//                    Integer.parseInt(response.getOrderStatusData().getFilledshares());

            String normalizedSymbol =
                    Utility.normalize(ctx.getTradingSymbol());

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

//        return cancelSellMono.then(balanceMono);

            return cancelSellMono
                    .then(Mono.defer(() -> {

                        if (failed.get()) {
                            log.warn("Skipping balance due to cancel failure");
                            return Mono.empty();
                        }

                        // ✅ SAFE STATE UPDATE
                        ctx.setSLOpen(false);
//                        ctx.setTradeCompleted(true);
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