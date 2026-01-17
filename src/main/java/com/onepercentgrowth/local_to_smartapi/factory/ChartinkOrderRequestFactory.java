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
        ChartInkMISBuyOrderRequest req = new ChartInkMISBuyOrderRequest();
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
    public IOrderRequest createSellLimitOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double price
    ) {
        ChartinkMISSellOrderRequest req = new ChartinkMISSellOrderRequest();
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
    public IOrderRequest createStopLossMarketOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double triggerPrice
    ) {
        log.info("Trigger price for stoploss market order is {}", triggerPrice);

        ChartinkMIS_SL_Market_OrderRequest req = new ChartinkMIS_SL_Market_OrderRequest();
        req.setVariety("STOPLOSS");
        req.setTradingsymbol(Utility.ensureEqSuffix(stockName));
        req.setSymboltoken(symbolToken);
        req.setTransactiontype("SELL");
        req.setExchange("NSE");
        req.setOrdertype("STOPLOSS_MARKET");
        req.setProducttype("INTRADAY");
        req.setDuration("DAY");
        req.setQuantity(String.valueOf(quantity));
        req.setTriggerprice(String.valueOf(triggerPrice));
        req.setDisclosedquantity("0");
        req.setScripconsent("yes");
        return req;
    }

    @Override
    public IOrderRequest createStopLossLimitOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double triggerPrice,
            double limitPrice
    ) {
        log.info("Trigger price for stoploss limit order is {}", triggerPrice);

        ChartinkMIS_SL_Limit_OrderRequest req = new ChartinkMIS_SL_Limit_OrderRequest();
        req.setVariety("STOPLOSS");
        req.setTradingsymbol(Utility.ensureEqSuffix(stockName));
        req.setSymboltoken(symbolToken);
        req.setTransactiontype("SELL");
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

    @Override
    public IOrderRequest modifyStopLossOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double triggerPrice,
            String orderId
    ) {
        ChartinkMIS_SL_Market_OrderRequest req =
                (ChartinkMIS_SL_Market_OrderRequest)
                        createStopLossMarketOrder(stockName, symbolToken, quantity, triggerPrice);

        req.setOrderid(orderId);
        return req;
    }

    @Override
    public IOrderRequest modifyLimitStopLossOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double triggerPrice,
            String orderId,
            double limitPrice
    ) {
        IOrderRequest req =
                        createStopLossLimitOrder(stockName, symbolToken, quantity, triggerPrice, limitPrice);

//        req.setOrderid(orderId);
        return req;
    }

    @Override
    public IOrderRequest createCancelOrder(String orderId, String variety) {
        ChartinkCancelOrderRequest request = new ChartinkCancelOrderRequest(orderId, variety);



        return request;
    }
}

