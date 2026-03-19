package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.enums.PositionSide;
import com.onepercentgrowth.local_to_smartapi.enums.TradingExchange;
import com.onepercentgrowth.local_to_smartapi.eventhandling.OrderEventQueue;
import com.onepercentgrowth.local_to_smartapi.factory.OrderRequestFactory;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.model.*;
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
                                                slPrice.limitPrice().doubleValue()
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
            log.info("Taking fixed quantity from property file");
            leverageMultiplier = 1;
            quantityAfterBuyingLess = applicationProperties.getFixedQuantity();
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

                    log.info("long BUY order registered in order registry: {}", ctx);
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
            log.info("Taking fixed quantity from property file");
            leverageMultiplier = 1;
            reducedQuantityForSafety = applicationProperties.getFixedQuantity();
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

                    log.info("short SELL order registered in order registry: {}", ctx);
                });
    }
}
