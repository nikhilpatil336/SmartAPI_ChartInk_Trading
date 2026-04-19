package com.onepercentgrowth.local_to_smartapi.utility;

import com.onepercentgrowth.local_to_smartapi.model.*;

import java.util.List;

public class OrderContextMapper {

    public static OrderContextFileModel toFileModel(OrderContext ctx) {

        List<ExitOrder> exitOrders =
                ctx.getExitOrderIds().stream()
                        .map(id -> new ExitOrder(id, ctx.getCurrentExitVariety()))
                        .toList();

        return new OrderContextFileModel(
                ctx.getBuyOrderId(),
                ctx.getSellOrderId(),
                ctx.getStopLossOrderId(),

                ctx.getTradingSymbol(),
                ctx.getSymbolToken(),
                ctx.getQuantity(),
                ctx.getExchange(),

                ctx.getBuyVariety(),
                ctx.getSellVariety(),
                ctx.getStopLossVariety(),

                ctx.getBuyPrice(),
                ctx.getSellPrice(),
                ctx.getStoplossLimitPrice(),
                ctx.getStoplossTriggerPrice(),

                ctx.getNetPositionQty().get(),
                ctx.getTradeCompleted().get(),
                ctx.getExitInProgress().get(),

                exitOrders,   // ✅ UPDATED

                ctx.getPositionSide().name()
        );
    }

    public static OrderContext toDomain(OrderContextFileModel model) {

        OrderContext ctx = new OrderContext(
                model.buyOrderId(),
                model.sellOrderId(),
                model.stopLossOrderId(),
                model.tradingSymbol(),
                model.symbolToken(),
                model.quantity(),
                model.sellVariety(),
                model.stopLossVariety()
        );

        ctx.setExchange(model.exchange());
        ctx.setBuyVariety(model.buyVariety());

        ctx.setBuyPrice(model.buyPrice());
        ctx.setSellPrice(model.sellPrice());
        ctx.setStoplossLimitPrice(model.stoplossLimitPrice());
        ctx.setStoplossTriggerPrice(model.stoplossTriggerPrice());

        ctx.getNetPositionQty().set(model.netPositionQty());
        ctx.getTradeCompleted().set(model.tradeCompleted());
        ctx.getExitInProgress().set(model.exitInProgress());

        ctx.setPositionSide(
                Enum.valueOf(
                        com.onepercentgrowth.local_to_smartapi.enums.PositionSide.class,
                        model.positionSide()
                )
        );

        // 🔥 RESTORE EXIT ORDERS CORRECTLY
        if (model.exitOrders() != null) {
            model.exitOrders().forEach(e ->
                    ctx.addExitOrder(e.orderId(), e.variety())
            );
        }

        return ctx;
    }
}