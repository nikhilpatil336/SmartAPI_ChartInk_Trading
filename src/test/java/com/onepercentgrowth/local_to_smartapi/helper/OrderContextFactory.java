package com.onepercentgrowth.local_to_smartapi.helper;

import com.onepercentgrowth.local_to_smartapi.enums.PositionSide;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;

import java.math.BigDecimal;

public class OrderContextFactory {

    public static OrderContext freshLong(String buyOrderId, int qty) {
        OrderContext ctx = new OrderContext(PositionSide.LONG, "RELIANCE", "2885", qty);
        ctx.setBuyOrderId(buyOrderId);
        ctx.setBuyPlaced(true);
        return ctx;
    }

    public static OrderContext freshShort(String sellOrderId, int qty) {
        OrderContext ctx = new OrderContext(PositionSide.SHORT, "RELIANCE", "2885", qty);
        ctx.setSellOrderId(sellOrderId);
        return ctx;
    }

    public static OrderContext longWithCasAlreadyClaimed(String buyOrderId, int qty) {
        OrderContext ctx = freshLong(buyOrderId, qty);
        ctx.tryBeginEntryOrders();
        return ctx;
    }

    public static OrderContext shortWithCasAlreadyClaimed(String sellOrderId, int qty) {
        OrderContext ctx = freshShort(sellOrderId, qty);
        ctx.tryBeginEntryOrders();
        return ctx;
    }

    // LONG context with both exit orders open — used by SellFilled and StopLossFilled tests
    public static OrderContext longWithSellAndSlOpen(int qty) {
        OrderContext ctx = new OrderContext(PositionSide.LONG, "RELIANCE", "2885", qty);
        ctx.setBuyOrderId("BUY-001");
        ctx.setBuyPlaced(true);
        ctx.setBuyPrice(new BigDecimal("100.00"));
        ctx.setSellOrderId("SELL-001");
        ctx.setSellPlaced(true);
        ctx.setSellOpen(true);
        ctx.setSellVariety("NORMAL");
        ctx.setStopLossOrderId("SL-001");
        ctx.setSlPlaced(true);
        ctx.setSLOpen(true);
        ctx.setStopLossVariety("STOPLOSS");
        return ctx;
    }

    // SHORT context with both exit orders open — used by ShortTargetFilled and ShortStopLossFilled tests
    public static OrderContext shortWithBuyAndSlOpen(int qty) {
        OrderContext ctx = new OrderContext(PositionSide.SHORT, "RELIANCE", "2885", qty);
        ctx.setSellOrderId("SELL-001");
        ctx.setSellPrice(new BigDecimal("100.00"));
        ctx.setBuyOrderId("BUY-001");
        ctx.setBuyPlaced(true);
        ctx.setBuyOpen(true);
        ctx.setBuyVariety("NORMAL");
        ctx.setStopLossOrderId("SL-001");
        ctx.setSlPlaced(true);
        ctx.setSLOpen(true);
        ctx.setStopLossVariety("STOPLOSS");
        return ctx;
    }

    // SHORT entry: sell order is open on exchange but no partial fill event was processed yet.
    // Triggers the unique guard in ShortEntryFilledOrderStrategy:
    //   if (!isSellPartiallyFilled() && isSellOpen()) → return empty
    public static OrderContext shortSellOpenNotPartialFilled(String sellOrderId, int qty) {
        OrderContext ctx = new OrderContext(PositionSide.SHORT, "RELIANCE", "2885", qty);
        ctx.setSellOrderId(sellOrderId);
        ctx.setSellOpen(true);
        // sellPartiallyFilled stays false intentionally
        return ctx;
    }

    // LONG entry: buy order is open on exchange but no partial fill event was processed yet.
    // Triggers the unique guard in BuyFilledOrderStrategy:
    //   if (!isBuyPartiallyFilled() && isBuyOpen()) → return empty
    public static OrderContext longBuyOpenNotPartialFilled(String buyOrderId, int qty) {
        OrderContext ctx = new OrderContext(PositionSide.LONG, "RELIANCE", "2885", qty);
        ctx.setBuyOrderId(buyOrderId);
        ctx.setBuyOpen(true);
        // buyPartiallyFilled stays false intentionally
        return ctx;
    }

