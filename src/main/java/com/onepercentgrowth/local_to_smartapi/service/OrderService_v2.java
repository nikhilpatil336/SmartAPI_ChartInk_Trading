package com.onepercentgrowth.local_to_smartapi.service;

import com.google.gson.JsonObject;
import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.enums.AlertState;
import com.onepercentgrowth.local_to_smartapi.enums.PositionSide;
import com.onepercentgrowth.local_to_smartapi.enums.TradingExchange;
import com.onepercentgrowth.local_to_smartapi.eventhandling.OrderEventDispatcher;
import com.onepercentgrowth.local_to_smartapi.eventhandling.OrderEventQueue;
import com.onepercentgrowth.local_to_smartapi.eventhandling.PendingOrderEventStore;
import com.onepercentgrowth.local_to_smartapi.factory.OrderRequestFactory;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;
import com.onepercentgrowth.local_to_smartapi.historicdata.HistoricalDataResponse;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.model.*;
import com.onepercentgrowth.local_to_smartapi.registry.AlertStateRegistry;
import com.onepercentgrowth.local_to_smartapi.utility.Utility;
import com.onepercentgrowth.local_to_smartapi.model.chartink_request.IOrderRequest;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.storage.SlOrderStore;
import com.onepercentgrowth.local_to_smartapi.storage.TokenStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


@Service
public class OrderService_v2 {

    private static final Logger log = LoggerFactory.getLogger(OrderService_v2.class);

    @Autowired
    private BrokerApiClient brokerApiClient;
    @Autowired
    private TokenStorageService tokenStorageService;
    @Autowired
    private ScripMasterService scripMasterService;
    @Autowired
    private SlOrderStore slOrderStore;
    @Autowired
    private ApplicationProperties applicationProperties;
    @Autowired
    private OrderRegistry orderRegistry;
    @Autowired
    private OrderRequestFactory orderRequestFactory;
    @Autowired
    private OrderCalculationService orderCalculationService;
    @Autowired
    private TokenManager tokenManager;
    @Autowired
    private OrderEventQueue orderEventQueue;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private LeverageService leverageService;
    @Autowired
    private OrderCalculationService calculationService;
    @Autowired
    private AlertStateRegistry alertStateRegistry;
    @Autowired
    private PendingOrderEventStore pendingOrderEventStore;
    @Autowired
    private OrderEventDispatcher orderEventDispatcher;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(5);

    private static final DateTimeFormatter API_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

//  ------------------- 1st version of buy order --------------------------------

