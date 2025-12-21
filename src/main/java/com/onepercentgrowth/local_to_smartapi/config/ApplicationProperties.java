package com.onepercentgrowth.local_to_smartapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "myapp")
public class ApplicationProperties {

//    private double minimum_balance_value;
//
//    private int minimum_stock_buy_quantity;
//
//    private double profit_percentage_multiplier;
//
//    private double stoploss_percentage_multiplier;
//
//    private long order_book_retry_miliseconds;
//
//    public double getMinimum_balance_value() {
//        return minimum_balance_value;
//    }
//
//    public int getMinimum_stock_buy_quantity() {
//        return minimum_stock_buy_quantity;
//    }
//
//    public double getProfit_percentage_multiplier() {
//        return profit_percentage_multiplier;
//    }
//
//    public double getStoploss_percentage_multiplier() {
//        return stoploss_percentage_multiplier;
//    }
//
//    public long getOrder_book_retry_miliseconds() {
//        return order_book_retry_miliseconds;
//    }

    private double minimumBalanceAmount;
    private int minimumStockBuyQuantity;
    private double profitPercentageMultiplier;
    private double stoplossPercentageMultiplier;
    private long orderBookRetryMilliseconds;

    public double getMinimumBalanceAmount() {
        return minimumBalanceAmount;
    }

    public void setMinimumBalanceAmount(double minimumBalanceValue) {
        this.minimumBalanceAmount = minimumBalanceValue;
    }

    public int getMinimumStockBuyQuantity() {
        return minimumStockBuyQuantity;
    }

    public void setMinimumStockBuyQuantity(int minimumStockBuyQuantity) {
        this.minimumStockBuyQuantity = minimumStockBuyQuantity;
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
}
