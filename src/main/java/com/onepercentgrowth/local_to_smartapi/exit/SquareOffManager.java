package com.onepercentgrowth.local_to_smartapi.exit;

import com.onepercentgrowth.local_to_smartapi.enums.PositionSide;
import com.onepercentgrowth.local_to_smartapi.execution.OrderActionExecutor;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.model.TradeBookEntry;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.service.OrderBookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class SquareOffManager {

    private static final Logger log =
            LoggerFactory.getLogger(SquareOffManager.class);

    private final OrderActionExecutor actionExecutor;
    private final AggressiveExitManager aggressiveExitManager;
    private final OrderBookService orderBookService;
    private final ApplicationProperties applicationProperties;

    public SquareOffManager(OrderActionExecutor actionExecutor,
                            AggressiveExitManager aggressiveExitManager,
                            OrderBookService orderBookService,
                            ApplicationProperties applicationProperties) {
        this.actionExecutor = actionExecutor;
        this.aggressiveExitManager = aggressiveExitManager;
        this.orderBookService = orderBookService;
        this.applicationProperties = applicationProperties;
    }

//    public void squareOff(OrderContext ctx) {
//
//        log.warn("3:00 PM SquareOff initiated | {}", ctx.getTradingSymbol());
//
//        // 1️⃣ Cancel everything
//        cancelIfPresent(ctx.getBuyOrderId(), ctx.getBuyVariety(), "BUY");
//        cancelIfPresent(ctx.getSellOrderId(), ctx.getSellVariety(), "SELL");
//        cancelIfPresent(ctx.getStopLossOrderId(), ctx.getStopLossVariety(), "STOPLOSS");
//
//        // 2️⃣ Calculate net position
//        int netQty =
//                ctx.getLastBuyFilledQty()
//                        - ctx.getLastSellFilledQty()
//                        - ctx.getLastStoplossFilledQty();
//
//        if (netQty <= 0) {
//            log.info("No open position to square off");
//            return;
//        }
//
//        // 3️⃣ Force exit
//        aggressiveExitManager.placeAggressiveExit(
//                ctx, netQty, ExitType.EOD_SQUARE_OFF
//        );
//    }

//    public Mono<Void> squareOff(OrderContext ctx) {
//
//        if (!ctx.tryStartExit()) {
//            return Mono.empty();
//        }
//
//        return Mono.when(
//                        cancelIfPresent(ctx.getBuyOrderId(), ctx.getBuyVariety(), "BUY"),
//                        cancelIfPresent(ctx.getSellOrderId(), ctx.getSellVariety(), "SELL"),
//                        cancelIfPresent(ctx.getStopLossOrderId(), ctx.getStopLossVariety(), "STOPLOSS")
//                )
//                .then(Mono.defer(() -> {
//
////                  best option after canceling the order is to first check the tradebook
////                  and get the values from there and then
////                  calculate the netQty for most robust solution
//
//                    int netQty =
//                            ctx.getLastBuyFilledQty()
//                                    - ctx.getLastSellFilledQty()
//                                    - ctx.getLastStoplossFilledQty();
//
//                    if (netQty <= 0) {
//                        return Mono.empty();
//                    }
//
//                    log.warn("EOD squareoff | symbol={} | netQty={}",
//                            ctx.getTradingSymbol(), netQty);
//
//                    return aggressiveExitManager.placeAggressiveExit(
//                            ctx,
//                            netQty,
//                            ExitType.EOD_SQUARE_OFF
//                    );
//                }));
//    }

    public Mono<Void> squareOff(OrderContext ctx) {

//        if (!ctx.tryStartExit()) {
        if(ctx.getTradeCompleted().get()){
            return Mono.empty();
        }

//        String jwt = tokenManager.getValidJwtToken();

        return Mono.when(
                        cancelIfPresent(ctx.getBuyOrderId(), ctx.getBuyVariety(), "BUY"),
                        cancelIfPresent(ctx.getSellOrderId(), ctx.getSellVariety(), "SELL"),
                        cancelIfPresent(ctx.getStopLossOrderId(), ctx.getStopLossVariety(), "STOPLOSS")
                )

                // Wait for exchange state to stabilize
                .then(Mono.delay(Duration.ofMillis(applicationProperties.getExitStratWaitTimeAfterCancel())))

                // Fetch latest tradebook
//                .then(brokerApiClient.getTradeBook(jwt))
                .then(orderBookService.fetchTradeBook_v1())

                .flatMap(tradeBook -> {

                    ctx.getTradeCompleted().set(true);

                    List<TradeBookEntry> trades = Optional.ofNullable(tradeBook.getData())
                            .orElse(List.of())
                            .stream()
                            .filter(t -> ctx.getTradingSymbol().equals(t.getTradingsymbol()))
                            .toList();

//                    List<TradeBookEntry> trades = tradeBook.getData();

                    int buyFilled = getFilledQty(trades, ctx.getBuyOrderId());
                    int sellFilled = getFilledQty(trades, ctx.getSellOrderId());
                    int slFilled = getFilledQty(trades, ctx.getStopLossOrderId());

//                    int netQty = buyFilled - sellFilled - slFilled;

                    int netQty;

                    if (ctx.getPositionSide() == PositionSide.LONG) {
                        netQty = buyFilled - sellFilled - slFilled;
                    } else {
                        netQty = sellFilled - buyFilled - slFilled;
                    }

                    if (netQty <= 0) {
                        log.info("SquareOff skipped | symbol={} | no open position",
                                ctx.getTradingSymbol());
                        return Mono.empty();
                    }

                    log.warn("EOD squareoff | symbol={} | netQty={}",
                            ctx.getTradingSymbol(), netQty);

                    return aggressiveExitManager.placeAggressiveExit(
                            ctx,
                            netQty,
                            ExitType.EOD_SQUARE_OFF
                    );
                });
    }

//    private void cancelIfPresent(String orderId, String variety, String orderType) {
//        if (orderId != null) {
//            actionExecutor.cancelOrder(orderId, variety, orderType).subscribe();
//        }
//    }

    private Mono<Void> cancelIfPresent(String orderId, String variety, String orderType) {

        if (orderId == null) {
            return Mono.empty();
        }

        return actionExecutor.cancelOrder(orderId, variety, orderType);
    }

    private int getFilledQty(List<TradeBookEntry> trades, String orderId) {

        if (orderId == null) {
            return 0;
        }

        return trades.stream()
                .filter(t -> orderId.equals(t.getOrderid()))
                .mapToInt(t -> Integer.parseInt(t.getFillsize()))
                .sum();
    }
}

