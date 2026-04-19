package com.onepercentgrowth.local_to_smartapi.model;

import java.math.BigDecimal;
import java.util.List;

public record OrderContextFileModel(
        String buyOrderId,
        String sellOrderId,
        String stopLossOrderId,

        String tradingSymbol,
        String symbolToken,
        int quantity,
        String exchange,

        String buyVariety,
        String sellVariety,
        String stopLossVariety,

        BigDecimal buyPrice,
        BigDecimal sellPrice,
        BigDecimal stoplossLimitPrice,
        BigDecimal stoplossTriggerPrice,

        int netPositionQty,
        boolean tradeCompleted,
        boolean exitInProgress,

        List<ExitOrder> exitOrders,

        String positionSide
) {}
