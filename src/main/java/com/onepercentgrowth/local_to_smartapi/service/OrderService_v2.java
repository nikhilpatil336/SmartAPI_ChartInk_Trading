package com.onepercentgrowth.local_to_smartapi.service;

import com.google.gson.JsonObject;
import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.enums.AlertState;
import com.onepercentgrowth.local_to_smartapi.enums.PositionSide;
import com.onepercentgrowth.local_to_smartapi.enums.TradingExchange;
import com.onepercentgrowth.local_to_smartapi.eventhandling.OrderEventDispatcher;
import com.onepercentgrowth.local_to_smartapi.marketdata.MarketDataService;
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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.onepercentgrowth.local_to_smartapi.utility.Utility.normalizeToCandleTime;
import static com.onepercentgrowth.local_to_smartapi.utility.Utility.parseTriggeredAt;


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
    @Autowired
    private MarketDataService marketDataService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    private static final DateTimeFormatter API_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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

        BigDecimal usableCash = balanceService.getUsableBalance(applicationProperties.getPercentBalanceUse());

        if (usableCash.doubleValue() <= applicationProperties.getBalanceMinimumAllowed()) {

            return Mono.error(new RuntimeException("Insufficient balance: " + usableCash));
        }

        int leverageMultiplier = applicationProperties.getLeverageMultiplierToUseForLong();
        int maxLeverage = (int) leverageService.get(stockName).multiplier();

        if (leverageMultiplier < 1) throw new IllegalArgumentException("Invalid Leverage Multiplier");

        if (leverageMultiplier > maxLeverage) leverageMultiplier = maxLeverage;

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
            return Mono.error(new RuntimeException("Not enough cash to buy minimum quantity"));
        }

        // 🔐 HARD RISK CHECK
        balanceService.assertSufficientFunds(BigDecimal.valueOf(triggerPrice), quantity);

        // 4. JWT
        String jwtToken = tokenStorageService.getJwtToken();
        if (jwtToken == null) {
            return Mono.error(new RuntimeException("User not logged in. No JWT token found."));
        }

        IOrderRequest buyOrderRequest = orderRequestFactory.createBuyOrder(stockName, symboltoken, quantity, price);

        return brokerApiClient.chartinkPlaceOrder(buyOrderRequest, jwtToken).flatMap(buyResponse -> {

            if (!buyResponse.isStatus()) {
                return Mono.error(new RuntimeException("BUY order failed"));
            }

            String buyOrderId = buyResponse.getData().getOrderid();

            // 🔁 WAIT until BUY is COMPLETE (retry 5 times, 2 sec delay)
            return pollOrderUntilComplete(buyOrderId, jwtToken).flatMap(executedPrice -> {

                double strikePrice = executedPrice;

                double profitTriggerPrice = strikePrice * applicationProperties.getBuyProfitPercentageMultiplier();

                IOrderRequest sellOrder = orderRequestFactory.createSellLimitOrder(stockName, symboltoken, quantity, profitTriggerPrice);

                // 2% SL from executed price
//                                double slTriggerPrice = strikePrice * applicationProperties.getStoplossPercentageMultiplier();

                StopLossPrice slPrice = calculationService.calculateStopLossPrice(BigDecimal.valueOf(triggerPrice), BigDecimal.valueOf(applicationProperties.getTradingStoplossPercent()), BigDecimal.valueOf(applicationProperties.getTradingStoplossBufferPercent()), stockName);

                IOrderRequest sellSlLimitOrder = orderRequestFactory.createStopLossLimitOrder(stockName, symboltoken, quantity, slPrice.triggerPrice().doubleValue(), slPrice.limitPrice().doubleValue(), "SELL");

                Mono<OrderResponse> sellMono = withOrderRetry(brokerApiClient.chartinkPlaceOrder(sellOrder, jwtToken).flatMap(resp -> {
                    if (!resp.isStatus()) {
                        return Mono.error(new RuntimeException("SELL order failed"));
                    }
                    return Mono.just(resp);
                }));

                Mono<OrderResponse> slmMono = withOrderRetry(brokerApiClient.chartinkPlaceOrder(sellSlLimitOrder, jwtToken).flatMap(resp -> {
                    if (!resp.isStatus()) {
                        return Mono.error(new RuntimeException("SLM order failed"));
                    }
                    return Mono.just(resp);
                }).doOnSuccess(slResp -> {
                    slOrderStore.put("NSE:" + stockName + ":" + LocalDate.now(), new SlOrderMeta(buyOrderId, slResp.getData().getOrderid(), quantity, slPrice.triggerPrice().doubleValue(), symboltoken));
                }));

                return Mono.zip(sellMono, slmMono).doOnSuccess(tuple -> {
                    OrderResponse sellResp = tuple.getT1();
                    OrderResponse slResp = tuple.getT2();

                    OrderContext ctx = new OrderContext(buyOrderId, sellResp.getData().getOrderid(), slResp.getData().getOrderid(), stockName, symboltoken, quantity, "NORMAL", "STOPLOSS");

                    orderRegistry.register(ctx);
                }).thenReturn(buyResponse);
            });
        });
    }

    public Mono<Double> pollOrderUntilComplete(String orderId, String jwtToken) {

//       return Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
        return Flux.interval(Duration.ZERO, Duration.ofMillis(applicationProperties.getOrderBookRetryMilliseconds())).flatMap(tick -> getOrderById(orderId, jwtToken).map(this::mapToOrderResult)        // OrderStatusItem → OrderResult
                .defaultIfEmpty(OrderResult.notFound())).filter(OrderResult::isTerminal).map(OrderResult::getResult).next();
    }

    private Mono<OrderStatusItem> getOrderById(String orderId, String jwtToken) {

        return brokerApiClient.getOrderBook(jwtToken).map(OrderBookResponse_v2::getData).flatMapMany(Flux::fromIterable).filter(item -> orderId.equals(item.getOrderid())).next();
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

        Retry retrySpec = Retry.fixedDelay(5, Duration.ofSeconds(3)).filter(ex -> ex instanceof RuntimeException).doBeforeRetry(rs -> log.warn("Retrying operation. Attempt {}", rs.totalRetries() + 1)).onRetryExhaustedThrow((spec, signal) -> signal.failure());

        return mono.retryWhen(retrySpec);
    }