    public Mono<OrderResponse> chartinkBuyOrder(WebhookRequest webhookRequest) {

        String stockName = webhookRequest.getStocks().split(",")[0].trim();
        String price = webhookRequest.getTrigger_prices().split(",")[0].trim();
        double triggerPrice = Double.parseDouble(price);

        // 1. Get token
        String symboltoken = scripMasterService.getTokenForName(stockName);
        if (symboltoken == null) {
            return Mono.error(new RuntimeException("TradingSymbol not found for stock: " + stockName));
        }

        // 2. RMS balance
//        RmsData rmsData = scripMasterService.getRmsData();
//        if (rmsData == null) {
//            return Mono.error(new RuntimeException("RMS Data not available. Fetch balance first."));
//        }
//
////        Double availableCash = (Double.parseDouble(rmsData.getAvailablecash())*applicationProperties.getPercentBalanceUse());
//        Double availableCash = (balanceService.getCurrentBalance()*applicationProperties.getPercentBalanceUse());
////      ***************************
////        availableCash = 10000.00;
//
//        if (availableCash <= applicationProperties.getBalanceMinimumAllowed()) {
//            return Mono.error(new RuntimeException("Insufficient balance: " + availableCash));
//        }
//
//        // 3. Quantity
//        int quantity = (int) (Math.floor(availableCash / triggerPrice)-applicationProperties.getNumberOfStocksBuyLess());
//
////        if(quantity > 1)
////            quantity -= applicationProperties.getNumberOfStocksBuyLess();
//
//        if (quantity <= applicationProperties.getStockBuyMinimumQuantityRequired()) {
//            return Mono.error(new RuntimeException("Not enough cash to buy " + applicationProperties.getStockBuyMinimumQuantityRequired()+1 + " shares."));
//        }

        BigDecimal usableCash =
                balanceService.getUsableBalance(
                        applicationProperties.getPercentBalanceUse()
                );

        if (usableCash.doubleValue()
                <= applicationProperties.getBalanceMinimumAllowed()) {

            return Mono.error(
                    new RuntimeException(
                            "Insufficient balance: " + usableCash
                    )
            );
        }

        int leverageMultiplier = applicationProperties.getLeverageMultiplierToUseForLong();
        int maxLeverage = (int) leverageService.get(stockName).multiplier();

        if(leverageMultiplier < 1)
            throw new IllegalArgumentException("Invalid Leverage Multiplier");

        if(leverageMultiplier > maxLeverage)
            leverageMultiplier = maxLeverage;

        double calculateQuantity = usableCash.doubleValue() / triggerPrice;
        double quantityAfterLeverage = calculateQuantity * leverageMultiplier;
        int absQuantity = (int) Math.floor(Math.abs(quantityAfterLeverage));
//        double currentBalance = usableCash.doubleValue() - (triggerPrice * (double) absQuantity / leverageMultiplier);
//        double rmsBalance = usableCash.doubleValue() - (absQuantity * (triggerPrice / maxLeverage));

        int quantity = absQuantity - applicationProperties.getNumberOfStocksBuyLessForLong();

//        int quantity =
//                (int) Math.floor(
//                        usableCash.doubleValue() / triggerPrice
//                ) - applicationProperties.getNumberOfStocksBuyLess();

        if (quantity <= applicationProperties.getStockBuyMinimumQuantityRequired()) {
            return Mono.error(
                    new RuntimeException(
                            "Not enough cash to buy minimum quantity"
                    )
            );
        }

        // 🔐 HARD RISK CHECK
        balanceService.assertSufficientFunds(
                BigDecimal.valueOf(triggerPrice),
                quantity
        );

        // 4. JWT
        String jwtToken = tokenStorageService.getJwtToken();
        if (jwtToken == null) {
            return Mono.error(new RuntimeException("User not logged in. No JWT token found."));
        }

        IOrderRequest buyOrderRequest =
                orderRequestFactory.createBuyOrder(stockName, symboltoken, quantity, price);

        return brokerApiClient
                .chartinkPlaceOrder(buyOrderRequest, jwtToken)
                .flatMap(buyResponse -> {

                    if (!buyResponse.isStatus()) {
                        return Mono.error(new RuntimeException("BUY order failed"));
                    }

                    String buyOrderId = buyResponse.getData().getOrderid();

                    // 🔁 WAIT until BUY is COMPLETE (retry 5 times, 2 sec delay)
                    return pollOrderUntilComplete(buyOrderId, jwtToken)
                            .flatMap(executedPrice  -> {

                                double strikePrice = executedPrice;

                                double profitTriggerPrice = strikePrice * applicationProperties.getBuyProfitPercentageMultiplier();

                                IOrderRequest sellOrder =
                                        orderRequestFactory.createSellLimitOrder(
                                                stockName,
                                                symboltoken,
                                                quantity,
                                                profitTriggerPrice
                                        );

                                // 2% SL from executed price
//                                double slTriggerPrice = strikePrice * applicationProperties.getStoplossPercentageMultiplier();

                                StopLossPrice slPrice =
                                        calculationService.calculateStopLossPrice(
                                                BigDecimal.valueOf(triggerPrice),
                                                BigDecimal.valueOf(applicationProperties.getTradingStoplossPercent()),
                                                BigDecimal.valueOf(applicationProperties.getTradingStoplossBufferPercent())
                                        );

                                IOrderRequest sellSlLimitOrder =
                                        orderRequestFactory.createStopLossLimitOrder(
                                                stockName,
                                                symboltoken,
                                                quantity,
                                                slPrice.triggerPrice().doubleValue(),
                                                slPrice.limitPrice().doubleValue(),
                                                "SELL"
                                        );

                                Mono<OrderResponse> sellMono =
                                        withOrderRetry(
                                                brokerApiClient.chartinkPlaceOrder(sellOrder, jwtToken)
                                                        .flatMap(resp -> {
                                                            if (!resp.isStatus()) {
                                                                return Mono.error(
                                                                        new RuntimeException("SELL order failed")
                                                                );
                                                            }
                                                            return Mono.just(resp);
                                                        })
                                        );

                                Mono<OrderResponse> slmMono =
                                        withOrderRetry(
                                                brokerApiClient.chartinkPlaceOrder(sellSlLimitOrder, jwtToken)
                                                        .flatMap(resp -> {
                                                            if (!resp.isStatus()) {
                                                                return Mono.error(
                                                                        new RuntimeException("SLM order failed")
                                                                );
                                                            }
                                                            return Mono.just(resp);
                                                        })
                                                        .doOnSuccess(slResp -> {
                                                            slOrderStore.put(
                                                                    "NSE:" + stockName + ":" + LocalDate.now(),
                                                                    new SlOrderMeta(
                                                                            buyOrderId,
                                                                            slResp.getData().getOrderid(),
                                                                            quantity,
                                                                            slPrice.triggerPrice().doubleValue(),
                                                                            symboltoken
                                                                    )
                                                            );
                                                        })
                                        );

                                return Mono.zip(sellMono, slmMono)
                                        .doOnSuccess(tuple -> {
                                            OrderResponse sellResp = tuple.getT1();
                                            OrderResponse slResp = tuple.getT2();

                                            OrderContext ctx =
                                                    new OrderContext(
                                                            buyOrderId,
                                                            sellResp.getData().getOrderid(),
                                                            slResp.getData().getOrderid(),
                                                            stockName,
                                                            symboltoken,
                                                            quantity,
                                                            "NORMAL",
                                                            "STOPLOSS"
                                                    );

                                            orderRegistry.register(ctx);
                                        })
                                        .thenReturn(buyResponse);
                            });
                });
    }