    // LONG context ready for a partial SELL fill — SL trigger/limit prices set for modifyStopLossOrder
    public static OrderContext longReadyForSellOpenFill(int qty) {
        OrderContext ctx = new OrderContext(PositionSide.LONG, "RELIANCE", "2885", qty);
        ctx.setBuyOrderId("BUY-001"); ctx.setBuyPlaced(true); ctx.setBuyPrice(new BigDecimal("100.00"));
        ctx.setSellOrderId("SELL-001"); ctx.setSellPlaced(true); ctx.setSellOpen(true); ctx.setSellVariety("NORMAL");
        ctx.setStopLossOrderId("SL-001"); ctx.setSlPlaced(true); ctx.setSLOpen(true); ctx.setStopLossVariety("STOPLOSS");
        ctx.setStoplossTriggerPrice(new BigDecimal("99.00"));
        ctx.setStoplossLimitPrice(new BigDecimal("98.90"));
        return ctx;
    }

    // LONG context ready for a partial SL fill — sellPrice + remainingQty pre-seeded.
    // stopLossRemainingQty(delta) does remainingQty.addAndGet(-delta); must start at qty or guard fails.
    public static OrderContext longReadyForSlOpenFill(int qty) {
        OrderContext ctx = new OrderContext(PositionSide.LONG, "RELIANCE", "2885", qty);
        ctx.setBuyOrderId("BUY-001"); ctx.setBuyPlaced(true); ctx.setBuyPrice(new BigDecimal("100.00"));
        ctx.setSellOrderId("SELL-001"); ctx.setSellPlaced(true); ctx.setSellOpen(true); ctx.setSellVariety("NORMAL");
        ctx.setSellPrice(new BigDecimal("101.00"));
        ctx.setStopLossOrderId("SL-001"); ctx.setSlPlaced(true); ctx.setSLOpen(true); ctx.setStopLossVariety("STOPLOSS");
        ctx.getRemainingQty().set(qty);
        return ctx;
    }

    // SHORT context ready for a partial BUY target fill — SL trigger/limit prices for modifyStopLossOrder
    public static OrderContext shortReadyForTargetOpenFill(int qty) {
        OrderContext ctx = new OrderContext(PositionSide.SHORT, "RELIANCE", "2885", qty);
        ctx.setSellOrderId("SELL-001"); ctx.setSellPlaced(true); ctx.setSellVariety("NORMAL");
        ctx.setSellPrice(new BigDecimal("100.00"));
        ctx.setBuyOrderId("BUY-001"); ctx.setBuyPlaced(true); ctx.setBuyOpen(true); ctx.setBuyVariety("NORMAL");
        ctx.setStopLossOrderId("SL-001"); ctx.setSlPlaced(true); ctx.setSLOpen(true); ctx.setStopLossVariety("STOPLOSS");
        ctx.setStoplossTriggerPrice(new BigDecimal("101.00"));
        ctx.setStoplossLimitPrice(new BigDecimal("101.10"));
        return ctx;
    }

    // SHORT context ready for a partial SL fill — buyPrice + remainingQty pre-seeded.
    // stopLossRemainingQty(delta) does remainingQty.addAndGet(-delta); must start at qty or guard fails.
    public static OrderContext shortReadyForSlOpenFill(int qty) {
        OrderContext ctx = new OrderContext(PositionSide.SHORT, "RELIANCE", "2885", qty);
        ctx.setSellOrderId("SELL-001"); ctx.setSellPlaced(true); ctx.setSellVariety("NORMAL");
        ctx.setSellPrice(new BigDecimal("100.00"));
        ctx.setBuyOrderId("BUY-001"); ctx.setBuyPlaced(true); ctx.setBuyOpen(true); ctx.setBuyVariety("NORMAL");
        ctx.setBuyPrice(new BigDecimal("99.00"));
        ctx.setStopLossOrderId("SL-001"); ctx.setSlPlaced(true); ctx.setSLOpen(true); ctx.setStopLossVariety("STOPLOSS");
        ctx.getRemainingQty().set(qty);
        return ctx;
    }
}
