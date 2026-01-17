package com.onepercentgrowth.local_to_smartapi.factory;

import com.onepercentgrowth.local_to_smartapi.model.chartink_request.IOrderRequest;

public interface OrderRequestFactory {

    IOrderRequest createBuyOrder(
            String stockName,
            String symbolToken,
            int quantity,
            String price
    );

    IOrderRequest modifyBuyOrder(
            String stockName,
            String symbolToken,
            int quantity,
            String price,
            String orderId
    );

    IOrderRequest createSellLimitOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double price
    );

    public IOrderRequest modifySellLimitOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double triggerPrice,
            String orderId,
            double limitPrice
    );

//    IOrderRequest createStopLossMarketOrder(
//            String stockName,
//            String symbolToken,
//            int quantity,
//            double triggerPrice
//    );

    IOrderRequest createStopLossLimitOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double triggerPrice,
            double limitPrice
    );


//    IOrderRequest modifyStopLossOrder(
//            String stockName,
//            String symbolToken,
//            int quantity,
//            double triggerPrice,
//            String orderId
//    );

    public IOrderRequest modifyLimitStopLossOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double triggerPrice,
            String orderId,
            double limitPrice
    );

    IOrderRequest createCancelOrder(String orderId, String variety);
}

