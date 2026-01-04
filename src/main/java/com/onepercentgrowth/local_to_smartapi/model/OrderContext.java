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
                '}';
    }
}

