package com.onepercentgrowth.local_to_smartapi.model;

import java.math.BigDecimal;

public class MarketQuote {

    private BigDecimal ltp;
    private BigDecimal bestBid;
    private BigDecimal bestAsk;

    public MarketQuote(BigDecimal ltp, BigDecimal bestBid, BigDecimal bestAsk) {
        this.ltp = ltp;
        this.bestBid = bestBid;
        this.bestAsk = bestAsk;
    }

    public BigDecimal getLtp() {
        return ltp;
    }

    public BigDecimal getBestBid() {
        return bestBid;
    }

    public BigDecimal getBestAsk() {
        return bestAsk;
    }
}
