package com.onepercentgrowth.local_to_smartapi.historicdata;

public class CandleAnalysisResponse {
    private Candle currentCandle;
    private Candle previousCandle;
    private String previousCandleColor; // GREEN / RED
    private double sma10Volume;
    private double sma20Volume;

    private String tradeDecision;

    public Candle getCurrentCandle() {
        return currentCandle;
    }

    public void setCurrentCandle(Candle currentCandle) {
        this.currentCandle = currentCandle;
    }

    public Candle getPreviousCandle() {
        return previousCandle;
    }

    public void setPreviousCandle(Candle previousCandle) {
        this.previousCandle = previousCandle;
    }

    public String getPreviousCandleColor() {
        return previousCandleColor;
    }

    public void setPreviousCandleColor(String previousCandleColor) {
        this.previousCandleColor = previousCandleColor;
    }

    public double getSma10Volume() {
        return sma10Volume;
    }

    public void setSma10Volume(double sma10Volume) {
        this.sma10Volume = sma10Volume;
    }

    public double getSma20Volume() {
        return sma20Volume;
    }

    public void setSma20Volume(double sma20Volume) {
        this.sma20Volume = sma20Volume;
    }

    public String getTradeDecision() {
        return tradeDecision;
    }

    public void setTradeDecision(String tradeDecision) {
        this.tradeDecision = tradeDecision;
    }
}