//---------------------------2nd version of buy order ---------------------

    public Mono<OrderResponse> chartinkSimpleBuyOrder(WebhookRequest webhookRequest, String exchange) {

        String stockName = webhookRequest.getStocks().split(",")[0].trim();
        String price = webhookRequest.getTrigger_prices().split(",")[0].trim();
//        double triggerPrice = Utility.roundToTick(Double.parseDouble(price));
        ScripMasterRecord buyRecord = scripMasterService.getNseEquityMap().get(stockName);
        BigDecimal tickSize = buyRecord != null ? buyRecord.getTickSize() : applicationProperties.getDefaultTickSize();
        BigDecimal triggerPrice = Utility.roundToTick(new BigDecimal(price), stockName, tickSize);


        String symbolToken = scripMasterService.getTokenForName(stockName);
        if (symbolToken == null) {
            return Mono.error(new IllegalStateException("Symbol not found: " + stockName));
        }

        BigDecimal usableCash = balanceService.getUsableBalance();

        log.info("Chartink long BUY received | stock={} | triggerPrice={} | usableCash={}", stockName, triggerPrice, usableCash);

        if (usableCash.doubleValue() <= applicationProperties.getBalanceMinimumAllowed()) {

            return Mono.error(new RuntimeException("Insufficient balance: " + usableCash));
        }

        int leverageMultiplier = applicationProperties.getLeverageMultiplierToUseForLong();
        int maxLeverage = (int) leverageService.get(stockName).multiplier();

        if (leverageMultiplier < 1) throw new IllegalArgumentException("Invalid Leverage Multiplier");

        if (leverageMultiplier > maxLeverage) leverageMultiplier = maxLeverage;

        int calculatedQuantity = orderCalculationService.calculateBuyQuantity(usableCash, triggerPrice, leverageMultiplier, maxLeverage);

//        double calculateQuantity = usableCash.doubleValue() / triggerPrice;
//        double quantityAfterLeverage = calculateQuantity * leverageMultiplier;
//        int absQuantity = (int) Math.floor(Math.abs(quantityAfterLeverage));
//        double currentBalance = usableCash.doubleValue() - (triggerPrice * (double) absQuantity / leverageMultiplier);
//        double rmsBalance = usableCash.doubleValue() - (absQuantity * (triggerPrice / maxLeverage));

        int quantityAfterBuyingLess = calculatedQuantity - applicationProperties.getNumberOfStocksBuyLessForLong();

        if (quantityAfterBuyingLess <= 0)
            throw new IllegalStateException("After reducing safety quantity from calculated quantity, the quantity become " + quantityAfterBuyingLess + ", which is invalid");

        if (applicationProperties.isFixedQuantityFlag() && quantityAfterBuyingLess > applicationProperties.getFixedQuantity()) {
//            log.info("Taking fixed quantity of {} from property file an applying leverage as {}", applicationProperties.getFixedQuantity(), leverageMultiplier);
            leverageMultiplier = 1;
            quantityAfterBuyingLess = applicationProperties.getFixedQuantity();
            log.info("Taking fixed quantity: {} | leverage I applied: {} | leverage applied from property file due to fixed quantity: {}", quantityAfterBuyingLess, applicationProperties.getLeverageMultiplierToUseForShort(), leverageMultiplier);
        }

        final int quantity = quantityAfterBuyingLess;

        log.info("long BUY calc | stock={} | leverage={}/{} | qtyCalculated={} | qtyFinal={} | buyLess={} | fixedQtyFlag={}", stockName, leverageMultiplier, maxLeverage, calculatedQuantity, quantity, applicationProperties.getNumberOfStocksBuyLessForLong(), applicationProperties.isFixedQuantityFlag());

//        balanceService.assertSufficientFunds(
//                triggerPrice,
//                quantity / leverageMultiplier
//        );

        balanceService.assertSufficientMargin(triggerPrice, quantity, leverageMultiplier);

        String jwtToken = tokenManager.getValidJwtToken();

        IOrderRequest buyOrder = orderRequestFactory.createBuyOrder(stockName, symbolToken, quantity, triggerPrice.toString());

        log.info("Placing long BUY order | stock={} | price={} | qty={} | marginUsed={} | usableCash={}", stockName, triggerPrice, quantity, triggerPrice.multiply(BigDecimal.valueOf(quantity)).divide(BigDecimal.valueOf(leverageMultiplier)), usableCash);


//        log.info("buy order request: {}", buyOrder);

        return brokerApiClient.chartinkPlaceOrder(buyOrder, jwtToken).doOnSuccess(resp -> {
            String buyOrderId = resp.getData().getOrderid();
            OrderContext ctx = new OrderContext(
//                                    buyOrderId,
                    PositionSide.LONG, stockName, symbolToken, quantity);

            ctx.setBuyOrderId(buyOrderId);
            ctx.setBuyVariety("NORMAL");
            ctx.setExchange(exchange);

            orderRegistry.registerBuy(ctx);

//                    pendingOrderEventStore.replayPendingEvents(buyOrderId);

            Queue<com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse> pendingEvents = pendingOrderEventStore.remove(buyOrderId);

            if (pendingEvents != null) {

                log.info("Replaying {} pending events for orderId={}", pendingEvents.size(), buyOrderId);

                pendingEvents.forEach(orderEventDispatcher::dispatchDirectly);
            }

            log.info("BUY order for long trade is registered in order context: {}", ctx);
        });
    }

    public Mono<OrderResponse> chartinkSimpleSellOrder(WebhookRequest webhookRequest, String exchange) {

        String stockName = webhookRequest.getStocks().split(",")[0].trim();
        String price = webhookRequest.getTrigger_prices().split(",")[0].trim();
        ScripMasterRecord sellRecord = scripMasterService.getNseEquityMap().get(stockName);
        BigDecimal tickSize = sellRecord != null ? sellRecord.getTickSize() : applicationProperties.getDefaultTickSize();
        BigDecimal triggerPrice = Utility.roundToTick(new BigDecimal(price), stockName, tickSize);

//        BigDecimal triggerPrice = Utility.roundToTick(new BigDecimal(price), stockName, scripMasterService.getNseEquityMap().get(stockName).getTickSize());

        String symbolToken = scripMasterService.getTokenForName(stockName);
        if (symbolToken == null) {
            return Mono.error(new IllegalStateException("Symbol not found: " + stockName));
        }

        BigDecimal usableCash = balanceService.getUsableBalance();

        log.info("Chartink short SELL received | stock={} | triggerPrice={} | usableCash={}", stockName, triggerPrice, usableCash);

        if (usableCash.doubleValue() <= applicationProperties.getBalanceMinimumAllowed()) {

            return Mono.error(new RuntimeException("Insufficient balance: " + usableCash));
        }

        int leverageMultiplier = applicationProperties.getLeverageMultiplierToUseForShort();
        int maxLeverage = (int) leverageService.get(stockName).multiplier();

        if (leverageMultiplier < 1) throw new IllegalArgumentException("Invalid Leverage Multiplier");

        if (leverageMultiplier > maxLeverage) leverageMultiplier = maxLeverage;

//        BigDecimal maxBuyPrice = triggerPrice.multiply(BigDecimal.valueOf(applicationProperties.getSellProfitPercentageMultiplier()));

        BigDecimal maxBuyPrice = calculationService.calculateSellStopLossPrice(triggerPrice, stockName);

        int calculatedQuantity = orderCalculationService.calculateSellQuantity(usableCash, maxBuyPrice, leverageMultiplier, maxLeverage);

        int reducedQuantityForSafety = calculatedQuantity - applicationProperties.getNumberOfStocksSellLessForShort();

        if (reducedQuantityForSafety <= 0)
            throw new IllegalStateException("After reducing safety quantity from calculated quantity, the quantity become" + reducedQuantityForSafety + ", which is invalid");

        if (applicationProperties.isFixedQuantityFlag() && reducedQuantityForSafety > applicationProperties.getFixedQuantity()) {
            leverageMultiplier = 1;
            reducedQuantityForSafety = applicationProperties.getFixedQuantity();
            log.info("Taking fixed quantity: {} | leverage I applied: {} | leverage applied from property file due to fixed quantity: {}", reducedQuantityForSafety, applicationProperties.getLeverageMultiplierToUseForShort(), leverageMultiplier);
        }
        final int quantity = reducedQuantityForSafety;

        log.info("short SELL calc | stock={} | leverage={}/{} | qtyCalculated={} | qtyFinal={} | buyLess={} | fixedQtyFlag={}", stockName, leverageMultiplier, maxLeverage, calculatedQuantity, quantity, applicationProperties.getNumberOfStocksSellLessForShort(), applicationProperties.isFixedQuantityFlag());

//        balanceService.assertSufficientFunds(
//                maxBuyPrice,
//                quantity / leverageMultiplier
//        );

        balanceService.assertSufficientMargin(maxBuyPrice, quantity, leverageMultiplier);

        String jwtToken = tokenManager.getValidJwtToken();

        IOrderRequest sellLimitOrder = orderRequestFactory.createSellLimitOrder(stockName, symbolToken, quantity, triggerPrice.doubleValue());

        log.info("Placing short SELL order | stock={} | price={} | qty={} | marginUsed={} | usableCash={}", stockName, triggerPrice, quantity, triggerPrice.multiply(BigDecimal.valueOf(quantity)).divide(BigDecimal.valueOf(leverageMultiplier)), usableCash);

        return brokerApiClient.chartinkPlaceOrder(sellLimitOrder, jwtToken).doOnSuccess(resp -> {
            String sellOrderId = resp.getData().getOrderid();
            OrderContext ctx = new OrderContext(
//                                    sellOrderId,
                    PositionSide.SHORT, stockName, symbolToken, quantity);

            ctx.setSellOrderId(sellOrderId);
            ctx.setSellVariety("NORMAL");
            ctx.setExchange(exchange);

            orderRegistry.registerSell(ctx);

//                    pendingOrderEventStore.replayPendingEvents(sellOrderId);

            Queue<com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse> pendingEvents = pendingOrderEventStore.remove(sellOrderId);

            if (pendingEvents != null) {

                log.info("Replaying {} pending events for orderId={}", pendingEvents.size(), sellOrderId);

                pendingEvents.forEach(orderEventDispatcher::dispatchDirectly);
            }

            log.info("SELL order for short trade is registered in order context: {}", ctx);
        });
    }