    public Mono<Double> pollOrderUntilComplete(String orderId, String jwtToken) {

//       return Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
        return Flux.interval(Duration.ZERO, Duration.ofMillis(applicationProperties.getOrderBookRetryMilliseconds()))
                .flatMap(tick ->
                        getOrderById(orderId, jwtToken)
                                .map(this::mapToOrderResult)        // OrderStatusItem → OrderResult
                                .defaultIfEmpty(OrderResult.notFound())
                )
                .filter(OrderResult::isTerminal)
                .map(OrderResult::getResult)
                .next();
    }

    private Mono<OrderStatusItem> getOrderById(String orderId, String jwtToken) {

        return brokerApiClient.getOrderBook(jwtToken)
                .map(OrderBookResponse_v2::getData)
                .flatMapMany(Flux::fromIterable)
                .filter(item -> orderId.equals(item.getOrderid()))
                .next();
    }

    private OrderResult mapToOrderResult(OrderStatusItem order) {

        String status = order.getStatus();
//      ******************************************
        status = "COMPLETE";

        if (status == null) {
            return OrderResult.pending(); // defensive fallback
        }

        switch (status.toUpperCase()) {

            // ❌ terminal negative states
            case "REJECTED":
            case "CANCELLED":
                return OrderResult.rejected();

            // ✅ terminal success states
            case "COMPLETE":
            case "FILLED": {
//                double avgPrice = Double.parseDouble(order.getAverageprice());
                double avgPrice = Double.parseDouble(order.getPrice());
                return OrderResult.completed(avgPrice);
            }

            // ⏳ non-terminal states
//          ****************************
//            case "OPEN":
            case "PENDING":
            case "TRIGGER PENDING":
            case "PARTIALLY FILLED":
            case "MODIFIED":
                return OrderResult.pending();

            // 🚨 unknown / unexpected status
            default:
                return OrderResult.rejected();
            // OR if you prefer to fail fast:
            // throw new IllegalStateException("Unknown order status: " + status);
        }
    }

    private <T> Mono<T> withOrderRetry(Mono<T> mono) {

        Retry retrySpec = Retry
                .fixedDelay(5, Duration.ofSeconds(3))
                .filter(ex -> ex instanceof RuntimeException)
                .doBeforeRetry(rs ->
                        log.warn("Retrying operation. Attempt {}",
                                rs.totalRetries() + 1)
                )
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());

        return mono.retryWhen(retrySpec);
    }

