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

    public int getStockBuyMinimumQuantityRequired() {
        return stockBuyMinimumQuantityRequired;
    }

    public double getProfitPercentageMultiplier() {
        return profitPercentageMultiplier;
    }

    public double getStoplossPercentageMultiplier() {
        return stoplossPercentageMultiplier;
    }

    public long getOrderBookRetryMilliseconds() {
        return orderBookRetryMilliseconds;
    }

    public double getPercentBalanceUse() {
        return percentBalanceUse;
    }

    public int getNumberOfStocksBuyLess() {
        return numberOfStocksBuyLess;
    }
}