// ================================ code for single stock ========================

//    public Mono<OrderResponse> handleAlert(WebhookRequest webhookRequest) {
//
////        String symbol = request.getStock(); // adapt to your field
////        LocalDateTime now = LocalDateTime.now();
//
//        String stockName = webhookRequest.getStocks().split(",")[0].trim();
//        String price = webhookRequest.getTrigger_prices().split(",")[0].trim();
//        BigDecimal tickSize = scripMasterService.getNseEquityMap().get(stockName).getTickSize().divide(BigDecimal.valueOf(100));
//        BigDecimal triggerPrice = Utility.roundToTick(new BigDecimal(price), stockName, tickSize);
//
////        BigDecimal triggerPrice = Utility.roundToTick(new BigDecimal(price), stockName, scripMasterService.getNseEquityMap().get(stockName).getTickSize());
////        LocalDateTime now = LocalDateTime.now();
//
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy h:mm a", Locale.ENGLISH);
//
//        String formatted = LocalDateTime.now().format(formatter);
//
//        LocalDateTime now = LocalDateTime.parse(formatted, formatter);
//
//        FirstAlertData existing = alertStateRegistry.get(stockName);
//
//        // 🟢 FIRST ALERT
//        if (existing == null) {
//            return handleFirstAlert(stockName, now, triggerPrice);
//        }
//
//        // 🔴 SECOND ALERT
//        return handleSecondAlert(stockName, now, existing, webhookRequest);
//    }
//
////    private Mono<OrderResponse> handleFirstAlert(String symbol, LocalDateTime now, BigDecimal triggerPrice) {
////
////        log.info("First Alert for stock: {}, now: {}, triggerPrice: {}", symbol, now, triggerPrice);
////
//////        String symbolToken = scripMasterService.getTokenForName(symbol);
//////
//////        LocalDateTime from = now.minusDays(3).withHour(9).withMinute(15);
//////        LocalDateTime to = now.withHour(15).withMinute(30);
//////
//////        JsonObject requestBody = new JsonObject();
//////        requestBody.addProperty("exchange", "NSE");
//////        requestBody.addProperty("symboltoken", symbolToken);
//////        requestBody.addProperty("interval", "FIVE_MINUTE");
//////        requestBody.addProperty("fromdate", from.format(API_FORMAT));
//////        requestBody.addProperty("todate", to.format(API_FORMAT));
//////
//////        String jwtToken = tokenManager.getValidJwtToken();
////
////        FirstAlertData data = new FirstAlertData(
////                symbol,
////                now,
////                triggerPrice.doubleValue(),
////                0,
////                0,
////                0,
////                AlertState.WAITING_SECOND_ALERT
////        );
////
////        alertStateRegistry.save(symbol, data);
////
//////        ScheduledFuture<?> future = scheduler.schedule(
//////                () -> triggerSyntheticSecondAlert(symbol),
//////                320,
//////                TimeUnit.SECONDS
//////        );
//////
//////        data.setFallbackTask(future);
////
////        return Mono.empty();
////
//////        return fetchWithRetry(requestBody, jwtToken)
//////                .doOnNext(candles -> {
//////
//////                    int index = candles.size() - 1;
//////
//////                    double sma10 = calculateSMA(candles, index, 10);
//////                    double sma20 = calculateSMA(candles, index, 20);
//////
//////                    FirstAlertData data = new FirstAlertData(
//////                            symbol,
//////                            now,
//////                            triggerPrice.doubleValue(),
//////                            sma10,
//////                            sma20
//////                    );
//////
//////                    alertStateRegistry.save(symbol, data);
//////                })
//////                .then(Mono.empty()); // no trade
////    }
//
//
//    private Mono<OrderResponse> handleFirstAlert(String symbol, LocalDateTime now, BigDecimal triggerPrice) {
//
//        log.info("First Alert for stock: {}, now: {}, triggerPrice: {}", symbol, now, triggerPrice);
//
//        String symbolToken = scripMasterService.getTokenForName(symbol);
//
//        String jwtToken = tokenManager.getValidJwtToken();
//
//        // Fetch previous 4 days only
//        return fetchHistoricalCandles(symbolToken, 1, // start from yesterday
//                4, // 4 days
//                jwtToken).flatMap(cachedCandles -> {
//
//            cachedCandles.sort(Comparator.comparing(Candle::getTime));
//
//            FirstAlertData data = new FirstAlertData(symbol, now, triggerPrice.doubleValue(), 0, 0, 0, AlertState.WAITING_SECOND_ALERT, null, cachedCandles, symbolToken);
//
//            alertStateRegistry.save(symbol, data);
//
//            return Mono.empty();
//        });
//    }
//
////    private Mono<OrderResponse> handleSecondAlert(
////            String symbol,
////            LocalDateTime now,
////            FirstAlertData firstAlert,
////            WebhookRequest request
////    ) {
////
////        log.info("Second Alert for stock: {}, now: {}, webhook request: {}", symbol, now, request.toString());
////
////        String symbolToken = scripMasterService.getTokenForName(symbol);
////
//////        LocalDateTime from = now.minusDays(3).withHour(9).withMinute(15);
//////        LocalDateTime to = now.withHour(15).withMinute(30);
//////
//////        JsonObject requestBody = new JsonObject();
//////        requestBody.addProperty("exchange", "NSE");
//////        requestBody.addProperty("symboltoken", symbolToken);
//////        requestBody.addProperty("interval", "FIVE_MINUTE");
//////        requestBody.addProperty("fromdate", from.format(API_FORMAT));
//////        requestBody.addProperty("todate", to.format(API_FORMAT));
////
//////        requestBody.addProperty("fromdate", "2026-01-19 09:15");
//////        requestBody.addProperty("todate", "2026-01-21 09:30");
////
////        String jwtToken = tokenManager.getValidJwtToken();
////
////        // ⛔ Expiry check (5–6 min window)
////        if (Duration.between(firstAlert.getTriggeredAt(), now).toMinutes() > 10) {
////            log.info("Alert details are: {}", alertStateRegistry.get(symbol).toString());
////            alertStateRegistry.remove(symbol);
////            return Mono.empty();
////        }
////
//////        return fetchWithRetry(requestBody, jwtToken)
////        return fetchLastNDaysCandles(symbolToken, applicationProperties.getHistoricDataDays(), jwtToken)
////                .flatMap(candles -> {
////
//////                    log.info("all merged data: {}", candles);
////
//////                    DateTimeFormatter formatter =
//////                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//////
//////                    LocalDateTime customTime = LocalDateTime.parse("2026-01-21 09:30:00", formatter);
////
//////                    int index = candles.size() - 1;
////                    int index = findNearestCandleIndex(candles, now);
////                    if (index < 0) return null;
////                    Candle current = candles.get(index);
////
//////                    log.info("Current candle selected for colour check: {}", current.toString());
////
////                    double sma10 = calculateSMA(candles, index, 10);
////                    double sma20 = calculateSMA(candles, index, 20);
////
////                    alertStateRegistry.get(symbol).setSecondPrice(Double.parseDouble(request.getTrigger_prices()));
////                    alertStateRegistry.get(symbol).setSma10(sma10);
////                    alertStateRegistry.get(symbol).setSma20(sma20);
////
////                    boolean isGreen = current.getClose() > current.getOpen();
////                    boolean isRed = current.getClose() <= current.getOpen();
////
//////                    double sma10 = calculateSMA(candles, index, 10);
////
////                    Mono<OrderResponse> result;
////
////                    log.info("Alert details are: {} | volume: {} | color of the candle: {}",
////                            alertStateRegistry.get(symbol).toString(), current.getVolume(), isGreen ? "Green" : "Red");
////
////                    if (isGreen && current.getVolume() > sma10) {
////                        log.info("Placing a Long order for stock: {}", symbol);
////                        result = chartinkSimpleBuyOrder(request, "NSE");
////                    } else if (isRed && current.getVolume() <= sma10) {
////                        log.info("Placing a Short order for stock: {}", symbol);
////                        result = chartinkSimpleSellOrder(request, "NSE");
////                    } else {
////                        log.info("Not Placing any order for stock: {}", symbol);
////                        result = Mono.empty();
////                    }
////
////                    alertStateRegistry.remove(symbol);
////
////                    return result;
////                });
////    }
//
//
//    private Mono<OrderResponse> handleSecondAlert(String symbol, LocalDateTime now, FirstAlertData firstAlert, WebhookRequest request) {
//
//        log.info("Second Alert for stock: {}, now: {}, webhook request: {}", symbol, now, request.toString());
//
//        String jwtToken = tokenManager.getValidJwtToken();
//
//        // ⛔ Expiry check
//        if (Duration.between(firstAlert.getTriggeredAt(), now).toMinutes() > 10) {
//
//            log.info("Alert details are: {}", alertStateRegistry.get(symbol).toString());
//
//            alertStateRegistry.remove(symbol);
//
//            return Mono.empty();
//        }
//
//        // Fetch ONLY current day
//        return fetchHistoricalCandles(firstAlert.getSymbolToken(), 0, 1, jwtToken).flatMap(todayCandles -> {
//
//            List<Candle> mergedCandles = new ArrayList<>();
//
//            // old cached candles
//            if (firstAlert.getCachedCandles() != null) {
//                mergedCandles.addAll(firstAlert.getCachedCandles());
//            }
//
//            // append today's candles
//            mergedCandles.addAll(todayCandles);
//
//            // final sorting
//            mergedCandles.sort(Comparator.comparing(Candle::getTime));
//
//            int index = findNearestCandleIndex(mergedCandles, now);
//
//            if (index < 0) {
//                return Mono.empty();
//            }
//
//            Candle current = mergedCandles.get(index);
//
//            double sma10 = calculateSMA(mergedCandles, index, 10);
//
//            double sma20 = calculateSMA(mergedCandles, index, 20);
//
//            alertStateRegistry.get(symbol).setSecondPrice(Double.parseDouble(request.getTrigger_prices()));
//
//            alertStateRegistry.get(symbol).setSma10(sma10);
//
//            alertStateRegistry.get(symbol).setSma20(sma20);
//
//            boolean isGreen = current.getClose() > current.getOpen();
//
//            boolean isRed = current.getClose() <= current.getOpen();
//
//            log.info("Alert details are: {} | volume: {} | color of the candle: {}", alertStateRegistry.get(symbol).toString(), current.getVolume(), isGreen ? "Green" : "Red");
//
//            Mono<OrderResponse> result;
//
//            if (isGreen && current.getVolume() > sma10) {
//
//                log.info("Placing Long order for stock: {}", symbol);
//
//                result = chartinkSimpleBuyOrder(request, "NSE");
//
//            } else if (isRed && current.getVolume() <= sma10) {
//
//                log.info("Placing Short order for stock: {}", symbol);
//
//                result = chartinkSimpleSellOrder(request, "NSE");
//
//            } else {
//
//                log.info("Not placing order for stock: {}", symbol);
//
//                result = Mono.empty();
//            }
//
//            alertStateRegistry.remove(symbol);
//
//            return result;
//        });
//    }
//
//    private Mono<List<Candle>> fetchHistoricalCandles(String symbolToken, int startOffset, int totalDays, String jwtToken) {
//
//        List<Candle> merged = new ArrayList<>();
//
//        return Flux.range(startOffset, totalDays)
//
//                .flatMap(dayOffset -> {
//
//                    LocalDate date = LocalDate.now().minusDays(dayOffset);
//
//                    LocalDateTime from = date.atTime(9, 15);
//
//                    LocalDateTime to = date.atTime(15, 30);
//
//                    JsonObject requestBody = new JsonObject();
//
//                    requestBody.addProperty("exchange", "NSE");
//
//                    requestBody.addProperty("symboltoken", symbolToken);
//
//                    requestBody.addProperty("interval", "FIVE_MINUTE");
//
//                    requestBody.addProperty("fromdate", from.format(API_FORMAT));
//
//                    requestBody.addProperty("todate", to.format(API_FORMAT));
//
//                    return fetchWithRetry(requestBody, jwtToken).onErrorResume(e -> {
//
//                        log.error("Failed for date: {} | error: {}", date, e.getMessage());
//
//                        return Mono.just(Collections.emptyList());
//                    });
//
//                }, 3)
//
//                .flatMapIterable(list -> list)
//
//                .collectList()
//
//                .map(candles -> {
//
//                    merged.addAll(candles);
//
//                    merged.sort(Comparator.comparing(Candle::getTime));
//
//                    return merged;
//                });
//    }
//
//    // reuse your logic
//    private double calculateSMA(List<Candle> candles, int endIndex, int period) {
//        if (endIndex < period - 1) return 0;
//
//        log.info("Last candle we picked for SMA{} calculation: {}", period, candles.get(endIndex));
//
//        double sum = 0;
//        for (int i = endIndex - period + 1; i <= endIndex; i++) {
//            sum += candles.get(i).getVolume();
//        }
//        return sum / period;
//    }
//
//    private Mono<List<Candle>> fetchWithRetry(JsonObject requestBody, String jwtToken) {
//
//        return brokerApiClient.getHistoricalCandleData(requestBody, jwtToken).retryWhen(Retry.fixedDelay(3, Duration.ofMillis(1500))).flatMap(response -> {
//
//            if (response == null || !response.isStatus() || response.getData() == null) {
//                return Mono.empty();
//            }
//
//            List<Candle> candles = response.getData().stream().map(this::mapToCandle).sorted(Comparator.comparing(Candle::getTimestamp)).toList();
//
//            return Mono.just(candles);
//        });
//    }
//
//    private Candle mapToCandle(List<Object> row) {
//
//        Candle candle = new Candle();
//
//        candle.setTimestamp((String) row.get(0));
//        candle.setOpen(Double.parseDouble(row.get(1).toString()));
//        candle.setHigh(Double.parseDouble(row.get(2).toString()));
//        candle.setLow(Double.parseDouble(row.get(3).toString()));
//        candle.setClose(Double.parseDouble(row.get(4).toString()));
//        candle.setVolume(Long.parseLong(row.get(5).toString()));
//
//        return candle;
//    }
//
//    private int findNearestCandleIndex(List<Candle> candles, LocalDateTime target) {
//
//        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
//
//        int closestIndex = -1;
//        long minDiff = Long.MAX_VALUE;
//
//        for (int i = 0; i < candles.size(); i++) {
//
//            LocalDateTime candleTime = LocalDateTime.parse(candles.get(i).getTimestamp(), formatter);
//
//            long diff = Math.abs(Duration.between(candleTime, target).toMinutes());
//
//            if (diff < minDiff) {
//                minDiff = diff;
//                closestIndex = i;
//            }
//        }
//
//        return closestIndex - 1;
//    }