//---------------------------2nd version of buy order ---------------------

    public Mono<OrderResponse> chartinkSimpleBuyOrder(WebhookRequest webhookRequest, String exchange) {

        String stockName = webhookRequest.getStocks().split(",")[0].trim();
        String price = webhookRequest.getTrigger_prices().split(",")[0].trim();
//        double triggerPrice = Utility.roundToTick(Double.parseDouble(price));
        BigDecimal triggerPrice = Utility.roundToTick(new BigDecimal(price));


        String symbolToken = scripMasterService.getTokenForName(stockName);
        if (symbolToken == null) {
            return Mono.error(
                    new IllegalStateException("Symbol not found: " + stockName)
            );
        }

        BigDecimal usableCash = balanceService.getUsableBalance();

        log.info(
                "Chartink long BUY received | stock={} | triggerPrice={} | usableCash={}",
                stockName,
                triggerPrice,
                usableCash
        );

        if (usableCash.doubleValue()
                <= applicationProperties.getBalanceMinimumAllowed()) {

            return Mono.error(
                    new RuntimeException(
                            "Insufficient balance: " + usableCash
                    )
            );
        }

        int leverageMultiplier = applicationProperties.getLeverageMultiplierToUseForLong();
        int maxLeverage = (int) leverageService.get(stockName).multiplier();

        if (leverageMultiplier < 1)
            throw new IllegalArgumentException("Invalid Leverage Multiplier");

        if (leverageMultiplier > maxLeverage)
            leverageMultiplier = maxLeverage;

        int calculatedQuantity =
                orderCalculationService.calculateBuyQuantity(
                        usableCash,
                        triggerPrice,
                        leverageMultiplier,
                        maxLeverage
                );

//        double calculateQuantity = usableCash.doubleValue() / triggerPrice;
//        double quantityAfterLeverage = calculateQuantity * leverageMultiplier;
//        int absQuantity = (int) Math.floor(Math.abs(quantityAfterLeverage));
//        double currentBalance = usableCash.doubleValue() - (triggerPrice * (double) absQuantity / leverageMultiplier);
//        double rmsBalance = usableCash.doubleValue() - (absQuantity * (triggerPrice / maxLeverage));

        int quantityAfterBuyingLess = calculatedQuantity - applicationProperties.getNumberOfStocksBuyLessForLong();

        if (quantityAfterBuyingLess <= 0)
            throw new IllegalStateException("After reducing safety quantity from calculated quantity, the quantity become "+ quantityAfterBuyingLess +", which is invalid");

        if (applicationProperties.isFixedQuantityFlag() && quantityAfterBuyingLess > applicationProperties.getFixedQuantity()) {
//            log.info("Taking fixed quantity of {} from property file an applying leverage as {}", applicationProperties.getFixedQuantity(), leverageMultiplier);
            leverageMultiplier = 1;
            quantityAfterBuyingLess = applicationProperties.getFixedQuantity();
            log.info("Taking fixed quantity: {} | leverage I applied: {} | leverage applied from property file due to fixed quantity: {}", quantityAfterBuyingLess, applicationProperties.getLeverageMultiplierToUseForShort(), leverageMultiplier);
        }

        final int quantity = quantityAfterBuyingLess;

        log.info(
                "long BUY calc | stock={} | leverage={}/{} | qtyCalculated={} | qtyFinal={} | buyLess={} | fixedQtyFlag={}",
                stockName,
                leverageMultiplier,
                maxLeverage,
                calculatedQuantity,
                quantity,
                applicationProperties.getNumberOfStocksBuyLessForLong(),
                applicationProperties.isFixedQuantityFlag()
        );

//        balanceService.assertSufficientFunds(
//                triggerPrice,
//                quantity / leverageMultiplier
//        );

        balanceService.assertSufficientMargin(triggerPrice, quantity, leverageMultiplier);

        String jwtToken = tokenManager.getValidJwtToken();

        IOrderRequest buyOrder =
                orderRequestFactory.createBuyOrder(
                        stockName,
                        symbolToken,
                        quantity,
                        triggerPrice.toString()
                );

        log.info(
                "Placing long BUY order | stock={} | price={} | qty={} | marginUsed={} | usableCash={}",
                stockName,
                triggerPrice,
                quantity,
                triggerPrice
                        .multiply(BigDecimal.valueOf(quantity))
                        .divide(BigDecimal.valueOf(leverageMultiplier)),
                usableCash
        );


//        log.info("buy order request: {}", buyOrder);

        return brokerApiClient.chartinkPlaceOrder(buyOrder, jwtToken)
                .doOnSuccess(resp -> {
                    String buyOrderId = resp.getData().getOrderid();
                    OrderContext ctx =
                            new OrderContext(
//                                    buyOrderId,
                                    PositionSide.LONG,
                                    stockName,
                                    symbolToken,
                                    quantity
                            );

                    ctx.setBuyOrderId(buyOrderId);
                    ctx.setBuyVariety("NORMAL");
                    ctx.setExchange(exchange);

                    orderRegistry.registerBuy(ctx);

//                    pendingOrderEventStore.replayPendingEvents(buyOrderId);

                    Queue<com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse> pendingEvents =
                            pendingOrderEventStore.remove(buyOrderId);

                    if (pendingEvents != null) {

                        log.info(
                                "Replaying {} pending events for orderId={}",
                                pendingEvents.size(),
                                buyOrderId
                        );

                        pendingEvents.forEach(orderEventDispatcher::dispatchDirectly);
                    }

                    log.info("BUY order for long trade is registered in order context: {}", ctx);
                });
    }

    public Mono<OrderResponse> chartinkSimpleSellOrder(WebhookRequest webhookRequest, String exchange) {

        String stockName = webhookRequest.getStocks().split(",")[0].trim();
        String price = webhookRequest.getTrigger_prices().split(",")[0].trim();
        BigDecimal triggerPrice = Utility.roundToTick(new BigDecimal(price));

        String symbolToken = scripMasterService.getTokenForName(stockName);
        if (symbolToken == null) {
            return Mono.error(
                    new IllegalStateException("Symbol not found: " + stockName)
            );
        }

        BigDecimal usableCash = balanceService.getUsableBalance();

        log.info(
                "Chartink short SELL received | stock={} | triggerPrice={} | usableCash={}",
                stockName,
                triggerPrice,
                usableCash
        );

        if (usableCash.doubleValue()
                <= applicationProperties.getBalanceMinimumAllowed()) {

            return Mono.error(
                    new RuntimeException(
                            "Insufficient balance: " + usableCash
                    )
            );
        }

        int leverageMultiplier = applicationProperties.getLeverageMultiplierToUseForShort();
        int maxLeverage = (int) leverageService.get(stockName).multiplier();

        if (leverageMultiplier < 1)
            throw new IllegalArgumentException("Invalid Leverage Multiplier");

        if (leverageMultiplier > maxLeverage)
            leverageMultiplier = maxLeverage;

//        BigDecimal maxBuyPrice = triggerPrice.multiply(BigDecimal.valueOf(applicationProperties.getSellProfitPercentageMultiplier()));

        BigDecimal maxBuyPrice =
                calculationService.calculateSellStopLossPrice(triggerPrice);

        int calculatedQuantity = orderCalculationService.calculateSellQuantity(
                usableCash,
                maxBuyPrice,
                leverageMultiplier,
                maxLeverage
        );

        int reducedQuantityForSafety = calculatedQuantity - applicationProperties.getNumberOfStocksSellLessForShort();

        if (reducedQuantityForSafety <= 0)
            throw new IllegalStateException("After reducing safety quantity from calculated quantity, the quantity become" +reducedQuantityForSafety+ ", which is invalid");

        if (applicationProperties.isFixedQuantityFlag() && reducedQuantityForSafety > applicationProperties.getFixedQuantity()) {
            leverageMultiplier = 1;
            reducedQuantityForSafety = applicationProperties.getFixedQuantity();
            log.info("Taking fixed quantity: {} | leverage I applied: {} | leverage applied from property file due to fixed quantity: {}", reducedQuantityForSafety, applicationProperties.getLeverageMultiplierToUseForShort(), leverageMultiplier);
        }
        final int quantity = reducedQuantityForSafety;

        log.info(
                "short SELL calc | stock={} | leverage={}/{} | qtyCalculated={} | qtyFinal={} | buyLess={} | fixedQtyFlag={}",
                stockName,
                leverageMultiplier,
                maxLeverage,
                calculatedQuantity,
                quantity,
                applicationProperties.getNumberOfStocksSellLessForShort(),
                applicationProperties.isFixedQuantityFlag()
        );

//        balanceService.assertSufficientFunds(
//                maxBuyPrice,
//                quantity / leverageMultiplier
//        );

        balanceService.assertSufficientMargin(maxBuyPrice, quantity, leverageMultiplier);

        String jwtToken = tokenManager.getValidJwtToken();

        IOrderRequest sellLimitOrder =
                orderRequestFactory.createSellLimitOrder(
                        stockName,
                        symbolToken,
                        quantity,
                        triggerPrice.doubleValue()
                );

        log.info(
                "Placing short SELL order | stock={} | price={} | qty={} | marginUsed={} | usableCash={}",
                stockName,
                triggerPrice,
                quantity,
                triggerPrice
                        .multiply(BigDecimal.valueOf(quantity))
                        .divide(BigDecimal.valueOf(leverageMultiplier)),
                usableCash
        );

        return brokerApiClient.chartinkPlaceOrder(sellLimitOrder, jwtToken)
                .doOnSuccess(resp -> {
                    String sellOrderId = resp.getData().getOrderid();
                    OrderContext ctx =
                            new OrderContext(
//                                    sellOrderId,
                                    PositionSide.SHORT,
                                    stockName,
                                    symbolToken,
                                    quantity
                            );

                    ctx.setSellOrderId(sellOrderId);
                    ctx.setSellVariety("NORMAL");
                    ctx.setExchange(exchange);

                    orderRegistry.registerSell(ctx);

//                    pendingOrderEventStore.replayPendingEvents(sellOrderId);

                    Queue<com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse> pendingEvents =
                            pendingOrderEventStore.remove(sellOrderId);

                    if (pendingEvents != null) {

                        log.info(
                                "Replaying {} pending events for orderId={}",
                                pendingEvents.size(),
                                sellOrderId
                        );

                        pendingEvents.forEach(orderEventDispatcher::dispatchDirectly);
                    }

                    log.info("SELL order for short trade is registered in order context: {}", ctx);
                });
    }

    public Mono<OrderResponse> handleAlert(WebhookRequest webhookRequest) {

//        String symbol = request.getStock(); // adapt to your field
//        LocalDateTime now = LocalDateTime.now();

        String stockName = webhookRequest.getStocks().split(",")[0].trim();
        String price = webhookRequest.getTrigger_prices().split(",")[0].trim();
        BigDecimal triggerPrice = Utility.roundToTick(new BigDecimal(price));
//        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("EEE, MMM d, yyyy h:mm a", Locale.ENGLISH);

        String formatted = LocalDateTime.now().format(formatter);

        LocalDateTime now = LocalDateTime.parse(formatted, formatter);

        FirstAlertData existing = alertStateRegistry.get(stockName);

        // 🟢 FIRST ALERT
        if (existing == null) {
            return handleFirstAlert(stockName, now, triggerPrice);
        }

        // 🔴 SECOND ALERT
        return handleSecondAlert(stockName, now, existing, webhookRequest);
    }

    private Mono<OrderResponse> handleFirstAlert(String symbol, LocalDateTime now, BigDecimal triggerPrice) {

        log.info("First Alert for stock: {}, now: {}, triggerPrice: {}", symbol, now, triggerPrice);

//        String symbolToken = scripMasterService.getTokenForName(symbol);
//
//        LocalDateTime from = now.minusDays(3).withHour(9).withMinute(15);
//        LocalDateTime to = now.withHour(15).withMinute(30);
//
//        JsonObject requestBody = new JsonObject();
//        requestBody.addProperty("exchange", "NSE");
//        requestBody.addProperty("symboltoken", symbolToken);
//        requestBody.addProperty("interval", "FIVE_MINUTE");
//        requestBody.addProperty("fromdate", from.format(API_FORMAT));
//        requestBody.addProperty("todate", to.format(API_FORMAT));
//
//        String jwtToken = tokenManager.getValidJwtToken();

        FirstAlertData data = new FirstAlertData(
                symbol,
                now,
                triggerPrice.doubleValue(),
                0,
                0,
                0,
                AlertState.WAITING_SECOND_ALERT
        );

        alertStateRegistry.save(symbol, data);

//        ScheduledFuture<?> future = scheduler.schedule(
//                () -> triggerSyntheticSecondAlert(symbol),
//                320,
//                TimeUnit.SECONDS
//        );
//
//        data.setFallbackTask(future);

        return Mono.empty();

//        return fetchWithRetry(requestBody, jwtToken)
//                .doOnNext(candles -> {
//
//                    int index = candles.size() - 1;
//
//                    double sma10 = calculateSMA(candles, index, 10);
//                    double sma20 = calculateSMA(candles, index, 20);
//
//                    FirstAlertData data = new FirstAlertData(
//                            symbol,
//                            now,
//                            triggerPrice.doubleValue(),
//                            sma10,
//                            sma20
//                    );
//
//                    alertStateRegistry.save(symbol, data);
//                })
//                .then(Mono.empty()); // no trade
    }

    private Mono<OrderResponse> handleSecondAlert(
            String symbol,
            LocalDateTime now,
            FirstAlertData firstAlert,
            WebhookRequest request
    ) {

        log.info("Second Alert for stock: {}, now: {}, webhook request: {}", symbol, now, request.toString());

        String symbolToken = scripMasterService.getTokenForName(symbol);

        LocalDateTime from = now.minusDays(3).withHour(9).withMinute(15);
        LocalDateTime to = now.withHour(15).withMinute(30);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("exchange", "NSE");
        requestBody.addProperty("symboltoken", symbolToken);
        requestBody.addProperty("interval", "FIVE_MINUTE");
        requestBody.addProperty("fromdate", from.format(API_FORMAT));
        requestBody.addProperty("todate", to.format(API_FORMAT));

//        requestBody.addProperty("fromdate", "2026-01-19 09:15");
//        requestBody.addProperty("todate", "2026-01-21 09:30");

        String jwtToken = tokenManager.getValidJwtToken();

        // ⛔ Expiry check (5–6 min window)
        if (Duration.between(firstAlert.getTriggeredAt(), now).toMinutes() > 10) {
            log.info("Alert details are: {}", alertStateRegistry.get(symbol).toString());
            alertStateRegistry.remove(symbol);
            return Mono.empty();
        }

//        return fetchWithRetry(requestBody, jwtToken)
        return fetchLastNDaysCandles(symbolToken, applicationProperties.getHistoricDataDays(), jwtToken)
                .flatMap(candles -> {

//                    log.info("all merged data: {}", candles);

//                    DateTimeFormatter formatter =
//                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//                    LocalDateTime customTime = LocalDateTime.parse("2026-01-21 09:30:00", formatter);

//                    int index = candles.size() - 1;
                    int index = findNearestCandleIndex(candles, now);
                    if (index < 0) return null;
                    Candle current = candles.get(index);

//                    log.info("Current candle selected for colour check: {}", current.toString());

                    double sma10 = calculateSMA(candles, index, 10);
                    double sma20 = calculateSMA(candles, index, 20);

                    alertStateRegistry.get(symbol).setSecondPrice(Double.parseDouble(request.getTrigger_prices()));
                    alertStateRegistry.get(symbol).setSma10(sma10);
                    alertStateRegistry.get(symbol).setSma20(sma20);

                    boolean isGreen = current.getClose() > current.getOpen();
                    boolean isRed = current.getClose() <= current.getOpen();

//                    double sma10 = calculateSMA(candles, index, 10);

                    Mono<OrderResponse> result;

                    log.info("Alert details are: {} | volume: {} | color of the candle: {}",
                            alertStateRegistry.get(symbol).toString(), current.getVolume(), isGreen? "Green": "Red");

                    if (isGreen && current.getVolume() > sma10) {
                        log.info("Placing a Long order for stock: {}", symbol);
                        result = chartinkSimpleBuyOrder(request, "NSE");
                    } else if (isRed && current.getVolume() <= sma10) {
                        log.info("Placing a Short order for stock: {}", symbol);
                        result = chartinkSimpleSellOrder(request, "NSE");
                    } else {
                        log.info("Not Placing any order for stock: {}", symbol);
                        result = Mono.empty();
                    }

                    alertStateRegistry.remove(symbol);

                    return result;
                });
    }

    // reuse your logic
    private double calculateSMA(List<Candle> candles, int endIndex, int period) {
        if (endIndex < period - 1) return 0;

        double sum = 0;
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            sum += candles.get(i).getVolume();
        }
        return sum / period;
    }

