package com.onepercentgrowth.local_to_smartapi.historicdata;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class Candle {
    private String timestamp;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;

    private LocalDateTime time;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
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

    public LocalDateTime getTime() {
        if (time == null && timestamp != null) {
            time = OffsetDateTime.parse(timestamp).toLocalDateTime();
        }
        return time;
    }

    @Override
    public String toString() {
        return "Candle{" +
                "timestamp='" + timestamp + '\'' +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", close=" + close +
                ", volume=" + volume +
                '}';
    }
}
