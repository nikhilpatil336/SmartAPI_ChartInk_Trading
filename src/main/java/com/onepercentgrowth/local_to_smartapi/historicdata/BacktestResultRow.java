package com.onepercentgrowth.local_to_smartapi.historicdata;

public class BacktestResultRow {

    private String triggeredAt;
    private String stock;

    private double open, high, low, close;
    private long volume;

    private String color;
    private long prevVolume;

    private double sma10, sma20;

    private boolean volGtSma10, volGtSma20;
    private boolean volLtSma10, volLtSma20;

    private String tradeDecision;

    public String getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(String triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public long getPrevVolume() {
        return prevVolume;
    }

    public void setPrevVolume(long prevVolume) {
        this.prevVolume = prevVolume;
    }

    public double getSma10() {
        return sma10;
    }

    public void setSma10(double sma10) {
        this.sma10 = sma10;
    }

    public double getSma20() {
        return sma20;
    }

    public void setSma20(double sma20) {
        this.sma20 = sma20;
    }

    public boolean isVolGtSma10() {
        return volGtSma10;
    }

    public void setVolGtSma10(boolean volGtSma10) {
        this.volGtSma10 = volGtSma10;
    }

    public boolean isVolGtSma20() {
        return volGtSma20;
    }

    public void setVolGtSma20(boolean volGtSma20) {
        this.volGtSma20 = volGtSma20;
    }

    public boolean isVolLtSma10() {
        return volLtSma10;
    }

    public void setVolLtSma10(boolean volLtSma10) {
        this.volLtSma10 = volLtSma10;
    }

    public boolean isVolLtSma20() {
        return volLtSma20;
    }

    public void setVolLtSma20(boolean volLtSma20) {
        this.volLtSma20 = volLtSma20;
    }

    public String getTradeDecision() {
        return tradeDecision;
    }

    public void setTradeDecision(String tradeDecision) {
        this.tradeDecision = tradeDecision;
    }
}
