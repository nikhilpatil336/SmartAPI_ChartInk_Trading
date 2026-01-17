package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.factory.OrderRequestFactory;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.model.*;
import com.onepercentgrowth.local_to_smartapi.model.chartink_request.ChartInkMISBuyOrderRequest;
import com.onepercentgrowth.local_to_smartapi.model.chartink_request.ChartinkMISSellOrderRequest;
import com.onepercentgrowth.local_to_smartapi.model.chartink_request.ChartinkMIS_SL_Market_OrderRequest;
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

import java.time.Duration;
import java.time.LocalDate;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

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
    private RmsService rmsService;


//    public OrderService(BrokerApiClient brokerApiClient,
//                        TokenStorageService tokenStorageService,
//                        ScripMasterService scripMasterService) {
//        this.brokerApiClient = brokerApiClient;
//        this.tokenStorageService = tokenStorageService;
//        this.scripMasterService = scripMasterService;
//    }

//    public Mono<OrderResponse> placeOrder(String symbol, int quantity, String transactionType, String token) {
//
//        // Build order request object
//        OrderRequest orderRequest = new OrderRequest();
//        orderRequest.setExchange("NSE");
////        orderRequest.setTradingsymbol(symbol);
//        orderRequest.setTradingSymbol(symbol);
//        orderRequest.setQuantity(quantity);
//        orderRequest.setDisclosedquantity(0);
//        orderRequest.setTransactiontype(transactionType);
//        orderRequest.setOrdertype("MARKET");
//        orderRequest.setVariety("NORMAL");
//        orderRequest.setProducttype("INTRADAY");
//        orderRequest.setScripconsent("yes");
//
//        // Call API client
//        return brokerApiClient.placeOrder(orderRequest, token);
//    }

    public Mono<OrderResponse> placeWebhookOrder(WebhookRequest webhookRequest) {

        // 1. Extract first stock
        String[] stocks = webhookRequest.getStocks().split(",");
        String stockName = stocks[0].trim();

        // 2. Extract first trigger price
        String[] prices = webhookRequest.getTrigger_prices().split(",");
        Double triggerPrice = Double.parseDouble(prices[0].trim());

        // 3. Get token from NSE map
        String symboltoken = scripMasterService.getTokenForName(stockName);
        if (symboltoken == null) {
            return Mono.error(new RuntimeException("TradingSymbol not found for stock: " + stockName));
        }

        // 4. Get RMS balance
//        RmsData rmsData = scripMasterService.getRmsData();
        RmsData rmsData = rmsService.getCachedRms();
        if (rmsData == null) {
            return Mono.error(new RuntimeException("RMS Data not available. Fetch balance first."));
        }

        Double availableCash = Double.parseDouble(rmsData.getAvailablecash());
        availableCash = 10000.00;
        if (availableCash <= 0) {
            return Mono.error(new RuntimeException("Insufficient balance: " + availableCash));
        }

        // 5. Calculate quantity
        int quantity = (int) Math.floor(availableCash / triggerPrice);
        if (quantity <= 0) {
            return Mono.error(new RuntimeException("Not enough cash to buy even 1 share."));
        }

        // 6. Get JWT token for order
        String jwtToken = tokenStorageService.getJwtToken();
        if (jwtToken == null) {
            return Mono.error(new RuntimeException("User not logged in. No JWT token found."));
        }

//        OrderRequest_v2 orderRequest = new OrderRequest_v2();
//        orderRequest.setExchange("NSE");
//        orderRequest.setTradingsymbol(stockName+"-EQ");
//        orderRequest.setSymboltoken(symboltoken);
//        orderRequest.setOrdertype("MARKET");
//        orderRequest.setProducttype("INTRADAY");
//        orderRequest.setTransactiontype("BUY");
//        orderRequest.setVariety("NORMAL");
////        orderRequest.setVariety("ROBO");
//        orderRequest.setDisclosedquantity(String.valueOf(0));
//        orderRequest.setQuantity(String.valueOf(quantity));
//        orderRequest.setScripconsent("yes");
//        orderRequest.setDuration("DAY");

        BracketOrderRequest orderRequest = createBracketOrderRequest(stockName, symboltoken, quantity, webhookRequest.getTrigger_prices());

        // 8. Call API client
        return brokerApiClient.placeOrder(orderRequest, jwtToken);
    }

