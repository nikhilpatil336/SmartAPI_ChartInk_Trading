package com.onepercentgrowth.local_to_smartapi.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.LocalTime;

@Component
@ConfigurationProperties(prefix = "myapp")
public class ApplicationProperties {

    private String tokenFilePath;
    private String scripmasterFilePath;
    private String filteredScripmasterFilePath;
    private String slOrderstoreFilePath;
    private double balanceMinimumAllowed;
    private int stockBuyMinimumQuantityRequired;
    private double profitPercentageMultiplier;
    private double stoplossPercentageMultiplier;
    private long orderBookRetryMilliseconds;
    private double percentBalanceUse;
    private int numberOfStocksBuyLess;
    private boolean fixedQuantityFlag;
    private int fixedQuantity;
    private boolean tradingWindowEnable;
    private LocalTime tradingWindowStartTime;
    private LocalTime tradingWindowEndTime;
    private String tradingWindowTimeZone;
    private boolean rmsAutoRefreshEnable;
    private boolean rmsFileOverwriteEnabled;
    private int rmsRefreshIntervalMinutes;
    private String rmsBalanceFilePath;

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

    public boolean isFixedQuantityFlag() {
        return fixedQuantityFlag;
    }

    public void setFixedQuantityFlag(boolean fixedQuantityFlag) {
        this.fixedQuantityFlag = fixedQuantityFlag;
    }

    public int getFixedQuantity() {
        return fixedQuantity;
    }

    public void setFixedQuantity(int fixedQuantity) {
        this.fixedQuantity = fixedQuantity;
    }

    public String getTokenFilePath() {
        return tokenFilePath;
    }

    public void setTokenFilePath(String tokenFilePath) {
        this.tokenFilePath = tokenFilePath;
    }

    public String getScripmasterFilePath() {
        return scripmasterFilePath;
    }

    public void setScripmasterFilePath(String scripmasterFilePath) {
        this.scripmasterFilePath = scripmasterFilePath;
    }

    public String getFilteredScripmasterFilePath() {
        return filteredScripmasterFilePath;
    }

    public void setFilteredScripmasterFilePath(String filteredScripmasterFilePath) {
        this.filteredScripmasterFilePath = filteredScripmasterFilePath;
    }

    public String getSlOrderstoreFilePath() {
        return slOrderstoreFilePath;
    }

    public void setSlOrderstoreFilePath(String slOrderstoreFilePath) {
        this.slOrderstoreFilePath = slOrderstoreFilePath;
    }

    public boolean isTradingWindowEnable() {
        return tradingWindowEnable;
    }

    public void setTradingWindowEnable(boolean tradingWindowEnable) {
        this.tradingWindowEnable = tradingWindowEnable;
    }

    public LocalTime getTradingWindowStartTime() {
        return tradingWindowStartTime;
    }

    public void setTradingWindowStartTime(LocalTime tradingWindowStartTime) {
        this.tradingWindowStartTime = tradingWindowStartTime;
    }

    public LocalTime getTradingWindowEndTime() {
        return tradingWindowEndTime;
    }

    public void setTradingWindowEndTime(LocalTime tradingWindowEndTime) {
        this.tradingWindowEndTime = tradingWindowEndTime;
    }

    public String getTradingWindowTimeZone() {
        return tradingWindowTimeZone;
    }

    public void setTradingWindowTimeZone(String tradingWindowTimeZone) {
        this.tradingWindowTimeZone = tradingWindowTimeZone;
    }

//    public boolean isAutoRefreshEnabled() {
//        return rmsAutoRefreshEnable;
//    }
//
//    public void setAutoRefreshEnabled(boolean rmsAutoRefreshEnable) {
//        this.rmsAutoRefreshEnable = rmsAutoRefreshEnable;
//    }
//
//    public int getRefreshIntervalMinutes() {
//        return rmsRefreshIntervalMinutes;
//    }
//
//    public void setRefreshIntervalMinutes(int rmsRefreshIntervalMinutes) {
//        this.rmsRefreshIntervalMinutes = rmsRefreshIntervalMinutes;
//    }
//
//    public boolean isFileOverwriteEnabled() {
//        return rmsFileOverwriteEnabled;
//    }
//
//    public void setFileOverwriteEnabled(boolean fileOverwriteEnabled) {
//        this.rmsFileOverwriteEnabled = rmsFileOverwriteEnabled;
//    }
//
//    public String getRmsBalanceFilePath() {
//        return rmsBalanceFilePath;
//    }
//
//    public void setRmsBalanceFilePath(String rmsBalanceFilePath) {
//        this.rmsBalanceFilePath = rmsBalanceFilePath;
//    }

    public boolean isRmsAutoRefreshEnable() {
        return rmsAutoRefreshEnable;
    }

    public void setRmsAutoRefreshEnable(boolean rmsAutoRefreshEnable) {
        this.rmsAutoRefreshEnable = rmsAutoRefreshEnable;
    }

    public boolean isRmsFileOverwriteEnabled() {
        return rmsFileOverwriteEnabled;
    }

    public void setRmsFileOverwriteEnabled(boolean rmsFileOverwriteEnabled) {
        this.rmsFileOverwriteEnabled = rmsFileOverwriteEnabled;
    }

    public int getRmsRefreshIntervalMinutes() {
        return rmsRefreshIntervalMinutes;
    }

    public void setRmsRefreshIntervalMinutes(int rmsRefreshIntervalMinutes) {
        this.rmsRefreshIntervalMinutes = rmsRefreshIntervalMinutes;
    }

    public String getRmsBalanceFilePath() {
        return rmsBalanceFilePath;
    }

    public void setRmsBalanceFilePath(String rmsBalanceFilePath) {
        this.rmsBalanceFilePath = rmsBalanceFilePath;
    }
}
