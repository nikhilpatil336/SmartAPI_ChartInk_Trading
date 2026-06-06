package com.onepercentgrowth.local_to_smartapi.helper;

import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusData;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;

public class OrderStatusResponseFactory {

    public static OrderStatusResponse buyComplete(String orderId, int filledQty, String avgPrice) {
        return build(orderId, "BUY", "complete", String.valueOf(filledQty), avgPrice, avgPrice);
    }

    public static OrderStatusResponse buyOpen(String orderId, int filledQty, String avgPrice) {
        return build(orderId, "BUY", "open", String.valueOf(filledQty), avgPrice, avgPrice);
    }

    public static OrderStatusResponse shortEntryComplete(String orderId, int filledQty, String avgPrice) {
        return build(orderId, "SELL", "complete", String.valueOf(filledQty), avgPrice, avgPrice);
    }

    public static OrderStatusResponse shortEntryOpen(String orderId, int filledQty, String avgPrice) {
        return build(orderId, "SELL", "open", String.valueOf(filledQty), avgPrice, avgPrice);
    }

    public static OrderStatusResponse sellComplete(String orderId, int filledQty, String avgPrice) {
        return build(orderId, "SELL", "complete", String.valueOf(filledQty), avgPrice, avgPrice);
    }

    public static OrderStatusResponse sellOpen(String orderId, int filledQty, String avgPrice) {
        return build(orderId, "SELL", "open", String.valueOf(filledQty), avgPrice, avgPrice);
    }

    private static OrderStatusResponse build(
            String orderId,
            String transactionType,
            String status,
            String filledShares,
            String price,
            String avgPrice
    ) {
        OrderStatusData data = new OrderStatusData();
        data.setOrderid(orderId);
        data.setTransactiontype(transactionType);
        data.setStatus(status);
        data.setFilledshares(filledShares);
        data.setPrice(price);
        data.setAverageprice(avgPrice);
        data.setQuantity(filledShares);

        OrderStatusResponse response = new OrderStatusResponse();
        response.setOrderStatusData(data);
        return response;
    }
}