//    public Mono<OrderResponse> chartinkBuyOrder(WebhookRequest webhookRequest) {
//
//        String stockName = webhookRequest.getStocks().split(",")[0].trim();
//        double triggerPrice = Double.parseDouble(
//                webhookRequest.getTrigger_prices().split(",")[0].trim()
//        );
//
//        // 3. Get token from NSE map
//        String symboltoken = scripMasterService.getTokenForName(stockName);
//        if (symboltoken == null) {
//            return Mono.error(new RuntimeException("TradingSymbol not found for stock: " + stockName));
//        }
//
//        // 4. Get RMS balance
//        RmsData rmsData = scripMasterService.getRmsData();
//        if (rmsData == null) {
//            return Mono.error(new RuntimeException("RMS Data not available. Fetch balance first."));
//        }
//
//        Double availableCash = Double.parseDouble(rmsData.getAvailablecash());
//        availableCash = 10000.00;
//        if (availableCash <= 0) {
//            return Mono.error(new RuntimeException("Insufficient balance: " + availableCash));
//        }
//
//        // 5. Calculate quantity
//        int quantity = (int) Math.floor(availableCash / triggerPrice);
//        if (quantity <= 0) {
//            return Mono.error(new RuntimeException("Not enough cash to buy even 1 share."));
//        }
//
//        // 6. Get JWT token for order
//        String jwtToken = tokenStorageService.getJwtToken();
//        if (jwtToken == null) {
//            return Mono.error(new RuntimeException("User not logged in. No JWT token found."));
//        }
//
//        IOrderRequest chartinkMISBuyOrderRequest = createChartinkBuyOrderRequest(stockName, symboltoken, quantity);
//
//        return brokerApiClient.chartinkPlaceOrder(chartinkMISBuyOrderRequest, jwtToken)
//                .flatMap(buyResponse -> {
//
//                    if (!buyResponse.isStatus()) {
//                        return Mono.error(new RuntimeException("BUY order failed"));
//                    }
//
////                    double slTriggerPrice = triggerPrice - (triggerPrice * 0.02);
//
//                    IOrderRequest sellSlmOrder =
//                            chartinkSellSlmOrderRequest(
//                                    stockName,
//                                    symboltoken,
//                                    quantity,
//                                    (triggerPrice - (triggerPrice * 0.02))
//                            );
//
//                    return brokerApiClient
//                            .chartinkPlaceOrder(sellSlmOrder, jwtToken)
//                            .doOnSuccess(slResp -> {
//                                slOrderStore.put(
//                                                "NSE:" + stockName + ":" + LocalDate.now(),
//                                                new SlOrderMeta(buyResponse.getData().getOrderid(), slResp.getData().getOrderid(), quantity, (triggerPrice - (triggerPrice * 0.02)), symboltoken
//                                                )
//                                        );
//                            })
//                            .thenReturn(buyResponse);
//                });
//    }