//    private HistoricalDataResponse fetchWithRetry(JsonObject requestBody, String jwtToken) {
//
//        int attempts = 0;
//
//        while (attempts < 3) {
//            try {
//                HistoricalDataResponse response =
//                        brokerApiClient.getHistoricalCandleData(requestBody, jwtToken).block();
//
//                if (response != null && response.isStatus() && response.getData() != null) {
//                    return response;
//                }
//
//            } catch (Exception e) {
//                System.out.println("Retry attempt " + attempts);
//            }
//
//            attempts++;
//
//            try {
//                Thread.sleep(1500);
//            } catch (InterruptedException ignored) {}
//        }
//
//        return null;

    private Mono<List<Candle>> fetchWithRetry(JsonObject requestBody, String jwtToken) {

        return brokerApiClient.getHistoricalCandleData(requestBody, jwtToken)
                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(1500)))
                .flatMap(response -> {

                    if (response == null || !response.isStatus() || response.getData() == null) {
                        return Mono.empty();
                    }

                    List<Candle> candles = response.getData().stream()
                            .map(this::mapToCandle)
                            .sorted(Comparator.comparing(Candle::getTimestamp))
                            .toList();

                    return Mono.just(candles);
                });
    }

    private Candle mapToCandle(List<Object> row) {

        Candle candle = new Candle();

        candle.setTimestamp((String) row.get(0));
        candle.setOpen(Double.parseDouble(row.get(1).toString()));
        candle.setHigh(Double.parseDouble(row.get(2).toString()));
        candle.setLow(Double.parseDouble(row.get(3).toString()));
        candle.setClose(Double.parseDouble(row.get(4).toString()));
        candle.setVolume(Long.parseLong(row.get(5).toString()));

        return candle;
    }

    private int findNearestCandleIndex(List<Candle> candles, LocalDateTime target) {

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        int closestIndex = -1;
        long minDiff = Long.MAX_VALUE;

        for (int i = 0; i < candles.size(); i++) {

            LocalDateTime candleTime = LocalDateTime.parse(
                    candles.get(i).getTimestamp(),
                    formatter
            );

            long diff = Math.abs(Duration.between(candleTime, target).toMinutes());

            if (diff < minDiff) {
                minDiff = diff;
                closestIndex = i;
            }
        }

        return closestIndex-1;
    }

    private Mono<List<Candle>> fetchLastNDaysCandles(
            String symbolToken,
            int days,
            String jwtToken
    ) {

        List<Candle> merged = new ArrayList<>();

        return Flux.range(0, days)
                .flatMap(dayOffset -> {

                    LocalDate date = LocalDate.now().minusDays(dayOffset);

                    LocalDateTime from = date.atTime(9, 15);
                    LocalDateTime to = date.atTime(15, 30);

                    JsonObject requestBody = new JsonObject();
                    requestBody.addProperty("exchange", "NSE");
                    requestBody.addProperty("symboltoken", symbolToken);
                    requestBody.addProperty("interval", "FIVE_MINUTE");
                    requestBody.addProperty("fromdate", from.format(API_FORMAT));
                    requestBody.addProperty("todate", to.format(API_FORMAT));

                    return fetchWithRetry(requestBody, jwtToken)
//                            .doOnSuccess(list ->
//                                    log.info("Success for: {} -> candles: {}", date, list.size())
//                            )
                            .onErrorResume(e -> {
                                log.error("Failed for date: {} | error: {}", date, e.getMessage());
                                return Mono.just(Collections.emptyList());
                            });

                }, 3) // ⚠️ concurrency = 3 (API limit ~3 req/sec)
                .flatMapIterable(list -> list)
                .collectList()
                .map(candles -> {

                    // ✅ Remove duplicates (by timestamp)
//                    Map<LocalDateTime, Candle> map = new HashMap<>();
//                    for (Candle c : candles) {
//                        map.put(c.getTime(), c);
//                    }

//                    List<Candle> merged = new ArrayList<>(map.values());

                    merged.addAll(candles);

                    // ✅ Sort ascending
                    merged.sort(Comparator.comparing(Candle::getTime));

                    return merged;
                });
    }

    private void triggerSyntheticSecondAlert(String symbol) {

        FirstAlertData data =
                alertStateRegistry.get(symbol);

        if (data == null) {
            return;
        }

        synchronized (data) {

            if (data.getAlertState() != AlertState.WAITING_SECOND_ALERT) {
                return;
            }

            data.setAlertState(AlertState.PROCESSING);
        }

        log.info(
                "Synthetic second alert triggered for symbol: {}",
                symbol
        );

        fetchLatestCompletedCandle(symbol)
                .flatMap(candle -> {

                    WebhookRequest synthetic =
                            new WebhookRequest();

                    synthetic.setStocks(symbol);

                    synthetic.setTrigger_prices(
                            String.valueOf(candle.getClose())
                    );

                    synthetic.setTriggered_at(
                            LocalTime.now().toString()
                    );

                    synthetic.setAlert_name(
                            "Synthetic Alert"
                    );

                    synthetic.setScan_name(
                            "Synthetic Scan"
                    );

                    synthetic.setScan_url(
                            "synthetic"
                    );

                    synthetic.setWebhook_url(
                            "internal"
                    );

                    return handleSecondAlert(
                            symbol,
                            LocalDateTime.now(),
                            data,
                            synthetic
                    );
                })
                .doOnError(error ->
                        log.error(
                                "Synthetic alert failed for symbol: {} | error: {}",
                                symbol,
                                error.getMessage()
                        )
                )
                .subscribe();
    }

    private Mono<Candle> fetchLatestCompletedCandle(
            String symbol
    ) {

        String token =
                scripMasterService.getTokenForName(symbol);

        String jwt =
                tokenManager.getValidJwtToken();

        return fetchLastNDaysCandles(token, 1, jwt)
                .flatMap(candles -> {

                    if (candles == null || candles.isEmpty()) {
                        return Mono.empty();
                    }

                    Candle latest =
                            candles.get(candles.size() - 1);

                    return Mono.just(latest);
                });
    }
}
