package com.onepercentgrowth.local_to_smartapi.model;

import java.math.BigDecimal;

public class OrderContext {

    private String buyOrderId;
    private String sellOrderId;
    private String stopLossOrderId;

    private String tradingSymbol;
    private String symbolToken;
    private int quantity;

    private String sellVariety;
    private String stopLossVariety;

    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private BigDecimal stoplossPrice;

    private int lastBuyFilledQty = 0;
    private int lastSellFilledQty = 0;
    private int lastStoplossFilledQty = 0;
    private boolean sellPlaced = false;
    private boolean slPlaced = false;


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
            String buyOrderId,
            String tradingSymbol,
            String symbolToken,
            int quantity
    ) {
        this.buyOrderId = buyOrderId;
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

//    public double getBuyPrice() {
//        return buyPrice;
//    }
//
//    public void setBuyPrice(double buyPrice) {
//        this.buyPrice = buyPrice;
//    }
//
//    public double getSellPrice() {
//        return sellPrice;
//    }
//
//    public void setSellPrice(double sellPrice) {
//        this.sellPrice = sellPrice;
//    }
//
//    public double getStoplossPrice() {
//        return stoplossPrice;
//    }
//
//    public void setStoplossPrice(double stoplossPrice) {
//        this.stoplossPrice = stoplossPrice;
//    }


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

    public BigDecimal getStoplossPrice() {
        return stoplossPrice;
    }

    public void setStoplossPrice(BigDecimal stoplossPrice) {
        this.stoplossPrice = stoplossPrice;
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

    @Override
    public String toString() {
        return "OrderContext{" +
                "buyOrderId='" + buyOrderId + '\'' +
                ", sellOrderId='" + sellOrderId + '\'' +
                ", stopLossOrderId='" + stopLossOrderId + '\'' +
                ", tradingSymbol='" + tradingSymbol + '\'' +
                ", symbolToken='" + symbolToken + '\'' +
                ", quantity=" + quantity +
                ", sellVariety='" + sellVariety + '\'' +
                ", stopLossVariety='" + stopLossVariety + '\'' +
                ", buyPrice=" + buyPrice +
                ", sellPrice=" + sellPrice +
                ", stoplossPrice=" + stoplossPrice +
                ", lastBuyFilledQty=" + lastBuyFilledQty +
                ", lastSellFilledQty=" + lastSellFilledQty +
                ", lastStoplossFilledQty=" + lastStoplossFilledQty +
                ", sellPlaced=" + sellPlaced +
                ", slPlaced=" + slPlaced +
                '}';
    }
}