//    public Mono<OrderResponse> chartinkBuyOrder(WebhookRequest webhookRequest) {
//
//        String stockName = webhookRequest.getStocks().split(",")[0].trim();
//        String price = webhookRequest.getTrigger_prices().split(",")[0].trim();
//        double triggerPrice = Double.parseDouble(price);
//
//        // 1. Get token
//        String symboltoken = scripMasterService.getTokenForName(stockName);
//        if (symboltoken == null) {
//            return Mono.error(new RuntimeException("TradingSymbol not found for stock: " + stockName));
//        }
//
//        // 2. RMS balance
////        RmsData rmsData = scripMasterService.getRmsData();
//        RmsData rmsData = rmsService.getCachedRms();
//        if (rmsData == null) {
//            return Mono.error(new RuntimeException("RMS Data not available. Fetch balance first."));
//        }
//
//        Double availableCash = (Double.parseDouble(rmsData.getAvailablecash())*applicationProperties.getPercentBalanceUse());
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
//
//        // 4. JWT
//        String jwtToken = tokenStorageService.getJwtToken();
//        if (jwtToken == null) {
//            return Mono.error(new RuntimeException("User not logged in. No JWT token found."));
//        }
//
////        IOrderRequest buyOrderRequest =
////                createChartinkBuyOrderRequest(stockName, symboltoken, quantity, price);
//
//        IOrderRequest buyOrderRequest =
//                orderRequestFactory.createBuyOrder(stockName, symboltoken, quantity, price);
//
//        return brokerApiClient
//                .chartinkPlaceOrder(buyOrderRequest, jwtToken)
//                .flatMap(buyResponse -> {
//
//                     if (!buyResponse.isStatus()) {
//                        return Mono.error(new RuntimeException("BUY order failed"));
//                    }
//
//                    String buyOrderId = buyResponse.getData().getOrderid();
//
//                    // WAIT until BUY is COMPLETE (retry 5 times, 2 sec delay)
//                    return pollOrderUntilComplete(buyOrderId, jwtToken)
//                            .flatMap(executedPrice  -> {
//
//                                double strikePrice = executedPrice;
//
//                                double profitTriggerPrice = strikePrice * applicationProperties.getProfitPercentageMultiplier();
//
////                                IOrderRequest sellOrder =
////                                        createChartinkSellOrderRequest(
////                                                stockName,
////                                                symboltoken,
////                                                quantity,
////                                                profitTriggerPrice
////                                        );
//
//                                IOrderRequest sellOrder =
//                                        orderRequestFactory.createSellLimitOrder(
//                                                stockName,
//                                                symboltoken,
//                                                quantity,
//                                                profitTriggerPrice
//                                        );
//
//                                // 2% SL from executed price
////                                double slTriggerPrice = strikePrice * applicationProperties.getStoplossPercentageMultiplier();
//
////                                IOrderRequest sellSlmOrder =
////                                        createChartinkSellSlmOrderRequest(
////                                                stockName,
////                                                symboltoken,
////                                                quantity,
////                                                slTriggerPrice
////                                        );
//
//                                IOrderRequest sellSlmOrder =
//                                        orderRequestFactory.createStopLossLimitOrder(
//                                                stockName,
//                                                symboltoken,
//                                                quantity,
//                                                slTriggerPrice
//                                        );
//
////                                return brokerApiClient
////                                        .chartinkPlaceOrder(sellOrder, jwtToken)
////                                        .flatMap(sellResp -> {
////
////                                            if (!sellResp.isStatus()) {
////                                                return Mono.error(
////                                                        new RuntimeException("SELL order failed")
////                                                );
////                                            }
////
////                                            String sellOrderId =
////                                                    sellResp.getData().getOrderid();
////
////                                            // 2️⃣ Place STOP LOSS order AFTER SELL
////                                            return brokerApiClient
////                                                    .chartinkPlaceOrder(sellSlmOrder, jwtToken)
////                                                    .doOnSuccess(slResp -> {
////
////                                                        slOrderStore.put(
////                                                                "NSE:" + stockName + ":" + LocalDate.now(),
////                                                                new SlOrderMeta(
////                                                                        buyOrderId,
////                                                                        slResp.getData().getOrderid(),
////                                                                        quantity,
////                                                                        slTriggerPrice,
////                                                                        symboltoken
////                                                                )
////                                                        );
////                                                    })
////                                                    // Final return value
////                                                    .thenReturn(buyResponse);
////                                        });
//                                Mono<OrderResponse> sellMono =
//                                        withOrderRetry(
//                                                brokerApiClient.chartinkPlaceOrder(sellOrder, jwtToken)
//                                                        .flatMap(resp -> {
//                                                            if (!resp.isStatus()) {
//                                                                return Mono.error(
//                                                                        new RuntimeException("SELL order failed")
//                                                                );
//                                                            }
//                                                            return Mono.just(resp);
//                                                        })
//                                        );
//
//                                Mono<OrderResponse> slmMono =
//                                        withOrderRetry(
//                                                brokerApiClient.chartinkPlaceOrder(sellSlmOrder, jwtToken)
//                                                        .flatMap(resp -> {
//                                                            if (!resp.isStatus()) {
//                                                                return Mono.error(
//                                                                        new RuntimeException("SLM order failed")
//                                                                );
//                                                            }
//                                                            return Mono.just(resp);
//                                                        })
//                                                        .doOnSuccess(slResp -> {
//                                                            slOrderStore.put(
//                                                                    "NSE:" + stockName + ":" + LocalDate.now(),
//                                                                    new SlOrderMeta(
//                                                                            buyOrderId,
//                                                                            slResp.getData().getOrderid(),
//                                                                            quantity,
//                                                                            slTriggerPrice,
//                                                                            symboltoken
//                                                                    )
//                                                            );
//                                                        })
//                                        );
//
//                                return Mono.zip(sellMono, slmMono)
//                                        .doOnSuccess(tuple -> {
//                                            OrderResponse sellResp = tuple.getT1();
//                                            OrderResponse slResp = tuple.getT2();
//
//                                            OrderContext ctx =
//                                                    new OrderContext(
//                                                            buyOrderId,
//                                                            sellResp.getData().getOrderid(),
//                                                            slResp.getData().getOrderid(),
//                                                            stockName,
//                                                            symboltoken,
//                                                            quantity,
//                                                            "NORMAL",
//                                                            "STOPLOSS"
//                                                    );
//
//                                            orderRegistry.register(ctx);
//                                        })
//                                        .thenReturn(buyResponse);
//
//
//                            });
//
//                });
//    }

    public Mono<OrderResponse> chartinkModifyOrder(WebhookRequest webhookRequest) {
        String stockName = webhookRequest.getStocks().split(",")[0].trim();

        SlOrderMeta meta = slOrderStore.get("NSE:" + stockName + ":" + LocalDate.now());

        String jwtToken = tokenStorageService.getJwtToken();
        if (jwtToken == null) {
            return Mono.error(new RuntimeException("User not logged in. No JWT token found."));
        }

        double triggerPrice = Double.parseDouble(
                webhookRequest.getTrigger_prices().split(",")[0].trim()
        );

        IOrderRequest modifyReq =
                modifyChartinkSellSlmOrderRequest(
                        stockName,
                        meta.getSymbolToken(),
                        meta.getQuantity(),
                        triggerPrice - (triggerPrice*0.02),
                        meta.getSlOrderId()
                );

        return brokerApiClient.chartinkModifyOrder(modifyReq, jwtToken);
    }

    private OrderRequest createOrderRequest(String stockName, String symboltoken, int quantity) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setExchange("NSE");
        orderRequest.setTradingsymbol(stockName + "-EQ");
        orderRequest.setSymboltoken(symboltoken);
        orderRequest.setOrdertype("MARKET");
        orderRequest.setProducttype("INTRADAY");
        orderRequest.setTransactiontype("BUY");
        orderRequest.setVariety("NORMAL");
        orderRequest.setDisclosedquantity("0");
        orderRequest.setQuantity(String.valueOf(quantity));
        orderRequest.setScripconsent("yes");
        orderRequest.setDuration("DAY");

        return orderRequest;
    }

    private BracketOrderRequest createBracketOrderRequest(String stockName, String symboltoken, int quantity, String price) {
//        OrderRequest_v2 orderRequest = new OrderRequest_v2();
//        orderRequest.setExchange("NSE");
//        orderRequest.setTradingsymbol(stockName + "-EQ");
//        orderRequest.setSymboltoken(symboltoken);
//        orderRequest.setOrdertype("MARKET");
//        orderRequest.setProducttype("INTRADAY");
//        orderRequest.setTransactiontype("BUY");
//        orderRequest.setVariety("NORMAL");
//        orderRequest.setDisclosedquantity("0");
//        orderRequest.setQuantity(String.valueOf(quantity));
//        orderRequest.setScripconsent("yes");
//        orderRequest.setDuration("DAY");

        BracketOrderRequest orderRequest = new BracketOrderRequest();
        orderRequest.setExchange("NSE");
        orderRequest.setTradingsymbol(stockName + "-EQ");
        orderRequest.setSymboltoken(symboltoken);
//        orderRequest.setOrdertype("MARKET");
        orderRequest.setOrdertype("LIMIT");
        orderRequest.setProducttype("BO");
        orderRequest.setTransactiontype("BUY");
        orderRequest.setVariety("ROBO");
        orderRequest.setDisclosedquantity("0");
        orderRequest.setQuantity(String.valueOf(quantity));
        orderRequest.setScripconsent("yes");
        orderRequest.setDuration("DAY");
        orderRequest.setSquareoff("20");
        orderRequest.setStoploss("20");
        orderRequest.setPrice(price);
        double double_price = Double.parseDouble(price);
        orderRequest.setTriggerprice(String.valueOf(double_price + (double_price * 0.05)));

        return orderRequest;
    }

    private ChartInkMISBuyOrderRequest createChartinkBuyOrderRequest(String stockName, String symboltoken, int quantity, String price) {

//        ChartInkMISBuyOrderRequest chartinkBuyOrderRequest = new ChartInkMISBuyOrderRequest();
//        chartinkBuyOrderRequest.setVariety("NORMAL");
//        chartinkBuyOrderRequest.setTradingsymbol(stockName + "-EQ");
//        chartinkBuyOrderRequest.setSymboltoken(symboltoken);
//        chartinkBuyOrderRequest.setTransactiontype("BUY");
//        chartinkBuyOrderRequest.setExchange("NSE");
//        chartinkBuyOrderRequest.setOrdertype("MARKET");
//        chartinkBuyOrderRequest.setProducttype("INTRADAY");
//        chartinkBuyOrderRequest.setDuration("DAY");
//        chartinkBuyOrderRequest.setDisclosedquantity("0");
//        chartinkBuyOrderRequest.setQuantity(String.valueOf(quantity));
//        chartinkBuyOrderRequest.setScripconsent("yes");

        ChartInkMISBuyOrderRequest chartinkBuyOrderRequest = new ChartInkMISBuyOrderRequest();
        chartinkBuyOrderRequest.setVariety("NORMAL");
        chartinkBuyOrderRequest.setTradingsymbol(stockName + "-EQ");
        chartinkBuyOrderRequest.setSymboltoken(symboltoken);
        chartinkBuyOrderRequest.setTransactiontype("BUY");
        chartinkBuyOrderRequest.setExchange("NSE");
        chartinkBuyOrderRequest.setOrdertype("LIMIT");
        chartinkBuyOrderRequest.setProducttype("INTRADAY");
        chartinkBuyOrderRequest.setDuration("DAY");
        chartinkBuyOrderRequest.setDisclosedquantity("0");
        chartinkBuyOrderRequest.setQuantity(String.valueOf(quantity));
        chartinkBuyOrderRequest.setScripconsent("yes");
        chartinkBuyOrderRequest.setPrice(price);
//        chartinkBuyOrderRequest.setSquareoff("20");
//        chartinkBuyOrderRequest.setStoploss("20");

//        double double_price = Double.parseDouble(price);
//        orderRequest.setTriggerprice(String.valueOf(double_price+(double_price * 0.05)));

        return chartinkBuyOrderRequest;
    }

    private ChartinkMIS_SL_Market_OrderRequest createChartinkSellSlmOrderRequest(String stockName, String symboltoken, int quantity, double triggerPrice) {
        ChartinkMIS_SL_Market_OrderRequest sellOrder = new ChartinkMIS_SL_Market_OrderRequest();
        sellOrder.setVariety("STOPLOSS");
        sellOrder.setTradingsymbol(stockName + "-EQ");
        sellOrder.setSymboltoken(symboltoken);
        sellOrder.setTransactiontype("SELL");
        sellOrder.setExchange("NSE");
        sellOrder.setOrdertype("STOPLOSS_MARKET");
        sellOrder.setProducttype("INTRADAY");
        sellOrder.setDuration("DAY");
//        chartinkBuyOrderRequest.setPrice(price);
//        chartinkBuyOrderRequest.setSquareoff("20");
//        chartinkBuyOrderRequest.setStoploss("20");
        sellOrder.setQuantity(String.valueOf(quantity));
        sellOrder.setTriggerprice(String.valueOf(triggerPrice));
        sellOrder.setDisclosedquantity("0");
        sellOrder.setScripconsent("yes");

        return sellOrder;
    }

    private ChartinkMIS_SL_Market_OrderRequest modifyChartinkSellSlmOrderRequest(String stockName, String symboltoken, int quantity, double triggerPrice, String slOrderId) {
        ChartinkMIS_SL_Market_OrderRequest modifiedSLOrder = new ChartinkMIS_SL_Market_OrderRequest();
        modifiedSLOrder.setVariety("STOPLOSS");
        modifiedSLOrder.setOrderid(slOrderId);
        modifiedSLOrder.setTradingsymbol(stockName + "-EQ");
        modifiedSLOrder.setSymboltoken(symboltoken);
        modifiedSLOrder.setTransactiontype("SELL");
        modifiedSLOrder.setExchange("NSE");
        modifiedSLOrder.setOrdertype("STOPLOSS_MARKET");
        modifiedSLOrder.setProducttype("INTRADAY");
        modifiedSLOrder.setDuration("DAY");
//        chartinkBuyOrderRequest.setPrice(price);
//        chartinkBuyOrderRequest.setSquareoff("20");
//        chartinkBuyOrderRequest.setStoploss("20");
        modifiedSLOrder.setQuantity(String.valueOf(quantity));
        modifiedSLOrder.setTriggerprice(String.valueOf(triggerPrice));
//        sellOrder.setDisclosedquantity("0");
        modifiedSLOrder.setScripconsent("yes");

        return modifiedSLOrder;
    }

    private ChartinkMISSellOrderRequest createChartinkSellOrderRequest( String stockName, String symboltoken, int quantity, double sellPrice) {

//        ChartinkMISSellOrderRequest sellOrder = new ChartinkMISSellOrderRequest();
//        sellOrder.setVariety("NORMAL");
//        sellOrder.setTradingsymbol(stockName + "-EQ");
//        sellOrder.setSymboltoken(symboltoken);
//        sellOrder.setTransactiontype("SELL");
//        sellOrder.setExchange("NSE");
//        sellOrder.setOrdertype("MARKET");
//        sellOrder.setProducttype("INTRADAY");
//        sellOrder.setDuration("DAY");
//
//        sellOrder.setPrice("0.00");
//        sellOrder.setSquareoff("0");
//        sellOrder.setStoploss("0");
//        sellOrder.setQuantity(String.valueOf(quantity));
//
//        return sellOrder;

        ChartinkMISSellOrderRequest sellOrder = new ChartinkMISSellOrderRequest();
        sellOrder.setVariety("NORMAL");
        sellOrder.setTradingsymbol(stockName + "-EQ");
        sellOrder.setSymboltoken(symboltoken);
        sellOrder.setTransactiontype("SELL");
        sellOrder.setExchange("NSE");
        sellOrder.setOrdertype("LIMIT");
        sellOrder.setProducttype("INTRADAY");
        sellOrder.setDuration("DAY");
        sellOrder.setPrice(String.valueOf(sellPrice));
        sellOrder.setQuantity(String.valueOf(quantity));

        sellOrder.setSquareoff("0");           // Required for NORMAL variety [1]
        sellOrder.setStoploss("0");            // Required for NORMAL variety [1]
        sellOrder.setDisclosedquantity("0");   // Standard parameter [3]
        sellOrder.setTriggerprice("0");
        sellOrder.setScripconsent("yes");

        return sellOrder;
    }

