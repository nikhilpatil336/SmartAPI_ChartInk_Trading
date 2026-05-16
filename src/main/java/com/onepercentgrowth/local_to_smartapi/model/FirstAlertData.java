package com.onepercentgrowth.local_to_smartapi.model;

import com.onepercentgrowth.local_to_smartapi.enums.AlertState;

import java.time.LocalDateTime;
import java.util.Deque;
import java.util.concurrent.ScheduledFuture;

public class FirstAlertData {

    private String symbol;
    private LocalDateTime triggeredAt;
    private double firstPrice;
    private double secondPrice;
    private double sma10;
    private double sma20;

    private AlertState alertState;

    private ScheduledFuture<?> fallbackTask;

    public FirstAlertData() {
    }

    public FirstAlertData(String symbol, LocalDateTime triggeredAt, double firstPrice, double secondPrice, double sma10, double sma20, AlertState alertState) {
        this.symbol = symbol;
        this.triggeredAt = triggeredAt;
        this.firstPrice = firstPrice;
        this.secondPrice = secondPrice;
        this.sma10 = sma10;
        this.sma20 = sma20;
        this.alertState = alertState;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(LocalDateTime triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public double getFirstPrice() {
        return firstPrice;
    }

    public void setFirstPrice(double firstPrice) {
        this.firstPrice = firstPrice;
    }

    public double getSecondPrice() {
        return secondPrice;
    }

    public void setSecondPrice(double secondPrice) {
        this.secondPrice = secondPrice;
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

    public AlertState getAlertState() {
        return alertState;
    }

    public void setAlertState(AlertState alertState) {
        this.alertState = alertState;
    }

    public ScheduledFuture<?> getFallbackTask() {
        return fallbackTask;
    }

    public void setFallbackTask(ScheduledFuture<?> fallbackTask) {
        this.fallbackTask = fallbackTask;
    }

    @Override
    public String toString() {
        return "FirstAlertData{" +
                "symbol='" + symbol + '\'' +
                ", triggeredAt=" + triggeredAt +
                ", firstPrice=" + firstPrice +
                ", secondPrice=" + secondPrice +
                ", sma10=" + sma10 +
                ", sma20=" + sma20 +
                ", alertState=" + alertState +
                ", fallbackTask=" + fallbackTask +
                '}';
    }
}
