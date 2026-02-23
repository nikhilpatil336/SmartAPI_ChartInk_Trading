package com.onepercentgrowth.local_to_smartapi.model;

import com.onepercentgrowth.local_to_smartapi.enums.PositionSide;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

public class OrderContext {

    private String buyOrderId;
    private String sellOrderId;
    private String stopLossOrderId;

    private String tradingSymbol;
    private String symbolToken;
    private int quantity;

    private String buyVariety;
    private String sellVariety;
    private String stopLossVariety;

    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private BigDecimal stoplossLimitPrice;
    private BigDecimal stoplossTriggerPrice;

    private int lastBuyFilledQty = 0;
    private int lastSellFilledQty = 0;
    private int lastStoplossFilledQty = 0;

    private boolean buyPlaced = false;
    private boolean sellPlaced = false;
    private boolean slPlaced = false;

    private boolean buyOpen = false;
    private boolean sellOpen = false;
    private boolean SLOpen = false;

    private boolean buyCanceled = false;
    private boolean sellCanceled = false;
    private boolean SLCanceled = false;
    private boolean tradeCompleted;
    private PositionSide positionSide;

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

//    public OrderContext(
////            String buyOrderId,
//            String tradingSymbol,
//            String symbolToken,
//            int quantity
//    ) {
////        this.buyOrderId = buyOrderId;
//        this.tradingSymbol = tradingSymbol;
//        this.symbolToken = symbolToken;
//        this.quantity = quantity;
//    }

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

    public int getLastBuyFilledQty() {
        return lastBuyFilledQty;
    }

    public void setLastBuyFilledQty(int lastBuyFilledQty) {
        this.lastBuyFilledQty = lastBuyFilledQty;
    }

    public int getLastSellFilledQty() {
        return lastSellFilledQty;
    }

    public void setLastSellFilledQty(int lastSellFilledQty) {
        this.lastSellFilledQty = lastSellFilledQty;
    }

    public int getLastStoplossFilledQty() {
        return lastStoplossFilledQty;
    }

    public void setLastStoplossFilledQty(int lastStoplossFilledQty) {
        this.lastStoplossFilledQty = lastStoplossFilledQty;
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

    public boolean isTradeCompleted() {
        return tradeCompleted;
    }

    public void setTradeCompleted(boolean tradeCompleted) {
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

    @Override
    public String toString() {
        return "OrderContext{" +
                "buyOrderId='" + buyOrderId + '\'' +
                ", sellOrderId='" + sellOrderId + '\'' +
                ", stopLossOrderId='" + stopLossOrderId + '\'' +
                ", tradingSymbol='" + tradingSymbol + '\'' +
                ", symbolToken='" + symbolToken + '\'' +
                ", quantity=" + quantity +
                ", buyVariety='" + buyVariety + '\'' +
                ", sellVariety='" + sellVariety + '\'' +
                ", stopLossVariety='" + stopLossVariety + '\'' +
                ", buyPrice=" + buyPrice +
                ", sellPrice=" + sellPrice +
                ", stoplossLimitPrice=" + stoplossLimitPrice +
                ", stoplossTriggerPrice=" + stoplossTriggerPrice +
                ", lastBuyFilledQty=" + lastBuyFilledQty +
                ", lastSellFilledQty=" + lastSellFilledQty +
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
                '}';
    }
}

