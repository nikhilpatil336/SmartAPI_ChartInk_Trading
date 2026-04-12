package com.onepercentgrowth.local_to_smartapi.factory;

import com.onepercentgrowth.local_to_smartapi.model.chartink_request.*;
import com.onepercentgrowth.local_to_smartapi.utility.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChartinkOrderRequestFactory implements OrderRequestFactory {

    private static final Logger log = LoggerFactory.getLogger(ChartinkOrderRequestFactory.class);

    @Override
    public IOrderRequest createBuyOrder(
            String stockName,
            String symbolToken,
            int quantity,
            String price
    ) {
        ChartInkBuyLimitOrderRequest req = new ChartInkBuyLimitOrderRequest();
        req.setVariety("NORMAL");
        req.setTradingsymbol(Utility.ensureEqSuffix(stockName));
        req.setSymboltoken(symbolToken);
        req.setTransactiontype("BUY");
        req.setExchange("NSE");
        req.setOrdertype("LIMIT");
        req.setProducttype("INTRADAY");
        req.setDuration("DAY");
        req.setQuantity(String.valueOf(quantity));
        req.setPrice(price);
        req.setDisclosedquantity("0");
        req.setScripconsent("yes");
        return req;
    }

    @Override
    public IOrderRequest modifyBuyOrder(
            String stockName,
            String symbolToken,
            int quantity,
            String price,
            String orderId // The unique ID returned during placement [10, 12]
    ) {
        // Note: Use the appropriate request class for modifications
        ChartInkBuyLimitOrderRequest req = new ChartInkBuyLimitOrderRequest();

        req.setOrderid(orderId); // Mandatory for modification [12, 13]
        req.setVariety("NORMAL"); // Must match the original order variety [12]
        req.setTradingsymbol(Utility.ensureEqSuffix(stockName));
        req.setSymboltoken(symbolToken);
        req.setExchange("NSE");
        req.setTransactiontype("BUY");
        // New parameters to be updated
        req.setOrdertype("LIMIT");
        req.setProducttype("INTRADAY");
        req.setDuration("DAY");
        req.setQuantity(String.valueOf(quantity)); // New quantity [12]
        req.setPrice(price); // New limit price [12]
        req.setDisclosedquantity("0");
//        req.setSquareoff("0");
//        req.setStoploss("0");
//        req.setTriggerprice(String.valueOf(triggerPrice));

        return req;
    }

    @Override
    public IOrderRequest createSellLimitOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double price
    ) {
        ChartinkSellLimitOrderRequest req = new ChartinkSellLimitOrderRequest();
        req.setVariety("NORMAL");
        req.setTradingsymbol(Utility.ensureEqSuffix(stockName));
        req.setSymboltoken(symbolToken);
        req.setTransactiontype("SELL");
        req.setExchange("NSE");
        req.setOrdertype("LIMIT");
        req.setProducttype("INTRADAY");
        req.setDuration("DAY");
        req.setQuantity(String.valueOf(quantity));
        req.setPrice(String.valueOf(price));
        req.setDisclosedquantity("0");
        req.setSquareoff("0");
        req.setStoploss("0");
        req.setTriggerprice("0");
        req.setScripconsent("yes");
        return req;
    }

    @Override
    public IOrderRequest modifySellLimitOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double triggerPrice, // Used for SL orders, otherwise 0
            String orderId,
            double limitPrice
    ) {
        ChartinkSellLimitOrderRequest req = new ChartinkSellLimitOrderRequest();
        req.setOrderid(orderId);
        req.setVariety("NORMAL");
        req.setTradingsymbol(Utility.ensureEqSuffix(stockName));
        req.setSymboltoken(symbolToken);
        req.setExchange("NSE");
        req.setTransactiontype("SELL");
        req.setOrdertype("LIMIT");
        req.setProducttype("INTRADAY");
        req.setDuration("DAY");
        req.setQuantity(String.valueOf(quantity));
        req.setPrice(String.valueOf(limitPrice));
        req.setDisclosedquantity("0");
        req.setSquareoff("0");
        req.setStoploss("0");
        req.setScripconsent("yes");
        req.setTriggerprice(String.valueOf(triggerPrice));

        return req;
    }

