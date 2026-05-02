package com.onepercentgrowth.local_to_smartapi.historicdata;

public class HistoricalDataRequest  {
    private String exchange;
    private String symboltoken;

    private String interval;

    private String fromdate;

    private String todate;

    public HistoricalDataRequest() {
    }

    public HistoricalDataRequest(String exchange, String symbolToken, String interval, String fromDate, String toDate) {
        this.exchange = exchange;
        this.symboltoken = symbolToken;
        this.interval = interval;
        this.fromdate = fromDate;
        this.todate = toDate;
    }



    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getSymbolToken() {
        return symboltoken;
    }

    public void setSymbolToken(String symbolToken) {
        this.symboltoken = symbolToken;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getFromDate() {
        return fromdate;
    }

    public void setFromDate(String fromDate) {
        this.fromdate = fromDate;
    }

    public String getToDate() {
        return todate;
    }

    public void setToDate(String toDate) {
        this.todate = toDate;
    }

    @Override
    public String toString() {
        return "HistoricalDataRequest{" +
                "exchange='" + exchange + '\'' +
                ", symboltoken='" + symboltoken + '\'' +
                ", interval='" + interval + '\'' +
                ", fromdate='" + fromdate + '\'' +
                ", todate='" + todate + '\'' +
                '}';
    }
}