//    public Mono<OrderStatusData> pollOrderUntilComplete(String orderId, String jwtToken) {
//
//        return Mono.defer(() ->
//                        brokerApiClient
////                                .getIndividualOrderStatus(orderId, jwtToken)
//                                .getOrderBook(jwtToken)
//                                .map(OrderBookResponse_v2::getData)
//                                .flatMapMany(Flux::fromIterable)             // Flux<OrderStatusItem>
//                                .filter(item -> orderId.equals(item.getOrderid()))
//                                .flatMap(resp -> {
//
//                                    // ✅ THIS NOW COMPILES
////                                    if (resp.isStatus()) {
//                                        if (true) {
//                                        return Mono.error(
//                                                new RuntimeException("Order status API failed")
//                                        );
//                                    }
//
////                                    OrderStatusData orderData = resp.getData();
//                                    OrderStatusData orderData = null;
//
//                                    if (orderData.isRejected()) {
//                                        return Mono.error(
//                                                new RuntimeException(
//                                                        "BUY order failed with status: " +
//                                                                orderData.getOrderstatus()
//                                                )
//                                        );
//                                    }
//
//                                    if (orderData.isCompleted()) {
//                                        return Mono.just(orderData);
//                                    }
//
//                                    return Mono.error(
//                                            new IllegalStateException(
//                                                    "Order not complete yet. Status=" +
//                                                            orderData.getOrderstatus()
//                                            )
//                                    );
//                                })
//                )
//                .retryWhen(
//                        reactor.util.retry.Retry
//                                .fixedDelay(5, Duration.ofSeconds(2))
//                                .filter(ex -> ex instanceof IllegalStateException)
//                                .onRetryExhaustedThrow(
//                                        (spec, signal) ->
//                                                new RuntimeException(
//                                                        "BUY order not completed after 5 attempts"
//                                                )
//                                )
//                );
//    }

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

