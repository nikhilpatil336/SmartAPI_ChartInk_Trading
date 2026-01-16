package com.onepercentgrowth.local_to_smartapi.model;

public class OrderContext {

    private String buyOrderId;
    private String sellOrderId;
    private String stopLossOrderId;

    private String tradingSymbol;
    private String symbolToken;
    private int quantity;

    private String sellVariety;
    private String stopLossVariety;

    private double buyPrice;
    private double sellPrice;
    private double stoplossPrice;

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

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public double getStoplossPrice() {
        return stoplossPrice;
    }

    public void setStoplossPrice(double stoplossPrice) {
        this.stoplossPrice = stoplossPrice;
    }

    public void markSellPlaced(String sellOrderId, double sellPrice) {
        if (this.sellOrderId != null) {
            throw new IllegalStateException("SELL already placed for BUY " + buyOrderId);
        }
        this.sellOrderId = sellOrderId;
        this.sellPrice = sellPrice;
    }

    public void markSlPlaced(String stopLossOrderId, double stoplossPrice) {
        if (this.stopLossOrderId != null) {
            throw new IllegalStateException("STOPLOSS already placed for BUY " + buyOrderId);
        }
        this.stopLossOrderId = stopLossOrderId;
        this.stoplossPrice = stoplossPrice;
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
                '}';
    }
}

