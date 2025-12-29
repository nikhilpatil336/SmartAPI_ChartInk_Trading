package com.onepercentgrowth.local_to_smartapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "myapp")
public class ApplicationProperties {
    private double balanceMinimumAllowed;
    private int stockBuyMinimumQuantityRequired;
    private double profitPercentageMultiplier;
    private double stoplossPercentageMultiplier;
    private long orderBookRetryMilliseconds;
    private double percentBalanceUse;
    private int numberOfStocksBuyLess;

    public double getBalanceMinimumAllowed() {
        return balanceMinimumAllowed;
    }

    public void setBalanceMinimumAllowed(double balanceMinimumAllowed) {
        this.balanceMinimumAllowed = balanceMinimumAllowed;
    }

    public int getStockBuyMinimumQuantityRequired() {
        return stockBuyMinimumQuantityRequired;
    }

    public void setStockBuyMinimumQuantityRequired(int stockBuyMinimumQuantityRequired) {
        this.stockBuyMinimumQuantityRequired = stockBuyMinimumQuantityRequired;
    }

    public double getProfitPercentageMultiplier() {
        return profitPercentageMultiplier;
    }

    public void setProfitPercentageMultiplier(double profitPercentageMultiplier) {
        this.profitPercentageMultiplier = profitPercentageMultiplier;
    }

    public double getStoplossPercentageMultiplier() {
        return stoplossPercentageMultiplier;
    }

    public void setStoplossPercentageMultiplier(double stoplossPercentageMultiplier) {
        this.stoplossPercentageMultiplier = stoplossPercentageMultiplier;
    }

    public long getOrderBookRetryMilliseconds() {
        return orderBookRetryMilliseconds;
    }

    public void setOrderBookRetryMilliseconds(long orderBookRetryMilliseconds) {
        this.orderBookRetryMilliseconds = orderBookRetryMilliseconds;
    }

    public double getPercentBalanceUse() {
        return percentBalanceUse;
    }

    public void setPercentBalanceUse(double percentBalanceUse) {
        this.percentBalanceUse = percentBalanceUse;
    }

    public int getNumberOfStocksBuyLess() {
        return numberOfStocksBuyLess;
    }

    public void setNumberOfStocksBuyLess(int numberOfStocksBuyLess) {
        this.numberOfStocksBuyLess = numberOfStocksBuyLess;
    }
}
