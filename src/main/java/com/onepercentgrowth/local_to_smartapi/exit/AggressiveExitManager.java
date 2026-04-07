package com.onepercentgrowth.local_to_smartapi.exit;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.execution.OrderActionExecutor;
import com.onepercentgrowth.local_to_smartapi.marketdata.MarketDataService;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.model.TradeBookEntry;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.service.OrderBookService;
import com.onepercentgrowth.local_to_smartapi.service.OrderExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AggressiveExitManager {

    private static final Logger log =
            LoggerFactory.getLogger(AggressiveExitManager.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final MarketDataService marketDataService;
    private final OrderRegistry orderRegistry;
    private final OrderBookService orderBookService;
    private final ApplicationProperties applicationProperties;
    private final OrderActionExecutor actionExecutor;

    public AggressiveExitManager(OrderExecutionService executionService,
                                 TokenManager tokenManager,
                                 MarketDataService marketDataService,
                                 OrderRegistry orderRegistry,
                                 OrderBookService orderBookService,
                                 ApplicationProperties applicationProperties,
                                 OrderActionExecutor actionExecutor) {
        this.executionService = executionService;
        this.tokenManager = tokenManager;
        this.marketDataService = marketDataService;
        this.orderRegistry = orderRegistry;
        this.orderBookService = orderBookService;
        this.applicationProperties = applicationProperties;
        this.actionExecutor = actionExecutor;
    }

//    public void placeAggressiveExit(
//            OrderContext ctx,
//            int quantity,
//            ExitType reason
//    ) {
//        BigDecimal ltp = marketDataService.getLastTradedPrice(ctx.getTradingSymbol());
//
//        double aggressivePrice =
//                ltp.doubleValue() * 0.995; // configurable
//
//        log.warn("Aggressive exit | reason={} | price={}",
//                reason, aggressivePrice);
//
//        executionService.placeSellOrder(
//                ctx.getTradingSymbol(),
//                ctx.getSymbolToken(),
//                quantity,
//                aggressivePrice,
//                tokenManager.getValidJwtToken()
//        ).subscribe();
//    }

//    public Mono<Void> placeAggressiveExit(
//            OrderContext ctx,
//            int quantity,
//            ExitType reason
//    ) {
//
////        need to think of a way using which, if aggrassieve price is not met then
////        we need to go down further, need to add that logic
//
//        return marketDataService
//                .getLastTradedPrice(ctx.getExchange(), ctx.getSymbolToken())
//
//                .flatMap(ltp -> {
//
//                    double aggressivePrice = ltp.doubleValue() * 0.995;
//
//                    log.warn("Aggressive exit | reason={} | price={}", reason, aggressivePrice);
//
//                    return executionService.placeSellOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            quantity,
//                            aggressivePrice,
//                            tokenManager.getValidJwtToken()
//                    );
//                })
//                .then();
//    }


    public Mono<Void> placeAggressiveExit(
            OrderContext ctx,
            int quantity,
            ExitType reason
    ) {

        // Prevent duplicate exit triggers
//        if (ctx.isExitInProgress()) {
//            log.warn("Exit already in progress for {}", ctx.getTradingSymbol());
//            return Mono.empty();
//        }
        if (!ctx.tryStartExit()) {
            return Mono.empty();
        }

//        ctx.setExitInProgress(true);
//        ctx.endExit();

        int maxAttempts = applicationProperties.getExitMaxAttempt();

        AtomicInteger remainingQty = new AtomicInteger(quantity);

        return Flux.range(1, maxAttempts)

                .concatMap(attempt -> {

                    if (remainingQty.get() <= 0) {
                        return Mono.empty();
                    }

                    return marketDataService
                            .getQuote(ctx.getExchange(), ctx.getSymbolToken(), "FULL")

                            .flatMap(quote -> {

                                double bestBid = quote.getBestBid().doubleValue();
                                double tick = applicationProperties.getTicksizeToReduce();

                                double price = bestBid - tick;

                                log.warn("Exit attempt={} | remaining={} | price={}",
                                        attempt,
                                        remainingQty.get(),
                                        price);

                                return executionService.placeSellOrder(
                                                ctx.getTradingSymbol(),
                                                ctx.getSymbolToken(),
                                                remainingQty.get(),
                                                price,
                                                tokenManager.getValidJwtToken()
                                        )

                                        // store exit order
                                        .flatMap(orderResponse -> {

                                            ctx.addExitOrder(orderResponse.getData().getOrderid(), "NORMAL");

                                            orderRegistry.registerExit(orderResponse.getData().getOrderid(), ctx);

                                            log.info("Exit order placed | orderId={} | attempt={}",
                                                    orderResponse,
                                                    attempt);

                                            return Mono.just(orderResponse);
                                        });
                            })

                            // wait briefly for fills
                            .then(Mono.delay(Duration.ofMillis(applicationProperties.getWaitTimeBetweePartialExits())))

                            // check tradebook
                            .then(orderBookService.fetchTradeBook_v1())

                            .flatMap(tradeBook -> {

                                String exitOrderId = ctx.getCurrentExitOrderId();

                                int filled = getFilledQty(
                                        tradeBook.getData(),
                                        exitOrderId
                                );

                                remainingQty.set(quantity - filled);

                                if (remainingQty.get() <= 0) {

                                    log.info("Exit completed | symbol={} | qty={}",
                                            ctx.getTradingSymbol(),
                                            quantity);

                                    return Mono.error(new RuntimeException("EXIT_DONE"));
                                }

                                log.warn("Partial fill | remaining={} | canceling order={}",
                                        remainingQty.get(),
                                        exitOrderId);

                                return cancelIfPresent(
                                        exitOrderId,
                                        ctx.getCurrentExitVariety(),
                                        "EXIT"
                                );
                            });
                })

                .onErrorResume(e -> {

                    if ("EXIT_DONE".equals(e.getMessage())) {
                        return Mono.empty();
                    }

                    return Mono.error(e);
                })

                .doFinally(signal -> {

//                    ctx.setExitInProgress(false);
                    ctx.endExit();

                    log.info("Exit flow finished | symbol={} | reason={}",
                            ctx.getTradingSymbol(),
                            reason);
                })

                .then();
    }

    private int getFilledQty(List<TradeBookEntry> trades, String orderId) {

        if (trades == null || orderId == null) {
            return 0;
        }

        return trades.stream()
                .filter(t -> orderId.equals(t.getOrderid()))
                .mapToInt(t -> {
                    try {
                        return Integer.parseInt(t.getFillsize());
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();
    }

//    private Mono<Void> cancelIfPresent(
//            String orderId,
//            String variety,
//            String tag
//    ) {
//
//        if (orderId == null || variety == null) {
//            return Mono.empty();
//        }
//
//        log.warn("Cancelling {} order {}", tag, orderId);
//
//        return executionService
//                .cancelOrder(orderId, variety, tokenManager.getValidJwtToken())
//                .doOnSuccess(v ->
//                        log.info("{} order cancelled {}", tag, orderId)
//                )
//                .onErrorResume(e -> {
//                    log.error("Cancel failed for order {} : {}", orderId, e.getMessage());
//                    return Mono.empty();
//                });
//    }

//    private Mono<Void> cancelIfPresent(
//            String orderId,
//            String variety,
//            String tag
//    ) {
//
//        if (orderId == null || variety == null) {
//            return Mono.empty();
//        }
//
//        log.warn("Cancelling {} order {}", tag, orderId);
//
//        return executionService
//                .placeCancelOrder(
//                        orderId,
//                        variety,
//                        tokenManager.getValidJwtToken(),
//                        tag
//                )
//                .doOnSuccess(resp ->
//                        log.info("{} order cancelled {}", tag, orderId)
//                )
//                .then()   // convert Mono<OrderResponse> -> Mono<Void>
//                .onErrorResume(e -> {
//                    log.error("Cancel failed for order {} : {}", orderId, e.getMessage());
//                    return Mono.empty();
//                });
//    }

    private Mono<Void> cancelIfPresent(String orderId, String variety, String orderType) {

        if (orderId == null) {
            return Mono.empty();
        }

        return actionExecutor.cancelOrder(orderId, variety, orderType);
    }

//    private Mono<Void> cancelIfPresent(String orderId, String variety, String orderType) {
//
//        if (orderId == null) {
//            return Mono.empty();
//        }
//
//        return actionExecutor.cancelOrder(orderId, variety, orderType);
//    }
}