//    private Mono<OrderResult> evaluateOrder(Object input) {
//
//        // ❌ Order not found
//        if (input == OrderResult.NOT_FOUND) {
//            return Mono.just(OrderResult.notFound());
//        }
//
//        OrderStatusItem order = (OrderStatusItem) input;
//        String status = order.getStatus();
//
//        // ❌ Rejected
//        if ("REJECTED".equalsIgnoreCase(status)) {
//            return Mono.just(OrderResult.rejected());
//        }
//
//        // ✅ Completed
//        if ("COMPLETE".equalsIgnoreCase(status) ||
//                "FILLED".equalsIgnoreCase(status)) {
//
//            double avgPrice = Double.parseDouble(order.getAverageprice());
//            return Mono.just(OrderResult.completed(avgPrice));
//        }
//
//        // ⏳ Still pending → keep polling
//        return Mono.just(OrderResult.pending());
//    }

    public Mono<JsonNode> getOrderStatus(String orderId) {

        log.info("Service: Fetching order status for orderId={}", orderId);

        String jwtToken = tokenStorageService.getJwtToken();
        if (jwtToken == null) {
            return Mono.error(new RuntimeException("JWT token not found"));
        }

        return brokerApiClient
                .getIndividualOrderStatus(orderId, jwtToken)
                .doOnSuccess(resp ->
                        log.info("Service: Order status fetched successfully for orderId={}", orderId)
                )
                .doOnError(err ->
                        log.error("Service: Failed to fetch order status for orderId={}", orderId, err)
                );
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

}

