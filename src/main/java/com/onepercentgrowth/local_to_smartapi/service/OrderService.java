package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.model.OrderRequest;
import com.onepercentgrowth.local_to_smartapi.model.OrderResponse;
import com.onepercentgrowth.local_to_smartapi.model.RmsData;
import com.onepercentgrowth.local_to_smartapi.model.WebhookRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final BrokerApiClient brokerApiClient;
    private final TokenStorageService tokenStorageService;
    private final ScripMasterService scripMasterService;

    public OrderService(BrokerApiClient brokerApiClient,
                        TokenStorageService tokenStorageService,
                        ScripMasterService scripMasterService) {
        this.brokerApiClient = brokerApiClient;
        this.tokenStorageService = tokenStorageService;
        this.scripMasterService = scripMasterService;
    }

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
        RmsData rmsData = scripMasterService.getRmsData();
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

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setExchange("NSE");
        orderRequest.setTradingsymbol(stockName+"-EQ");
        orderRequest.setSymboltoken(symboltoken);
        orderRequest.setOrdertype("MARKET");
        orderRequest.setProducttype("INTRADAY");
        orderRequest.setTransactiontype("BUY");
        orderRequest.setVariety("NORMAL");
//        orderRequest.setVariety("ROBO");
        orderRequest.setDisclosedquantity(String.valueOf(0));
        orderRequest.setQuantity(String.valueOf(quantity));
        orderRequest.setScripconsent("yes");
        orderRequest.setDuration("DAY");

        // 8. Call API client
        return brokerApiClient.placeOrder(orderRequest, jwtToken);
    }
}
