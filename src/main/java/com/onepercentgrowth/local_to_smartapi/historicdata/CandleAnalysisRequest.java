package com.onepercentgrowth.local_to_smartapi.historicdata;

public class CandleAnalysisRequest {

    private String symbol;
    private String date;
    private String interval;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    @Override
    public String toString() {
        return "CandleAnalysisRequest{" +
                "symbol='" + symbol + '\'' +
                ", date='" + date + '\'' +
                ", interval='" + interval + '\'' +
                '}';
    }
}