//    @Override
//    public IOrderRequest createStopLossMarketOrder(
//            String stockName,
//            String symbolToken,
//            int quantity,
//            double triggerPrice
//    ) {
//        log.info("Trigger price for stoploss market order is {}", triggerPrice);
//
//        ChartinkMIS_SL_Market_OrderRequest req = new ChartinkMIS_SL_Market_OrderRequest();
//        req.setVariety("STOPLOSS");
//        req.setTradingsymbol(Utility.ensureEqSuffix(stockName));
//        req.setSymboltoken(symbolToken);
//        req.setTransactiontype("SELL");
//        req.setExchange("NSE");
//        req.setOrdertype("STOPLOSS_MARKET");
//        req.setProducttype("INTRADAY");
//        req.setDuration("DAY");
//        req.setQuantity(String.valueOf(quantity));
//        req.setTriggerprice(String.valueOf(triggerPrice));
//        req.setDisclosedquantity("0");
//        req.setScripconsent("yes");
//        return req;
//    }

    @Override
    public IOrderRequest createStopLossLimitOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double triggerPrice,
            double limitPrice,
            String orderType
    ) {
        log.info("Trigger price for stoploss limit order is {}", triggerPrice);

        ChartinkMIS_SL_Limit_OrderRequest req = new ChartinkMIS_SL_Limit_OrderRequest();
        req.setVariety("STOPLOSS");
        req.setTradingsymbol(Utility.ensureEqSuffix(stockName));
        req.setSymboltoken(symbolToken);
//        req.setTransactiontype("SELL");
        req.setTransactiontype(orderType);
        req.setExchange("NSE");
        req.setOrdertype("STOPLOSS_LIMIT");
        req.setProducttype("INTRADAY");
        req.setDuration("DAY");
        req.setQuantity(String.valueOf(quantity));
        req.setTriggerprice(String.valueOf(triggerPrice));
        req.setPrice(String.valueOf(limitPrice));
        req.setDisclosedquantity("0");
        req.setScripconsent("yes");
        return req;
    }

//    @Override
//    public IOrderRequest modifyStopLossOrder(
//            String stockName,
//            String symbolToken,
//            int quantity,
//            double triggerPrice,
//            String orderId
//    ) {
//        ChartinkMIS_SL_Market_OrderRequest req =
//                (ChartinkMIS_SL_Market_OrderRequest)
//                        createStopLossMarketOrder(stockName, symbolToken, quantity, triggerPrice);
//
//        req.setOrderid(orderId);
//        return req;
//    }

    @Override
    public IOrderRequest modifyLimitStopLossOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double triggerPrice,
            String orderId,
            double limitPrice,
            String orderType
    ) {
        ChartinkMIS_SL_Limit_OrderRequest req = new ChartinkMIS_SL_Limit_OrderRequest();
        req.setOrderid(orderId);
        req.setVariety("STOPLOSS");
        req.setTradingsymbol(Utility.ensureEqSuffix(stockName));
        req.setSymboltoken(symbolToken);
        req.setExchange("NSE");
        req.setTransactiontype(orderType);
        req.setOrdertype("STOPLOSS_LIMIT");
        req.setProducttype("INTRADAY");
        req.setDuration("DAY");
        req.setQuantity(String.valueOf(quantity));
        req.setPrice(String.valueOf(limitPrice)); // The new execution price
        req.setDisclosedquantity("0");
        req.setScripconsent("yes");
        req.setTriggerprice(String.valueOf(triggerPrice));
        return req;
    }

    @Override
    public IOrderRequest createCancelOrder(String orderId, String variety) {
        ChartinkCancelOrderRequest request = new ChartinkCancelOrderRequest(orderId, variety);

        return request;
    }
}