//    ================================================================


// ======================== code for multiple stocks =======================

//    public Mono<OrderResponse> handleAlert(WebhookRequest webhookRequest) {
//
//        String[] stocks = webhookRequest.getStocks().split(",");
//
//        String[] prices = webhookRequest.getTrigger_prices().split(",");
//
//        return Flux.range(0, stocks.length)
//
//                .concatMap(index -> {
//
//                    String stockName = stocks[index].trim();
//
//                    String price = prices[index].trim();
//
//                    return handleSingleStockAlert(stockName, price, webhookRequest);
//                })
//
//                // IMPORTANT:
//                // first stock that places order wins
//                .filter(Objects::nonNull)
//
//                .next()
//
//                // if no stock matched
//                .switchIfEmpty(Mono.empty());
//    }

    public Mono<OrderResponse> handleAlert(WebhookRequest webhookRequest) {

        String[] stocks = webhookRequest.getStocks().split(",");

        String[] prices = webhookRequest.getTrigger_prices().split(",");

        // Parse webhook trigger time ONCE
        LocalDateTime triggerTime = normalizeToCandleTime(parseTriggeredAt(webhookRequest.getTriggered_at()));

        return Flux.range(0, stocks.length)

                .concatMap(index -> {

                    String stockName = stocks[index].trim();

                    String price = prices[index].trim();

                    return handleSingleStockAlert(stockName, price, triggerTime, webhookRequest);
                })

                .filter(Objects::nonNull)

                .next()

                .switchIfEmpty(Mono.empty())

                // cancel fallback tasks for any stocks skipped by .next() short-circuit
                .doOnSuccess(response -> {
                    if (response != null) {
                        for (String stock : stocks) {
                            alertStateRegistry.cancelFallbackIfPending(stock.trim());
                        }
                    }
                });
    }

    private Mono<OrderResponse> handleSingleStockAlert(String stockName, String price, LocalDateTime triggerTime, WebhookRequest webhookRequest) {

        ScripMasterRecord scripRecord = scripMasterService.getNseEquityMap().get(stockName);
        BigDecimal tickSize = scripRecord != null ? scripRecord.getTickSize() : applicationProperties.getDefaultTickSize();

        BigDecimal triggerPrice = Utility.roundToTick(new BigDecimal(price), stockName, tickSize);

        // Feature: skip stock if a single share costs more than the available balance
        BigDecimal availableBalance = balanceService.getUsableBalance();
        if (triggerPrice.compareTo(availableBalance) > 0) {
            log.info("Skipping stock: {} | triggerPrice: {} > availableBalance: {} (cannot afford even 1 share)", stockName, triggerPrice, availableBalance);
            return Mono.empty();
        }

//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy h:mm a", Locale.ENGLISH);
//
//        String formatted = LocalDateTime.now().format(formatter);
//
//        LocalDateTime now = LocalDateTime.parse(formatted, formatter);

        LocalDateTime serverNow = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

        FirstAlertData existing = alertStateRegistry.get(stockName);

//        if (existing != null && Duration.between(existing.getTriggeredAt(), serverNow).toMinutes() > 10) {
//
//            log.info("Removing stale alert for stock: {}", stockName);
//
//            alertStateRegistry.remove(stockName);
//
//            existing = null;
//        }

        if (existing == null) {

            return handleFirstAlert(stockName, triggerTime, triggerPrice);
        }

        // atomic claim: only one thread (real alert vs fallback) proceeds to handleSecondAlert
        if (!existing.getAlertState().compareAndSet(AlertState.WAITING_SECOND_ALERT, AlertState.PROCESSING)) {
            log.info("Duplicate 2nd alert ignored for stock: {} | state already: {}", stockName, existing.getAlertState().get());
            return Mono.empty();
        }

        // cancel the LTP fallback task — real second alert arrived first
        if (existing.getFallbackTask() != null && !existing.getFallbackTask().isDone()) {
            boolean cancelled = existing.getFallbackTask().cancel(false);
            log.info("Cancelled fallback LTP task for stock: {} (real 2nd alert received) | cancelled={}", stockName, cancelled);
        }

        // IMPORTANT:
        // pass stock-specific request
        WebhookRequest stockSpecificRequest = cloneRequestForSingleStock(webhookRequest, stockName, price);

        return handleSecondAlert(stockName, serverNow, existing, stockSpecificRequest);
    }

    private WebhookRequest cloneRequestForSingleStock(WebhookRequest original, String stock, String price) {

        WebhookRequest request = new WebhookRequest();

        request.setStocks(stock);

        request.setTrigger_prices(price);

        request.setTriggered_at(original.getTriggered_at());

        request.setScan_name(original.getScan_name());

        request.setScan_url(original.getScan_url());

        request.setAlert_name(original.getAlert_name());

        request.setWebhook_url(original.getWebhook_url());

        return request;
    }

    private Mono<OrderResponse> handleFirstAlert(String symbol, LocalDateTime triggerTime, BigDecimal triggerPrice) {

        log.info("First Alert for stock: {}, now: {}, triggerPrice: {}", symbol, triggerTime, triggerPrice);

        String symbolToken = scripMasterService.getTokenForName(symbol);

        String jwtToken = tokenManager.getValidJwtToken();

        // Fetch previous 4 days only
        return fetchHistoricalCandles(symbolToken, 1, // start from yesterday
                4, // 4 days
                jwtToken).flatMap(cachedCandles -> {

            cachedCandles.sort(Comparator.comparing(Candle::getTime));

            FirstAlertData data = new FirstAlertData(symbol, triggerTime, triggerPrice.doubleValue(), 0, 0, 0, AlertState.WAITING_SECOND_ALERT, null, cachedCandles, symbolToken);

            alertStateRegistry.save(symbol, data);

            if (applicationProperties.isFallbackAlertEnable()) {
                long waitMs = applicationProperties.getFallbackAlertWaitTimeMs();
                ScheduledFuture<?> future = scheduler.schedule(
                        () -> triggerLtpFallback(symbol, data),
                        waitMs,
                        TimeUnit.MILLISECONDS
                );
                data.setFallbackTask(future);
                log.info("Fallback LTP task scheduled for stock: {} | waitMs: {}", symbol, waitMs);
            }

            return Mono.empty();
        });
    }

    private Mono<OrderResponse> handleSecondAlert(String symbol, LocalDateTime now, FirstAlertData firstAlert, WebhookRequest request) {

        log.info("Second Alert for stock: {}, now: {}, webhook request: {}", symbol, now, request.toString());

        String jwtToken = tokenManager.getValidJwtToken();

//        long minutesDiff = Duration.between(firstAlert.getTriggeredAt(), now).toMinutes();
//
////         ⛔ Expiry check
//        if (minutesDiff > 10) {
//
////            log.info("Alert details are: {}", alertStateRegistry.get(symbol).toString());
//            log.info("Trade opportunity expired for stock: {} | minutesDiff: {}", symbol, minutesDiff);
//
//            alertStateRegistry.remove(symbol);
//
//            return Mono.empty();
//        }

        // Fetch ONLY current day
        return fetchHistoricalCandles(firstAlert.getSymbolToken(), 0, 1, jwtToken).flatMap(todayCandles -> {

            List<Candle> mergedCandles = new ArrayList<>();

            // old cached candles
            if (firstAlert.getCachedCandles() != null) {
                mergedCandles.addAll(firstAlert.getCachedCandles());
            }

            // append today's candles
            mergedCandles.addAll(todayCandles);

            // final sorting
            mergedCandles.sort(Comparator.comparing(Candle::getTime));

//            int index = findNearestCandleIndex(mergedCandles, now);
            int index = findExactCandleIndex(mergedCandles, firstAlert.getTriggeredAt());

            if (index < 0) {

                log.warn("Exact candle not found for stock: {}. Falling back to nearest candle.", symbol);

                index = findNearestCandleIndex(mergedCandles, firstAlert.getTriggeredAt());
            }

            if (index < 0) {

                log.warn("No candle found for stock: {}", symbol);

                return Mono.empty();
            }

            Candle current = mergedCandles.get(index);

            double sma10 = calculateSMA(mergedCandles, index, 10);

            double sma20 = calculateSMA(mergedCandles, index, 20);

            alertStateRegistry.get(symbol).setSecondPrice(Double.parseDouble(request.getTrigger_prices()));

            alertStateRegistry.get(symbol).setSma10(sma10);

            alertStateRegistry.get(symbol).setSma20(sma20);

            boolean isGreen = current.getClose() > current.getOpen();

            boolean isRed = current.getClose() <= current.getOpen();

            log.info("Alert details are: {} | volume: {} | candleTime: {} | candleColor: {}", alertStateRegistry.get(symbol), current.getVolume(), current.getTimestamp(), isGreen ? "Green" : "Red");

            Mono<OrderResponse> result;

            if (isGreen && current.getVolume() > sma10) {

                log.info("Placing Long order for stock: {}", symbol);

                result = chartinkSimpleBuyOrder(request, "NSE");

            } else if (isRed && current.getVolume() <= sma10) {

                log.info("Placing Short order for stock: {}", symbol);

                result = chartinkSimpleSellOrder(request, "NSE");

            } else {

                log.info("Not placing order for stock: {}", symbol);

                result = Mono.empty();
            }

//            alertStateRegistry.remove(symbol);
//
//            return result;
            return result.doFinally(signal -> {

                log.info("Cleaning registry for stock: {}", symbol);

                alertStateRegistry.remove(symbol);
            });
        });
    }

    private void triggerLtpFallback(String symbol, FirstAlertData data) {

        log.info("Fallback triggered for stock: {}", symbol);

        Mono<BigDecimal> ltpMono;

        if (applicationProperties.isFallbackLtpMockEnable()) {
            BigDecimal mockPrice = BigDecimal.valueOf(applicationProperties.getFallbackLtpMockPrice());
            log.info("Using mock LTP for stock: {} | mockPrice: {}", symbol, mockPrice);
            ltpMono = Mono.just(mockPrice);
        } else {
            ltpMono = marketDataService.getLastTradedPrice("NSE", data.getSymbolToken(), applicationProperties.getExitLtpMode());
        }

        ltpMono.flatMap(ltp -> {

            log.info("LTP fetched for stock: {} | ltp: {}", symbol, ltp);

            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

            WebhookRequest syntheticRequest = new WebhookRequest();
            syntheticRequest.setStocks(symbol);
            syntheticRequest.setTrigger_prices(ltp.toPlainString());
            syntheticRequest.setTriggered_at(now.format(DateTimeFormatter.ofPattern("hh:mm a")));
            syntheticRequest.setScan_name("Fallback Alert");
            syntheticRequest.setScan_url("fallback-alert");
            syntheticRequest.setAlert_name("Fallback Alert for " + symbol);
            syntheticRequest.setWebhook_url("");

            return handleAlert(syntheticRequest);

        }).subscribe(
            result -> log.info("Fallback order placed for stock: {} | result: {}", symbol, result),
            error  -> log.error("Fallback processing failed for stock: {} | error: {}", symbol, error.getMessage())
        );
    }

    private Mono<List<Candle>> fetchHistoricalCandles(String symbolToken, int startOffset, int totalDays, String jwtToken) {

        List<Candle> merged = new ArrayList<>();

        return Flux.range(startOffset, totalDays)

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

                    return fetchWithRetry(requestBody, jwtToken).onErrorResume(e -> {

                        log.error("Failed for date: {} | error: {}", date, e.getMessage());

                        return Mono.just(Collections.emptyList());
                    });

                }, 3)

                .flatMapIterable(list -> list)

                .collectList()

                .map(candles -> {

                    merged.addAll(candles);

                    merged.sort(Comparator.comparing(Candle::getTime));

                    return merged;
                });
    }

    // reuse your logic
    private double calculateSMA(List<Candle> candles, int endIndex, int period) {
        if (endIndex < period - 1) return 0;

        log.info("Last candle we picked for SMA{} calculation: {}", period, candles.get(endIndex));

        double sum = 0;
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            sum += candles.get(i).getVolume();
        }
        return sum / period;
    }

    private Mono<List<Candle>> fetchWithRetry(JsonObject requestBody, String jwtToken) {

        return brokerApiClient.getHistoricalCandleData(requestBody, jwtToken).retryWhen(Retry.fixedDelay(3, Duration.ofMillis(1500))).flatMap(response -> {

            if (response == null || !response.isStatus() || response.getData() == null) {
                return Mono.empty();
            }

            List<Candle> candles = response.getData().stream().map(this::mapToCandle).sorted(Comparator.comparing(Candle::getTimestamp)).toList();

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

    private int findExactCandleIndex(List<Candle> candles, LocalDateTime target) {

        for (int i = 0; i < candles.size(); i++) {

            LocalDateTime candleTime = OffsetDateTime.parse(candles.get(i).getTimestamp()).toLocalDateTime();

            if (candleTime.equals(target)) {

                log.info("Exact candle matched at index: {} | candleTime: {}", i, candleTime);

                return i;
            }
        }

        return -1;
    }

//    private int findNearestCandleIndex(List<Candle> candles, LocalDateTime target) {
//
//        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
//
//        int closestIndex = -1;
//        long minDiff = Long.MAX_VALUE;
//
//        for (int i = 0; i < candles.size(); i++) {
//
//            LocalDateTime candleTime = LocalDateTime.parse(candles.get(i).getTimestamp(), formatter);
//
//            long diff = Math.abs(Duration.between(candleTime, target).toMinutes());
//
//            if (diff < minDiff) {
//                minDiff = diff;
//                closestIndex = i;
//            }
//        }
//
//        return closestIndex - 1;
//    }

    private int findNearestCandleIndex(List<Candle> candles, LocalDateTime target) {

        int closestIndex = -1;

        long minDiff = Long.MAX_VALUE;

        for (int i = 0; i < candles.size(); i++) {

            LocalDateTime candleTime = OffsetDateTime.parse(candles.get(i).getTimestamp()).toLocalDateTime();

            long diff = Math.abs(Duration.between(candleTime, target).toMinutes());

            if (diff < minDiff) {

                minDiff = diff;

                closestIndex = i;
            }
        }

        if (closestIndex >= 0) {

            log.info("Nearest candle matched at index: {} | candleTime: {}", closestIndex, candles.get(closestIndex).getTimestamp());
        }

        // IMPORTANT
        // removed dangerous "-1"
        return closestIndex;
    }


// =========================================================================

//    private Mono<List<Candle>> fetchLastNDaysCandles(
//            String symbolToken,
//            int days,
//            String jwtToken
//    ) {
//
//        List<Candle> merged = new ArrayList<>();
//
//        return Flux.range(0, days)
//                .flatMap(dayOffset -> {
//
//                    LocalDate date = LocalDate.now().minusDays(dayOffset);
//
//                    LocalDateTime from = date.atTime(9, 15);
//                    LocalDateTime to = date.atTime(15, 30);
//
//                    JsonObject requestBody = new JsonObject();
//                    requestBody.addProperty("exchange", "NSE");
//                    requestBody.addProperty("symboltoken", symbolToken);
//                    requestBody.addProperty("interval", "FIVE_MINUTE");
//                    requestBody.addProperty("fromdate", from.format(API_FORMAT));
//                    requestBody.addProperty("todate", to.format(API_FORMAT));
//
//                    return fetchWithRetry(requestBody, jwtToken)
////                            .doOnSuccess(list ->
////                                    log.info("Success for: {} -> candles: {}", date, list.size())
////                            )
//                            .onErrorResume(e -> {
//                                log.error("Failed for date: {} | error: {}", date, e.getMessage());
//                                return Mono.just(Collections.emptyList());
//                            });
//
//                }, 3) // ⚠️ concurrency = 3 (API limit ~3 req/sec)
//                .flatMapIterable(list -> list)
//                .collectList()
//                .map(candles -> {
//
//                    // ✅ Remove duplicates (by timestamp)
////                    Map<LocalDateTime, Candle> map = new HashMap<>();
////                    for (Candle c : candles) {
////                        map.put(c.getTime(), c);
////                    }
//
////                    List<Candle> merged = new ArrayList<>(map.values());
//
//                    merged.addAll(candles);
//
//                    // ✅ Sort ascending
//                    merged.sort(Comparator.comparing(Candle::getTime));
//
//                    return merged;
//                });
//    }

//    private void triggerSyntheticSecondAlert(String symbol) {
//
//        FirstAlertData data =
//                alertStateRegistry.get(symbol);
//
//        if (data == null) {
//            return;
//        }
//
//        synchronized (data) {
//
//            if (data.getAlertState() != AlertState.WAITING_SECOND_ALERT) {
//                return;
//            }
//
//            data.setAlertState(AlertState.PROCESSING);
//        }
//
//        log.info(
//                "Synthetic second alert triggered for symbol: {}",
//                symbol
//        );
//
//        fetchLatestCompletedCandle(symbol)
//                .flatMap(candle -> {
//
//                    WebhookRequest synthetic =
//                            new WebhookRequest();
//
//                    synthetic.setStocks(symbol);
//
//                    synthetic.setTrigger_prices(
//                            String.valueOf(candle.getClose())
//                    );
//
//                    synthetic.setTriggered_at(
//                            LocalTime.now().toString()
//                    );
//
//                    synthetic.setAlert_name(
//                            "Synthetic Alert"
//                    );
//
//                    synthetic.setScan_name(
//                            "Synthetic Scan"
//                    );
//
//                    synthetic.setScan_url(
//                            "synthetic"
//                    );
//
//                    synthetic.setWebhook_url(
//                            "internal"
//                    );
//
//                    return handleSecondAlert(
//                            symbol,
//                            LocalDateTime.now(),
//                            data,
//                            synthetic
//                    );
//                })
//                .doOnError(error ->
//                        log.error(
//                                "Synthetic alert failed for symbol: {} | error: {}",
//                                symbol,
//                                error.getMessage()
//                        )
//                )
//                .subscribe();
//    }

//    private Mono<Candle> fetchLatestCompletedCandle(
//            String symbol
//    ) {
//
//        String token =
//                scripMasterService.getTokenForName(symbol);
//
//        String jwt =
//                tokenManager.getValidJwtToken();
//
//        return fetchLastNDaysCandles(token, 1, jwt)
//                .flatMap(candles -> {
//
//                    if (candles == null || candles.isEmpty()) {
//                        return Mono.empty();
//                    }
//
//                    Candle latest =
//                            candles.get(candles.size() - 1);
//
//                    return Mono.just(latest);
//                });
//    }


//// ==================================================================
//// handleAlert()
//// ==================================================================
//
//    public Mono<OrderResponse> handleAlert(WebhookRequest webhookRequest) {
//
//        String stockName = webhookRequest.getStocks().split(",")[0].trim();
//
//        String price = webhookRequest.getTrigger_prices().split(",")[0].trim();
//
//        BigDecimal tickSize = scripMasterService.getNseEquityMap().get(stockName).getTickSize().divide(BigDecimal.valueOf(100));
//
//        BigDecimal triggerPrice = Utility.roundToTick(new BigDecimal(price), stockName, tickSize);
//
//        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
//
//        FirstAlertData existing = alertStateRegistry.get(stockName);
//
//        // FIRST ALERT
//        if (existing == null) {
//
//            return handleFirstAlert(stockName, now, triggerPrice);
//        }
//
//        // SECOND ALERT
//        return handleSecondAlert(stockName, now, existing, webhookRequest);
//    }
//
//    // ===============================
//// handleFirstAlert()
//// ===============================
//
//    private Mono<OrderResponse> handleFirstAlert(String symbol, LocalDateTime now, BigDecimal triggerPrice) {
//
//        log.info("First Alert for stock: {}, now: {}, triggerPrice: {}", symbol, now, triggerPrice);
//
//        String symbolToken = scripMasterService.getTokenForName(symbol);
//
//        String jwtToken = tokenManager.getValidJwtToken();
//
//        // FETCH ALL REQUIRED DATA DURING FIRST ALERT
//        return fetchLastNDaysCandles(symbolToken, applicationProperties.getHistoricDataDays(), jwtToken).flatMap(candles -> {
//
//            if (candles == null || candles.isEmpty()) {
//
//                log.warn("No candles received during first alert preload for symbol: {}", symbol);
//
//                return Mono.empty();
//            }
//
//            int index = findLatestCompletedCandleIndex(candles, now);
//
//            if (index < 0) {
//
//                log.warn("No valid candle index found during first alert for symbol: {}", symbol);
//
//                return Mono.empty();
//            }
//
//            double sma10 = calculateSMA(candles, index, 10);
//
//            double sma20 = calculateSMA(candles, index, 20);
//
//            FirstAlertData data = new FirstAlertData();
//
//            data.setSymbol(symbol);
//
//            data.setTriggeredAt(now);
//
//            data.setFirstPrice(triggerPrice.doubleValue());
//
//            data.setSecondPrice(0);
//
//            data.setSma10(sma10);
//
//            data.setSma20(sma20);
//
//            data.setAlertState(AlertState.WAITING_SECOND_ALERT);
//
//            data.setCachedCandles(candles);
//
//            data.setSymbolToken(symbolToken);
//
//            alertStateRegistry.save(symbol, data);
//
//            log.info("Cached {} candles for symbol: {} during first alert", candles.size(), symbol);
//
//            return Mono.empty();
//        });
//    }
//
//    // ===============================
//// handleSecondAlert()
//// ===============================
//
//    private Mono<OrderResponse> handleSecondAlert(String symbol, LocalDateTime now, FirstAlertData firstAlert, WebhookRequest request) {
//
//        log.info("Second Alert for stock: {}, now: {}, webhook request: {}", symbol, now, request);
//
//        // EXPIRE OLD ALERTS
//        if (Duration.between(firstAlert.getTriggeredAt(), now).toMinutes() > 10) {
//
//            log.info("Alert expired for symbol: {}", symbol);
//
//            alertStateRegistry.remove(symbol);
//
//            return Mono.empty();
//        }
//
//        // USE CACHED CANDLES
//        List<Candle> candles = new ArrayList<>(firstAlert.getCachedCandles());
//
//        if (candles.isEmpty()) {
//
//            log.warn("Cached candles missing for symbol: {}", symbol);
//
//            alertStateRegistry.remove(symbol);
//
//            return Mono.empty();
//        }
//
//        // FETCH ONLY LATEST CANDLE
//        return fetchLatestCompletedCandle(symbol)
//
//                .flatMap(latestCandle -> {
//
//                    if (latestCandle == null) {
//
//                        log.warn(
//                                "Latest candle is null for symbol: {}",
//                                symbol
//                        );
//
//                        return Mono.empty();
//                    }
//
//                    // REMOVE DUPLICATE CANDLE IF SAME TIMESTAMP EXISTS
//                    candles.removeIf(c -> c.getTimestamp().equals(latestCandle.getTimestamp()));
//
//                    candles.add(latestCandle);
//
//                    candles.sort(Comparator.comparing(Candle::getTimestamp));
//
//                    int latestIndex = candles.size() - 1;
//
//                    double sma10 = calculateSMA(candles, latestIndex, 10);
//
//                    double sma20 = calculateSMA(candles, latestIndex, 20);
//
//                    firstAlert.setSecondPrice(Double.parseDouble(request.getTrigger_prices()));
//
//                    firstAlert.setSma10(sma10);
//
//                    firstAlert.setSma20(sma20);
//
//                    boolean isGreen = latestCandle.getClose() > latestCandle.getOpen();
//
//                    boolean isRed = latestCandle.getClose() <= latestCandle.getOpen();
//
//                    log.info("Alert details are: {} | volume: {} | candleColor: {}", firstAlert, latestCandle.getVolume(), isGreen ? "Green" : "Red");
//
//                    Mono<OrderResponse> result;
//
//                    // BUY CONDITION
//                    if (isGreen && latestCandle.getVolume() > sma10) {
//
//                        log.info("Placing LONG order for stock: {}", symbol);
//
//                        result = chartinkSimpleBuyOrder(request, "NSE");
//                    }
//
//                    // SELL CONDITION
//                    else if (isRed && latestCandle.getVolume() <= sma10) {
//
//                        log.info("Placing SHORT order for stock: {}", symbol);
//
//                        result = chartinkSimpleSellOrder(request, "NSE");
//                    } else {
//
//                        log.info("No trade condition matched for stock: {}", symbol);
//
//                        result = Mono.empty();
//                    }
//
//                    alertStateRegistry.remove(symbol);
//
//                    return result;
//                });
//    }
//
//    // ===============================
//// findLatestCompletedCandleIndex()
//// ===============================
//
//    private int findLatestCompletedCandleIndex(List<Candle> candles, LocalDateTime target) {
//
//        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
//
//        int latestIndex = -1;
//
//        for (int i = 0; i < candles.size(); i++) {
//
//            LocalDateTime candleTime = OffsetDateTime.parse(candles.get(i).getTimestamp(), formatter).toLocalDateTime();
//
//            // STRICTLY BEFORE CURRENT TIME
//            if (!candleTime.isAfter(target)) {
//
//                latestIndex = i;
//            }
//        }
//
//        return latestIndex;
//    }
//
//    // ===============================
//// fetchLatestCompletedCandle()
//// ===============================
//
////    private Mono<Candle> fetchLatestCompletedCandle(String symbol) {
////
////        String token = scripMasterService.getTokenForName(symbol);
////
////        String jwt = tokenManager.getValidJwtToken();
////
////        JsonObject requestBody = new JsonObject();
////
////        LocalDateTime now = LocalDateTime.now();
////
////        LocalDateTime from = now.minusHours(2);
////
////        requestBody.addProperty("exchange", "NSE");
////
////        requestBody.addProperty("symboltoken", token);
////
////        requestBody.addProperty("interval", "FIVE_MINUTE");
////
////        requestBody.addProperty("fromdate", from.format(API_FORMAT));
////
////        requestBody.addProperty("todate", now.format(API_FORMAT));
////
////        return fetchWithRetry(requestBody, jwt).flatMap(candles -> {
////
////            if (candles == null || candles.isEmpty()) {
////
////                return Mono.empty();
////            }
////
////            candles.sort(Comparator.comparing(Candle::getTimestamp));
////
////            // LAST FULLY COMPLETED CANDLE
////            Candle latest = candles.get(candles.size() - 2);
////
////            return Mono.just(latest);
////        });
////    }
//
//    private Mono<Candle> fetchLatestCompletedCandle(String symbol) {
//
//        String token = scripMasterService.getTokenForName(symbol);
//
//        String jwt = tokenManager.getValidJwtToken();
//
//        JsonObject requestBody = new JsonObject();
//
//        LocalDateTime effectiveNow = getEffectiveNow();
//
//        LocalDateTime from = effectiveNow.minusHours(2);
//
//        requestBody.addProperty("exchange", "NSE");
//
//        requestBody.addProperty("symboltoken", token);
//
//        requestBody.addProperty("interval", "FIVE_MINUTE");
//
//        requestBody.addProperty("fromdate", from.format(API_FORMAT));
//
//        requestBody.addProperty("todate", effectiveNow.format(API_FORMAT));
//
//        return fetchWithRetry(requestBody, jwt)
//
//                .flatMap(candles -> {
//
//                    if (candles == null || candles.isEmpty()) {
//
//                        return Mono.empty();
//                    }
//
////                    candles.sort(Comparator.comparing(Candle::getTimestamp));
//
//                    // CURRENT TIME
//                    LocalDateTime now = LocalDateTime.now();
//
//                    // TARGET COMPLETED CANDLE
//                    LocalDateTime targetCandleTime =
//                            getLatestCompleted5MinCandleTime(now);
//
//                    Candle latestCompleted = findCandleByTime(
//                            candles,
//                            targetCandleTime
//                    );
//
//                    if (latestCompleted == null) {
//
//                        log.warn(
//                                "No completed candle found for time: {}",
//                                targetCandleTime
//                        );
//
//                        return Mono.empty();
//                    }
//
//                    return Mono.just(latestCompleted);
//                });
//    }
//
//    // ===============================
//// OPTIONAL OPTIMIZATION
//// KEEP ONLY LAST 25 CANDLES
//// ===============================
//
//    private List<Candle> trimCandles(List<Candle> candles) {
//
//        if (candles.size() <= 25) {
//            return candles;
//        }
//
//        return candles.subList(candles.size() - 25, candles.size());
//    }
//
//    private LocalDateTime getEffectiveNow() {
//
//        LocalDateTime now = LocalDateTime.now();
//
//        // MARKET START
//        LocalTime marketStart = LocalTime.of(9, 15);
//
//        // MARKET END
//        LocalTime marketEnd = LocalTime.of(15, 30);
//
//        DayOfWeek day = now.getDayOfWeek();
//
//        // SATURDAY -> FRIDAY 15:30
//        if (day == DayOfWeek.SATURDAY) {
//
//            return now.minusDays(1)
//                    .withHour(15)
//                    .withMinute(30)
//                    .withSecond(0)
//                    .withNano(0);
//        }
//
//        // SUNDAY -> FRIDAY 15:30
//        if (day == DayOfWeek.SUNDAY) {
//
//            return now.minusDays(2)
//                    .withHour(15)
//                    .withMinute(30)
//                    .withSecond(0)
//                    .withNano(0);
//        }
//
//        // BEFORE MARKET OPEN
//        if (now.toLocalTime().isBefore(marketStart)) {
//
//            return now.minusDays(1)
//                    .withHour(15)
//                    .withMinute(30)
//                    .withSecond(0)
//                    .withNano(0);
//        }
//
//        // AFTER MARKET CLOSE
//        if (now.toLocalTime().isAfter(marketEnd)) {
//
//            return now.withHour(15)
//                    .withMinute(30)
//                    .withSecond(0)
//                    .withNano(0);
//        }
//
//        return now;
//    }
//
//    private LocalDateTime getLatestCompleted5MinCandleTime(LocalDateTime now) {
//
//        int minute = now.getMinute();
//
//        int roundedMinute = (minute / 5) * 5;
//
//        return now.withMinute(roundedMinute)
//                .withSecond(0)
//                .withNano(0)
//                .minusMinutes(5);
//    }
//
//    private Candle findCandleByTime(
//            List<Candle> candles,
//            LocalDateTime target
//    ) {
//
//        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
//
//        return candles.stream()
//
//                .filter(candle -> {
//
//                    LocalDateTime candleTime = OffsetDateTime
//                            .parse(candle.getTimestamp(), formatter)
//                            .toLocalDateTime();
//
//                    return candleTime.equals(target);
//                })
//
//                .findFirst()
//
//                .orElse(null);
//    }
}