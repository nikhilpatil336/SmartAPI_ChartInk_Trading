package com.onepercentgrowth.local_to_smartapi.model;

import java.math.BigDecimal;

public class ScripMasterRecord {
    String token;
    BigDecimal tickSize;
    int lotSize;
    String symbol;
    String exchange;

    public ScripMasterRecord() {
    }

    public ScripMasterRecord(String token, BigDecimal tickSize, int lotSize, String symbol, String exchange) {
        this.token = token;
        this.tickSize = tickSize;
        this.lotSize = lotSize;
        this.symbol = symbol;
        this.exchange = exchange;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public BigDecimal getTickSize() {
        return tickSize;
    }

    public void setTickSize(BigDecimal tickSize) {
        this.tickSize = tickSize;
    }

    public int getLotSize() {
        return lotSize;
    }

    public void setLotSize(int lotSize) {
        this.lotSize = lotSize;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }
}