//import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
//import com.onepercentgrowth.local_to_smartapi.factory.OrderRequestFactory;
//import com.onepercentgrowth.local_to_smartapi.model.*;
////import com.onepercentgrowth.local_to_smartapi.model.chartink_request.WebhookRequest;
//import com.onepercentgrowth.local_to_smartapi.model.chartink_request.IOrderRequest;
//import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
//import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
//import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
//import com.onepercentgrowth.local_to_smartapi.storage.SlOrderStore;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Mono;
//import tools.jackson.databind.JsonNode;
//
//import java.time.LocalDate;
//
//@Service
//public class OrderService {
//
//    private static final Logger log =
//            LoggerFactory.getLogger(OrderService.class);
//
//    private final ScripMasterService scripMasterService;
//    private final TokenManager tokenManager;
//    private final OrderValidationService validationService;
//    private final OrderCalculationService calculationService;
//    private final OrderExecutionService executionService;
//    private final OrderPollingService pollingService;
//    private final OrderRegistry orderRegistry;
//    private final SlOrderStore slOrderStore;
//    private final ApplicationProperties applicationProperties;
//    private final OrderRequestFactory orderRequestFactory;
//    private final BrokerApiClient brokerApiClient;
//
//    public OrderService(
//            ScripMasterService scripMasterService,
//            TokenManager tokenManager,
//            OrderValidationService validationService,
//            OrderCalculationService calculationService,
//            OrderExecutionService executionService,
//            OrderPollingService pollingService,
//            OrderRegistry orderRegistry,
//            SlOrderStore slOrderStore,
//            ApplicationProperties applicationProperties,
//            OrderRequestFactory orderRequestFactory,
//            BrokerApiClient brokerApiClient
//    ) {
//        this.scripMasterService = scripMasterService;
//        this.tokenManager = tokenManager;
//        this.validationService = validationService;
//        this.calculationService = calculationService;
//        this.executionService = executionService;
//        this.pollingService = pollingService;
//        this.orderRegistry = orderRegistry;
//        this.slOrderStore = slOrderStore;
//        this.applicationProperties = applicationProperties;
//        this.orderRequestFactory = orderRequestFactory;
//        this.brokerApiClient = brokerApiClient;
//    }
//
//    // ----------------------------------------------------
//    // ENTRY POINT FROM WEBHOOK / CONTROLLER
//    // ----------------------------------------------------
//
//    public Mono<OrderResponse> chartinkBuyOrder(WebhookRequest webhook) {
//
//        return Mono.defer(() -> {
//
//            // 1️⃣ Extract & validate basic input
//            String stockName =
//                    webhook.getStocks().split(",")[0].trim();
//
//            double triggerPrice =
//                    Double.parseDouble(
//                            webhook.getTrigger_prices().split(",")[0].trim()
//                    );
//
//            String jwtToken = tokenManager.getValidJwtToken();
//            validationService.validateToken(jwtToken);
//
//            // 2️⃣ Resolve symbol token
//            String symbolToken =
//                    scripMasterService.getTokenForName(stockName);
//            validationService.validateSymbol(symbolToken, stockName);
//
//            // 3️⃣ Get RMS & calculate quantity
//            var rms = scripMasterService.getRmsData();
//            validationService.validateRms(rms);
//
//            int quantity = calculationService.calculateQuantity(
//                    Double.parseDouble(rms.getAvailablecash()),
//                    triggerPrice
//            );
//
////            final int quantity = 1;
//
//            log.info("📊 Calculated quantity={} for stock={}",
//                    quantity, stockName);
//
//            // 4️⃣ Place BUY order
//            return executionService
//                    .placeBuyOrder(
//                            stockName,
//                            symbolToken,
//                            quantity,
//                            String.valueOf(triggerPrice),
//                            jwtToken
//                    )
//                    .flatMap(buyResp ->
//                            handleBuyOrder(
//                                    buyResp,
//                                    stockName,
//                                    symbolToken,
//                                    quantity,
//                                    jwtToken
//                            )
//                    );
//        });
//    }
//
//
//    // ----------------------------------------------------
//    // BUY → WAIT → SELL + SL
//    // ----------------------------------------------------
//
//    private Mono<OrderResponse> handleBuyOrder(
//            OrderResponse buyResponse,
//            String stockName,
//            String symbolToken,
//            int quantity,
//            String jwtToken
//    ) {
//        String buyOrderId = buyResponse.getData().getOrderid();
//
//        log.info("⏳ Waiting for BUY order {} to complete", buyOrderId);
//
//        return pollingService
//                .waitUntilCompleted(buyOrderId, jwtToken)
//                .flatMap(executedPrice -> {
//
//                    log.info("✅ BUY completed at price={}", executedPrice);
//
//                    double sellPrice =
//                            calculationService.calculateProfitPrice(executedPrice);
//
//                    double slPrice =
//                            calculationService.calculateStopLossPrice(executedPrice);
//
//                    // Place SELL & SL in parallel
//                    return Mono.zip(
//                            executionService.placeSellOrder(
//                                    stockName,
//                                    symbolToken,
//                                    quantity,
//                                    sellPrice,
//                                    jwtToken
//                            ),
//                            executionService.placeStopLossOrder(
//                                    stockName,
//                                    symbolToken,
//                                    quantity,
//                                    slPrice,
//                                    jwtToken
//                            )
//                    ).flatMap(tuple -> {
//
//                        String sellOrderId =
//                                tuple.getT1().getData().getOrderid();
//                        String slOrderId =
//                                tuple.getT2().getData().getOrderid();
//
//                        // ✅ Create full OrderContext
//                        OrderContext ctx = new OrderContext(
//                                buyOrderId,
//                                sellOrderId,
//                                slOrderId,
//                                stockName,
//                                symbolToken,
//                                quantity,
//                                "NORMAL",
//                                "STOPLOSS"
//                        );
//
//                        // ✅ Register ONCE
//                        orderRegistry.register(ctx);
//
//                        String key = "NSE:" + stockName + ":" + LocalDate.now();
//
//                        // Optional persistence
//                        SlOrderMeta meta = new SlOrderMeta(
//                                buyOrderId,
//                                slOrderId,
//                                quantity,
//                                slPrice,        // triggerPrice
//                                symbolToken
//                        );
//
//                        slOrderStore.put(key, meta);
//
//                        log.info(
//                                "🎯 Order chain registered | BUY={} SELL={} SL={}",
//                                buyOrderId, sellOrderId, slOrderId
//                        );
//
//                        return Mono.just(buyResponse);
//                    });
//                });
//    }
//
//    public Mono<OrderResponse> placeWebhookOrder(WebhookRequest webhookRequest) {
//
//        // 1️⃣ Parse webhook
//        String stockName = webhookRequest.getStocks().split(",")[0].trim();
//        String priceStr = webhookRequest.getTrigger_prices().split(",")[0].trim();
//        double triggerPrice = Double.parseDouble(priceStr);
//
//        // 2️⃣ Resolve symbol token
//        String symbolToken = scripMasterService.getTokenForName(stockName);
//        if (symbolToken == null) {
//            return Mono.error(
//                    new IllegalArgumentException("Symbol token not found for " + stockName)
//            );
//        }
//
//        // 3️⃣ Get RMS balance
//        RmsData rms = scripMasterService.getRmsData();
//        if (rms == null) {
//            return Mono.error(new IllegalStateException("RMS data not available"));
//        }
//
//        double availableCash =
//                Double.parseDouble(rms.getAvailablecash())
//                        * applicationProperties.getPercentBalanceUse();
//
//        if (availableCash <= applicationProperties.getBalanceMinimumAllowed()) {
//            return Mono.error(
//                    new IllegalStateException("Insufficient balance: " + availableCash)
//            );
//        }
//
//        // 4️⃣ Quantity calculation
////        int quantity =
////                (int) Math.floor(availableCash / triggerPrice)
////                        - applicationProperties.getNumberOfStocksBuyLess();
////
////        if (quantity <= applicationProperties.getStockBuyMinimumQuantityRequired()) {
////            return Mono.error(
////                    new IllegalStateException("Quantity too low: " + quantity)
////            );
////        }
//        int quantity = calculationService.calculateQuantity(
//                Double.parseDouble(rms.getAvailablecash()),
//                triggerPrice
//        );
//
//        // 5️⃣ JWT
//        String jwtToken = tokenManager.getValidJwtToken();
//        if (jwtToken == null) {
//            return Mono.error(new IllegalStateException("JWT token missing"));
//        }
//
//        // 6️⃣ Delegate execution
//        return executionService
//                .executeBracketFlow(
//                        stockName,
//                        symbolToken,
//                        quantity,
//                        priceStr,
//                        jwtToken
//                );
//    }
//
//    public Mono<OrderResponse> chartinkModifyOrder(WebhookRequest webhookRequest) {
//
//        // 1️⃣ Parse stock
//        String stockName =
//                webhookRequest.getStocks().split(",")[0].trim();
//
//        // 2️⃣ Load existing SL metadata
//        String key = "NSE:" + stockName + ":" + LocalDate.now();
//        SlOrderMeta meta = slOrderStore.get(key);
//
//        if (meta == null) {
//            return Mono.error(
//                    new IllegalStateException("No SL order found for " + stockName)
//            );
//        }
//
//        // 3️⃣ JWT
//        String jwtToken = tokenManager.getValidJwtToken();
//        if (jwtToken == null) {
//            return Mono.error(new IllegalStateException("JWT token missing"));
//        }
//
//        // 4️⃣ New trigger price from webhook
//        double webhookPrice =
//                Double.parseDouble(
//                        webhookRequest.getTrigger_prices().split(",")[0].trim()
//                );
//
//        // 5️⃣ Apply SL calculation logic
//        double newTriggerPrice =
//                calculationService.calculateStopLossPrice(webhookPrice);
//
//        // 6️⃣ Create modify request via factory
//        IOrderRequest modifyRequest =
//                orderRequestFactory.modifyStopLossOrder(
//                        stockName,
//                        meta.getSymbolToken(),
//                        meta.getQuantity(),
//                        newTriggerPrice,
//                        meta.getSlOrderId()
//                );
//
//        // 7️⃣ Call broker
//        return brokerApiClient.chartinkModifyOrder(modifyRequest, jwtToken);
//    }
//
//    public Mono<JsonNode> getOrderStatus(String orderId) {
//
//        log.info("📄 Fetching order status for orderId={}", orderId);
//
//        if (orderId == null || orderId.isBlank()) {
//            return Mono.error(
//                    new IllegalArgumentException("orderId must not be blank")
//            );
//        }
//
//        // ✅ Always go through TokenManager
//        String jwtToken = tokenManager.getValidJwtToken();
//        if (jwtToken == null) {
//            return Mono.error(
//                    new IllegalStateException("JWT token unavailable")
//            );
//        }
//
//        return brokerApiClient
//                .getIndividualOrderStatus(orderId, jwtToken)
//                .doOnSuccess(resp ->
//                        log.info("✅ Order status fetched for orderId={}", orderId)
//                )
//                .doOnError(err ->
//                        log.error("❌ Failed to fetch order status for orderId={}",
//                                orderId, err)
//                );
//    }
//
//}
