package com.onepercentgrowth.local_to_smartapi.factory;

import com.onepercentgrowth.local_to_smartapi.model.chartink_request.IOrderRequest;

public interface OrderRequestFactory {

    IOrderRequest createBuyOrder(
            String stockName,
            String symbolToken,
            int quantity,
            String price
    );

    IOrderRequest createSellOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double price
    );

    IOrderRequest createStopLossOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double triggerPrice
    );

    IOrderRequest modifyStopLossOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double triggerPrice,
            String orderId
    );

    IOrderRequest createCancelOrder(String orderId, String variety);
}

