package com.onepercentgrowth.local_to_smartapi.model;

import com.onepercentgrowth.local_to_smartapi.enums.PositionSide;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderContext {

    private String buyOrderId;
    private String sellOrderId;
    private String stopLossOrderId;

    private String tradingSymbol;
    private String symbolToken;
    private int quantity;
    private String exchange;

    private String buyVariety;
    private String sellVariety;
    private String stopLossVariety;

    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private BigDecimal stoplossLimitPrice;
    private BigDecimal stoplossTriggerPrice;

//    private int lastBuyFilledQty = 0;
//    private final AtomicInteger lastSellFilledQty = new AtomicInteger(0);
//    private int lastStoplossFilledQty = 0;

    private final AtomicInteger currentSellFilledQty = new AtomicInteger(0);
    private final AtomicInteger currentBuyFilledQty = new AtomicInteger(0);
    private final AtomicInteger currentStoplossFilledQty = new AtomicInteger(0);

    private final AtomicInteger lastSellFilledQty = new AtomicInteger(0);
    private final AtomicInteger lastBuyFilledQty = new AtomicInteger(0);
    private final AtomicInteger lastStoplossFilledQty = new AtomicInteger(0);

    private boolean buyPlaced = false;
    private boolean sellPlaced = false;
    private boolean slPlaced = false;

    private boolean buyOpen = false;
    private boolean sellOpen = false;
    private boolean SLOpen = false;

    private boolean buyPartiallyFilled = false;
    private boolean sellPartiallyFilled = false;
    private boolean SLPartiallyFilled = false;

    private boolean buyCanceled = false;
    private boolean sellCanceled = false;
    private boolean SLCanceled = false;
    private AtomicBoolean tradeCompleted = new AtomicBoolean(false);;
    private PositionSide positionSide;

//    private boolean exitInProgress = false;

    private final AtomicBoolean exitInProgress = new AtomicBoolean(false);
    private final AtomicBoolean entryOrdersPlacing = new AtomicBoolean(false);
    private final AtomicInteger netPositionQty = new AtomicInteger(0);

    private final AtomicInteger remainingQty = new AtomicInteger(0);
    private final List<String> exitOrderIds = new CopyOnWriteArrayList<>();


    private String currentExitOrderId;
    private String currentExitVariety;

    private volatile boolean systemInconsistent = false;

    public OrderContext(
            String buyOrderId,
            String sellOrderId,
            String stopLossOrderId,
            String tradingSymbol,
            String symbolToken,
            int quantity,
            String sellVariety,
            String stopLossVariety
    ) {
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.stopLossOrderId = stopLossOrderId;
        this.tradingSymbol = tradingSymbol;
        this.symbolToken = symbolToken;
        this.quantity = quantity;
        this.sellVariety = sellVariety;
        this.stopLossVariety = stopLossVariety;
    }

    public OrderContext(
            PositionSide positionSide,
            String tradingSymbol,
            String symbolToken,
            int quantity
    ) {
        this.positionSide = positionSide;
        this.tradingSymbol = tradingSymbol;
        this.symbolToken = symbolToken;
        this.quantity = quantity;
    }

    public String getBuyOrderId() {
        return buyOrderId;
    }

    public void setBuyOrderId(String buyOrderId) {
        this.buyOrderId = buyOrderId;
    }

    public String getSellOrderId() {
        return sellOrderId;
    }

    public void setSellOrderId(String sellOrderId) {
        this.sellOrderId = sellOrderId;
    }

    public String getStopLossOrderId() {
        return stopLossOrderId;
    }

    public void setStopLossOrderId(String stopLossOrderId) {
        this.stopLossOrderId = stopLossOrderId;
    }

    public String getTradingSymbol() {
        return tradingSymbol;
    }

    public void setTradingSymbol(String tradingSymbol) {
        this.tradingSymbol = tradingSymbol;
    }

    public String getSymbolToken() {
        return symbolToken;
    }

    public void setSymbolToken(String symbolToken) {
        this.symbolToken = symbolToken;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getBuyVariety() {
        return buyVariety;
    }

    public void setBuyVariety(String buyVariety) {
        this.buyVariety = buyVariety;
    }

    public String getSellVariety() {
        return sellVariety;
    }

    public void setSellVariety(String sellVariety) {
        this.sellVariety = sellVariety;
    }

    public String getStopLossVariety() {
        return stopLossVariety;
    }

    public void setStopLossVariety(String stopLossVariety) {
        this.stopLossVariety = stopLossVariety;
    }

    public BigDecimal getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(BigDecimal buyPrice) {
        this.buyPrice = buyPrice;
    }

    public BigDecimal getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(BigDecimal sellPrice) {
        this.sellPrice = sellPrice;
    }

    public AtomicInteger getCurrentSellFilledQty() {
        return currentSellFilledQty;
    }

    public AtomicInteger getCurrentBuyFilledQty() {
        return currentBuyFilledQty;
    }

    public AtomicInteger getCurrentStoplossFilledQty() {
        return currentStoplossFilledQty;
    }

    public BigDecimal getStoplossLimitPrice() {
        return stoplossLimitPrice;
    }

    public void setStoplossLimitPrice(BigDecimal stoplossLimitPrice) {
        this.stoplossLimitPrice = stoplossLimitPrice;
    }

    public BigDecimal getStoplossTriggerPrice() {
        return stoplossTriggerPrice;
    }

    public void setStoplossTriggerPrice(BigDecimal stoplossTriggerPrice) {
        this.stoplossTriggerPrice = stoplossTriggerPrice;
    }

    public AtomicInteger getLastSellFilledQty() {
        return lastSellFilledQty;
    }

    public AtomicInteger getLastBuyFilledQty() {
        return lastBuyFilledQty;
    }

    public AtomicInteger getLastStoplossFilledQty() {
        return lastStoplossFilledQty;
    }

    public boolean isBuyPlaced() {
        return buyPlaced;
    }

    public void setBuyPlaced(boolean buyPlaced) {
        this.buyPlaced = buyPlaced;
    }

    public boolean isSellPlaced() {
        return sellPlaced;
    }

    public void setSellPlaced(boolean sellPlaced) {
        this.sellPlaced = sellPlaced;
    }

    public boolean isSlPlaced() {
        return slPlaced;
    }

    public void setSlPlaced(boolean slPlaced) {
        this.slPlaced = slPlaced;
    }

    public boolean isBuyOpen() {
        return buyOpen;
    }

    public void setBuyOpen(boolean buyOpen) {
        this.buyOpen = buyOpen;
    }

    public boolean isSellOpen() {
        return sellOpen;
    }

    public void setSellOpen(boolean sellOpen) {
        this.sellOpen = sellOpen;
    }

    public boolean isSLOpen() {
        return SLOpen;
    }

    public void setSLOpen(boolean SLOpen) {
        this.SLOpen = SLOpen;
    }

    public boolean isBuyCanceled() {
        return buyCanceled;
    }

    public void setBuyCanceled(boolean buyCanceled) {
        this.buyCanceled = buyCanceled;
    }

    public boolean isSellCanceled() {
        return sellCanceled;
    }

    public void setSellCanceled(boolean sellCanceled) {
        this.sellCanceled = sellCanceled;
    }

    public boolean isSLCanceled() {
        return SLCanceled;
    }

    public void setSLCanceled(boolean SLCanceled) {
        this.SLCanceled = SLCanceled;
    }

//    public boolean isTradeCompleted() {
//        return tradeCompleted;
//    }
//
//    public void setTradeCompleted(boolean tradeCompleted) {
//        this.tradeCompleted = tradeCompleted;
//    }


    public AtomicBoolean getTradeCompleted() {
        return tradeCompleted;
    }

    public void setTradeCompleted(AtomicBoolean tradeCompleted) {
        this.tradeCompleted = tradeCompleted;
    }

    public PositionSide getPositionSide() {
        return positionSide;
    }

    public void setPositionSide(PositionSide positionSide) {
        this.positionSide = positionSide;
    }

    public boolean isLong() {
        return positionSide == PositionSide.LONG;
    }

    public boolean isShort() {
        return positionSide == PositionSide.SHORT;
    }

    public String getEntryOrderId() {
        return isLong() ? buyOrderId : sellOrderId;
    }

    public String getTargetOrderId() {
        return isLong() ? sellOrderId : buyOrderId;
    }

    public AtomicBoolean getExitInProgress() {
        return exitInProgress;
    }

    public String getCurrentExitOrderId() {
        return currentExitOrderId;
    }

    public void setCurrentExitOrderId(String currentExitOrderId) {
        this.currentExitOrderId = currentExitOrderId;
    }

    public String getCurrentExitVariety() {
        return currentExitVariety;
    }

    public void setCurrentExitVariety(String currentExitVariety) {
        this.currentExitVariety = currentExitVariety;
    }

    public List<String> getExitOrderIds() {
        return exitOrderIds;
    }

    public AtomicInteger getNetPositionQty() {
        return netPositionQty;
    }

    public AtomicInteger getRemainingQty() {
        return remainingQty;
    }

    public boolean isSystemInconsistent() {
        return systemInconsistent;
    }

    public void setSystemInconsistent(boolean systemInconsistent) {
        this.systemInconsistent = systemInconsistent;
    }

    public boolean isBuyPartiallyFilled() {
        return buyPartiallyFilled;
    }

    public void setBuyPartiallyFilled(boolean buyPartiallyFilled) {
        this.buyPartiallyFilled = buyPartiallyFilled;
    }

    public boolean isSellPartiallyFilled() {
        return sellPartiallyFilled;
    }

    public void setSellPartiallyFilled(boolean sellPartiallyFilled) {
        this.sellPartiallyFilled = sellPartiallyFilled;
    }

    public boolean isSLPartiallyFilled() {
        return SLPartiallyFilled;
    }

    public void setSLPartiallyFilled(boolean SLPartiallyFilled) {
        this.SLPartiallyFilled = SLPartiallyFilled;
    }

    public void addExitOrder(String orderId, String variety) {
        exitOrderIds.add(orderId);
        currentExitOrderId = orderId;
        currentExitVariety = variety;
    }

    public boolean tryStartExit() {
        return exitInProgress.compareAndSet(false, true);
    }

    public boolean tryBeginEntryOrders() {
        return entryOrdersPlacing.compareAndSet(false, true);
    }

    public void endExit() {
        exitInProgress.set(false);
    }

    public void markInconsistent() {
        this.systemInconsistent = true;
    }


    public int longBuyRemainingQty(int filledQty)
    {
//        lastBuyFilledQty.set(filledQty);
        remainingQty.set(remainingQty.addAndGet(filledQty));
        return remainingQty.get();
    }

    public int longSellRemainingQty(int filledQty)
    {
//        lastSellFilledQty.set(filledQty);
        remainingQty.set(remainingQty.addAndGet(-filledQty));
        return remainingQty.get();
    }

    public int shortBuyRemainingQty(int filledQty)
    {
//        lastBuyFilledQty.set(filledQty);
        remainingQty.set(remainingQty.addAndGet(-filledQty));
        return remainingQty.get();
    }

    public int shortSellRemainingQty(int filledQty)
    {
//        lastSellFilledQty.set(filledQty);
        remainingQty.set(remainingQty.addAndGet(filledQty));
        return remainingQty.get();
    }

    public int stopLossRemainingQty(int filledQty)
    {
//        lastStoplossFilledQty.set(filledQty);
        remainingQty.set(remainingQty.addAndGet(-filledQty));
        return remainingQty.get();
    }

    public static class SellUpdate {
        public final int delta;
        public final int remainingQty;

        public SellUpdate(int delta, int remainingQty) {
            this.delta = delta;
            this.remainingQty = remainingQty;
        }
    }

//    public synchronized int safeReduceSell(int filledQty) {
//        int previous = lastSellFilledQty.get();
//        int delta = filledQty - previous;
//
//        if (delta <= 0) return netPositionQty.get();
//
//        lastSellFilledQty.set(filledQty);
//        return netPositionQty.addAndGet(-delta);
//    }

    public synchronized SellUpdate reduceSell(int filledQty) {

        int previous = lastSellFilledQty.get();
        int delta = filledQty - previous;

        if (delta <= 0) {
            return new SellUpdate(0, netPositionQty.get());
        }

        lastSellFilledQty.set(filledQty);

        int remainingQty;

        if(netPositionQty.get() < lastSellFilledQty.get())
            remainingQty = netPositionQty.addAndGet(lastSellFilledQty.get()); // 🔥 CRITICAL
        else
            remainingQty = netPositionQty.addAndGet(-lastSellFilledQty.get());

//         = netPositionQty.addAndGet(delta);

        return new SellUpdate(delta, remainingQty);
    }

    public static class StopLossUpdate {
        public final int delta;
        public final int remainingQty;

        public StopLossUpdate(int delta, int remainingQty) {
            this.delta = delta;
            this.remainingQty = remainingQty;
        }
    }

    public synchronized StopLossUpdate reduceStopLoss(int filledQty) {

        int previous = lastStoplossFilledQty.get();
        int delta = filledQty - previous;

        if (delta <= 0) {
            return new StopLossUpdate(0, netPositionQty.get());
        }

        lastStoplossFilledQty.set(filledQty);

        int remainingQty;

        if(netPositionQty.get() < lastStoplossFilledQty.get())
            remainingQty = netPositionQty.addAndGet(lastStoplossFilledQty.get()); // 🔥 CRITICAL
        else
            remainingQty = netPositionQty.addAndGet(-lastStoplossFilledQty.get());

        return new StopLossUpdate(delta, remainingQty);
    }

    public static class BuyUpdate {
        public final int delta;
        public final int remainingQty;

        public BuyUpdate(int delta, int remainingQty) {
            this.delta = delta;
            this.remainingQty = remainingQty;
        }
    }

    public synchronized BuyUpdate reduceBuy(int filledQty) {

        int previous = lastBuyFilledQty.get();
        int delta = filledQty - previous;

        if (delta <= 0) {
            return new BuyUpdate(0, netPositionQty.get());
        }

        lastBuyFilledQty.set(filledQty);

        int remainingQty;

        if(netPositionQty.get() < lastBuyFilledQty.get())
             remainingQty = netPositionQty.addAndGet(lastBuyFilledQty.get()); // 🔥 CRITICAL
        else
            remainingQty = netPositionQty.addAndGet(-lastBuyFilledQty.get());

        return new BuyUpdate(delta, remainingQty);
    }

//    public synchronized boolean tryStartExit() {
//        if (exitInProgress) {
//            return false;
//        }
//        exitInProgress = true;
//        return true;
//    }

    @Override
    public String toString() {
        return "OrderContext{" +
                "buyOrderId='" + buyOrderId + '\'' +
                ", sellOrderId='" + sellOrderId + '\'' +
                ", stopLossOrderId='" + stopLossOrderId + '\'' +
                ", tradingSymbol='" + tradingSymbol + '\'' +
                ", symbolToken='" + symbolToken + '\'' +
                ", quantity=" + quantity +
                ", exchange='" + exchange + '\'' +
                ", buyVariety='" + buyVariety + '\'' +
                ", sellVariety='" + sellVariety + '\'' +
                ", stopLossVariety='" + stopLossVariety + '\'' +
                ", buyPrice=" + buyPrice +
                ", sellPrice=" + sellPrice +
                ", stoplossLimitPrice=" + stoplossLimitPrice +
                ", stoplossTriggerPrice=" + stoplossTriggerPrice +
                ", currentSellFilledQty=" + currentSellFilledQty +
                ", currentBuyFilledQty=" + currentBuyFilledQty +
                ", currentStoplossFilledQty=" + currentStoplossFilledQty +
                ", lastSellFilledQty=" + lastSellFilledQty +
                ", lastBuyFilledQty=" + lastBuyFilledQty +
                ", lastStoplossFilledQty=" + lastStoplossFilledQty +
                ", buyPlaced=" + buyPlaced +
                ", sellPlaced=" + sellPlaced +
                ", slPlaced=" + slPlaced +
                ", buyOpen=" + buyOpen +
                ", sellOpen=" + sellOpen +
                ", SLOpen=" + SLOpen +
                ", buyCanceled=" + buyCanceled +
                ", sellCanceled=" + sellCanceled +
                ", SLCanceled=" + SLCanceled +
                ", tradeCompleted=" + tradeCompleted +
                ", positionSide=" + positionSide +
                ", exitInProgress=" + exitInProgress +
                ", netPositionQty=" + netPositionQty +
                ", exitOrderIds=" + exitOrderIds +
                ", currentExitOrderId='" + currentExitOrderId + '\'' +
                ", currentExitVariety='" + currentExitVariety + '\'' +
                ", systemInconsistent=" + systemInconsistent +
                '}';
    }
}

