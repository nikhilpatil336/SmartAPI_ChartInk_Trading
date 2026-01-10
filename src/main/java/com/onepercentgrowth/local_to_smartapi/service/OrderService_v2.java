package com.onepercentgrowth.local_to_smartapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.eventhandling.OrderEventQueue;
import com.onepercentgrowth.local_to_smartapi.factory.OrderRequestFactory;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.model.*;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import com.onepercentgrowth.local_to_smartapi.model.chartink_request.ChartInkMISBuyOrderRequest;
import com.onepercentgrowth.local_to_smartapi.model.chartink_request.ChartinkMISSellOrderRequest;
import com.onepercentgrowth.local_to_smartapi.model.chartink_request.ChartinkMIS_SL_OrderRequest;
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
import tools.jackson.databind.JsonNode;

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

        int quantity =
                (int) Math.floor(
                        usableCash.doubleValue() / triggerPrice
                ) - applicationProperties.getNumberOfStocksBuyLess();

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

                                double profitTriggerPrice = strikePrice * applicationProperties.getProfitPercentageMultiplier();

                                IOrderRequest sellOrder =
                                        orderRequestFactory.createSellOrder(
                                                stockName,
                                                symboltoken,
                                                quantity,
                                                profitTriggerPrice
                                        );

                                // 2% SL from executed price
                                double slTriggerPrice = strikePrice * applicationProperties.getStoplossPercentageMultiplier();

                                IOrderRequest sellSlmOrder =
                                        orderRequestFactory.createStopLossOrder(
                                                stockName,
                                                symboltoken,
                                                quantity,
                                                slTriggerPrice
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
                                                brokerApiClient.chartinkPlaceOrder(sellSlmOrder, jwtToken)
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
                                                                            slTriggerPrice,
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

    public Mono<OrderResponse> chartinkSimpleBuyOrder(WebhookRequest webhookRequest) {

        String stockName = webhookRequest.getStocks().split(",")[0].trim();
        String price = webhookRequest.getTrigger_prices().split(",")[0].trim();
        double triggerPrice = Double.parseDouble(price);

        String symbolToken = scripMasterService.getTokenForName(stockName);
        if (symbolToken == null) {
            return Mono.error(
                    new IllegalStateException("Symbol not found: " + stockName)
            );
        }

//        RmsData rmsData = scripMasterService.getRmsData();
//        if (rmsData == null) {
//            return Mono.error(
//                    new IllegalStateException("RMS not available")
//            );
//        }

        BigDecimal usableCash =
                balanceService.getUsableBalance(
                        applicationProperties.getPercentBalanceUse()
                );

        int calculatedQuantity =
                orderCalculationService.calculateQuantity(
                        usableCash.doubleValue(),
                        triggerPrice
                );

//        double usableCash =
//                Double.parseDouble(rmsData.getAvailablecash())
//                        * applicationProperties.getPercentBalanceUse();
//
//        int calculatedQuantity =
//                orderCalculationService.calculateQuantity(
//                        usableCash,
//                        triggerPrice
//                );

        if(applicationProperties.isFixedQuantityFlag() && calculatedQuantity  > applicationProperties.getFixedQuantity()) {
            log.info("Taking fixed quantity from property file");
            calculatedQuantity = applicationProperties.getFixedQuantity();
        }
        final int quantity = calculatedQuantity;

        balanceService.assertSufficientFunds(
                BigDecimal.valueOf(triggerPrice),
                quantity
        );

        String jwtToken = tokenManager.getValidJwtToken();

        IOrderRequest buyOrder =
                orderRequestFactory.createBuyOrder(
                        stockName,
                        symbolToken,
                        quantity,
                        price
                );

//        log.info("buy order request: {}", buyOrder);

        return brokerApiClient.chartinkPlaceOrder(buyOrder, jwtToken)
                .doOnSuccess(resp -> {

//                    ObjectMapper mapper = new ObjectMapper();
//
//                    /* Root JSON */
//                    ObjectNode rootNode = mapper.createObjectNode();
//
//// map from OrderResponse
//                    rootNode.put("user-id", "Your_client_code");
//                    rootNode.put("status-code", resp.isStatus() ? "200" : "500");
//                    rootNode.put("order-status", resp.isStatus() ? "SUCCESS" : "FAILED");
//                    rootNode.put("error-message", resp.getMessage() != null ? resp.getMessage() : "");
//
//                    /* orderData JSON */
//                    ObjectNode orderData = mapper.createObjectNode();
//
//// values coming from OrderResponse
//                    orderData.put("orderid", resp.getData() != null ? resp.getData().getOrderid() : "");
//                    orderData.put("text", resp.getMessage() != null ? resp.getMessage() : "");
//
//// static / default values (because OrderResponse does NOT have them)
//                    orderData.put("variety", "NORMAL");
//                    orderData.put("ordertype", "LIMIT");
//                    orderData.put("producttype", "DELIVERY");
//                    orderData.put("transactiontype", "BUY");
//                    orderData.put("exchange", "NSE");
//                    orderData.put("duration", "DAY");
//
//// numeric defaults
//                    orderData.put("price", webhookRequest.getTrigger_prices());
//                    orderData.put("triggerprice", webhookRequest.getTrigger_prices());
//                    orderData.put("squareoff", 0);
//                    orderData.put("stoploss", 0);
//                    orderData.put("trailingstoploss", 0);
//                    orderData.put("averageprice", webhookRequest.getTrigger_prices());
//                    orderData.put("strikeprice", -1);
//
//// empty fields
//                    orderData.put("quantity", quantity);
//                    orderData.put("disclosedquantity", "0");
//                    orderData.put("tradingsymbol", "");
//                    orderData.put("symboltoken", "");
//                    orderData.put("instrumenttype", "");
//                    orderData.put("optiontype", "");
//                    orderData.put("expirydate", "");
//                    orderData.put("lotsize", "0");
//                    orderData.put("cancelsize", "0");
//                    orderData.put("filledshares", "0");
//                    orderData.put("unfilledshares", "0");
////                    orderData.put("status", resp.isStatus() ? "FILLED" : "REJECTED");
////                    orderData.put("orderstatus", resp.isStatus() ? "FILLED" : "REJECTED");
//                    orderData.put("status", "COMPLETE");
//                    orderData.put("orderstatus", "COMPLETE");
//                    orderData.put("updatetime", "");
//                    orderData.put("exchtime", "");
//                    orderData.put("exchorderupdatetime", "");
//                    orderData.put("fillid", "");
//                    orderData.put("filltime", "");
//                    orderData.put("parentorderid", "");
//
//                    /* attach orderData */
//                    rootNode.set("orderData", orderData);
//
//                    /* convert to JSON string */
//                    try {
//                        String requestJson = mapper.writeValueAsString(rootNode);
//                    } catch (JsonProcessingException e) {
//                        throw new RuntimeException(e);
//                    }

                    String buyOrderId = resp.getData().getOrderid();

                    OrderContext ctx =
                            new OrderContext(
                                    buyOrderId,
                                    stockName,
                                    symbolToken,
                                    quantity
                            );

                    orderRegistry.registerBuy(ctx);

                    log.info("Buy order registered in order registry: {}", ctx);

//                    try {
//                        OrderStatusResponse orderStatusResponse =
//                                mapper.treeToValue(rootNode, OrderStatusResponse.class);
//
//                        orderEventQueue.publish(orderStatusResponse);
//                    } catch (JsonProcessingException e) {
//                        throw new RuntimeException(e);
//                    }
                });
    }


}
