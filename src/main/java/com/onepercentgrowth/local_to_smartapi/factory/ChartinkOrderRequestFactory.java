package com.onepercentgrowth.local_to_smartapi.factory;

import com.onepercentgrowth.local_to_smartapi.model.chartink_request.*;
import com.onepercentgrowth.local_to_smartapi.service.OrderService_v2;
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
        req.setTradingsymbol(stockName + "-EQ");
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
    public IOrderRequest createSellOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double price
    ) {
        ChartinkMISSellOrderRequest req = new ChartinkMISSellOrderRequest();
        req.setVariety("NORMAL");
        req.setTradingsymbol(stockName + "-EQ");
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
    public IOrderRequest createStopLossOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double triggerPrice
    ) {
        log.info("Trigger price for stoploss is {}", triggerPrice);

        ChartinkMIS_SL_OrderRequest req = new ChartinkMIS_SL_OrderRequest();
        req.setVariety("STOPLOSS");
        req.setTradingsymbol(stockName + "-EQ");
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
    public IOrderRequest modifyStopLossOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double triggerPrice,
            String orderId
    ) {
        ChartinkMIS_SL_OrderRequest req =
                (ChartinkMIS_SL_OrderRequest)
                        createStopLossOrder(stockName, symbolToken, quantity, triggerPrice);

        req.setOrderid(orderId);
        return req;
    }

    @Override
    public IOrderRequest createCancelOrder(String orderId, String variety) {
        ChartinkCancelOrderRequest request = new ChartinkCancelOrderRequest(orderId, variety);



        return request;
    }
}

