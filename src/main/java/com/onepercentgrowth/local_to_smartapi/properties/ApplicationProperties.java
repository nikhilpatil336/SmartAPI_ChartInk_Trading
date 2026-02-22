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
    private double buyProfitPercentageMultiplier;
    private double buyStoplossPercentageMultiplier;
    private double sellProfitPercentageMultiplier;
    private double sellStoplossPercentageMultiplier;
    private double defaultBuyProfitPercentageMultiplier;
    private double defaultBuyStoplossPercentageMultiplier;
    private double defaultSellProfitPercentageMultiplier;
    private double defaultSellStoplossPercentageMultiplier;
    private long orderBookRetryMilliseconds;
    private double percentBalanceUse;
    private int numberOfStocksBuyLessForLong;
    private int numberOfStocksSellLessForShort;
    private int leverageMultiplierToUse;
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
    private boolean scripmasterOnlyFnoStocks;
    private boolean scripmasterEnableFnoUniverse;
    private String scripmasterFnoListFilePath;
    private boolean leverageEnable;
    private boolean leverageUniverse;
    private String leverageExchange;
    private String leverageListFilePath;
    private double tradingStoplossPercent;
    private double tradingStoplossBufferPercent;
    private String excelToSaveAlerts;
    private String growthAlertExcelPath;
    private String shortAlertExcelPath;

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

    public double getBuyProfitPercentageMultiplier() {
        return buyProfitPercentageMultiplier;
    }

    public void setBuyProfitPercentageMultiplier(double buyProfitPercentageMultiplier) {
        this.buyProfitPercentageMultiplier = buyProfitPercentageMultiplier;
    }

    public double getBuyStoplossPercentageMultiplier() {
        return buyStoplossPercentageMultiplier;
    }

    public void setBuyStoplossPercentageMultiplier(double buyStoplossPercentageMultiplier) {
        this.buyStoplossPercentageMultiplier = buyStoplossPercentageMultiplier;
    }

    public double getSellProfitPercentageMultiplier() {
        return sellProfitPercentageMultiplier;
    }

    public void setSellProfitPercentageMultiplier(double sellProfitPercentageMultiplier) {
        this.sellProfitPercentageMultiplier = sellProfitPercentageMultiplier;
    }

    public double getSellStoplossPercentageMultiplier() {
        return sellStoplossPercentageMultiplier;
    }

    public void setSellStoplossPercentageMultiplier(double sellStoplossPercentageMultiplier) {
        this.sellStoplossPercentageMultiplier = sellStoplossPercentageMultiplier;
    }

    public double getDefaultBuyProfitPercentageMultiplier() {
        return defaultBuyProfitPercentageMultiplier;
    }

    public void setDefaultBuyProfitPercentageMultiplier(double defaultBuyProfitPercentageMultiplier) {
        this.defaultBuyProfitPercentageMultiplier = defaultBuyProfitPercentageMultiplier;
    }

    public double getDefaultBuyStoplossPercentageMultiplier() {
        return defaultBuyStoplossPercentageMultiplier;
    }

    public void setDefaultBuyStoplossPercentageMultiplier(double defaultBuyStoplossPercentageMultiplier) {
        this.defaultBuyStoplossPercentageMultiplier = defaultBuyStoplossPercentageMultiplier;
    }

    public double getDefaultSellProfitPercentageMultiplier() {
        return defaultSellProfitPercentageMultiplier;
    }

    public void setDefaultSellProfitPercentageMultiplier(double defaultSellProfitPercentageMultiplier) {
        this.defaultSellProfitPercentageMultiplier = defaultSellProfitPercentageMultiplier;
    }

    public double getDefaultSellStoplossPercentageMultiplier() {
        return defaultSellStoplossPercentageMultiplier;
    }

    public void setDefaultSellStoplossPercentageMultiplier(double defaultSellStoplossPercentageMultiplier) {
        this.defaultSellStoplossPercentageMultiplier = defaultSellStoplossPercentageMultiplier;
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

    public int getNumberOfStocksBuyLessForLong() {
        return numberOfStocksBuyLessForLong;
    }

    public void setNumberOfStocksBuyLessForLong(int numberOfStocksBuyLessForLong) {
        this.numberOfStocksBuyLessForLong = numberOfStocksBuyLessForLong;
    }

    public int getNumberOfStocksSellLessForShort() {
        return numberOfStocksSellLessForShort;
    }

    public void setNumberOfStocksSellLessForShort(int numberOfStocksSellLessForShort) {
        this.numberOfStocksSellLessForShort = numberOfStocksSellLessForShort;
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

    public boolean isScripmasterOnlyFnoStocks() {
        return scripmasterOnlyFnoStocks;
    }

    public void setScripmasterOnlyFnoStocks(boolean scripmasterOnlyFnoStocks) {
        this.scripmasterOnlyFnoStocks = scripmasterOnlyFnoStocks;
    }

    public boolean isScripmasterEnableFnoUniverse() {
        return scripmasterEnableFnoUniverse;
    }

    public void setScripmasterEnableFnoUniverse(boolean scripmasterEnableFnoUniverse) {
        this.scripmasterEnableFnoUniverse = scripmasterEnableFnoUniverse;
    }

    public String getScripmasterFnoListFilePath() {
        return scripmasterFnoListFilePath;
    }

    public void setScripmasterFnoListFilePath(String scripmasterFnoListFilePath) {
        this.scripmasterFnoListFilePath = scripmasterFnoListFilePath;
    }

    public boolean isLeverageEnable() {
        return leverageEnable;
    }

    public void setLeverageEnable(boolean leverageEnable) {
        this.leverageEnable = leverageEnable;
    }

    public boolean isLeverageUniverse() {
        return leverageUniverse;
    }

    public void setLeverageUniverse(boolean leverageUniverse) {
        this.leverageUniverse = leverageUniverse;
    }

    public String getLeverageExchange() {
        return leverageExchange;
    }

    public void setLeverageExchange(String leverageExchange) {
        this.leverageExchange = leverageExchange;
    }

    public String getLeverageListFilePath() {
        return leverageListFilePath;
    }

    public void setLeverageListFilePath(String leverageListFilePath) {
        this.leverageListFilePath = leverageListFilePath;
    }

    public int getLeverageMultiplierToUse() {
        return leverageMultiplierToUse;
    }

    public void setLeverageMultiplierToUse(int leverageMultiplierToUse) {
        this.leverageMultiplierToUse = leverageMultiplierToUse;
    }

    public double getTradingStoplossPercent() {
        return tradingStoplossPercent;
    }

    public void setTradingStoplossPercent(double tradingStoplossPercent) {
        this.tradingStoplossPercent = tradingStoplossPercent;
    }

    public double getTradingStoplossBufferPercent() {
        return tradingStoplossBufferPercent;
    }

    public void setTradingStoplossBufferPercent(double tradingStoplossBufferPercent) {
        this.tradingStoplossBufferPercent = tradingStoplossBufferPercent;
    }

    public String getExcelToSaveAlerts() {
        return excelToSaveAlerts;
    }

    public void setExcelToSaveAlerts(String excelToSaveAlerts) {
        this.excelToSaveAlerts = excelToSaveAlerts;
    }

    public String getGrowthAlertExcelPath() {
        return growthAlertExcelPath;
    }

    public void setGrowthAlertExcelPath(String growthAlertExcelPath) {
        this.growthAlertExcelPath = growthAlertExcelPath;
    }

    public String getShortAlertExcelPath() {
        return shortAlertExcelPath;
    }

    public void setShortAlertExcelPath(String shortAlertExcelPath) {
        this.shortAlertExcelPath = shortAlertExcelPath;
    }

    @Override
    public String toString() {
        return "ApplicationProperties{" +
                "tokenFilePath='" + tokenFilePath + '\'' +
                ", scripmasterFilePath='" + scripmasterFilePath + '\'' +
                ", filteredScripmasterFilePath='" + filteredScripmasterFilePath + '\'' +
                ", slOrderstoreFilePath='" + slOrderstoreFilePath + '\'' +
                ", balanceMinimumAllowed=" + balanceMinimumAllowed +
                ", stockBuyMinimumQuantityRequired=" + stockBuyMinimumQuantityRequired +
                ", buyProfitPercentageMultiplier=" + buyProfitPercentageMultiplier +
                ", buyStoplossPercentageMultiplier=" + buyStoplossPercentageMultiplier +
                ", sellProfitPercentageMultiplier=" + sellProfitPercentageMultiplier +
                ", sellStoplossPercentageMultiplier=" + sellStoplossPercentageMultiplier +
                ", defaultBuyProfitPercentageMultiplier=" + defaultBuyProfitPercentageMultiplier +
                ", defaultBuyStoplossPercentageMultiplier=" + defaultBuyStoplossPercentageMultiplier +
                ", defaultSellProfitPercentageMultiplier=" + defaultSellProfitPercentageMultiplier +
                ", defaultSellStoplossPercentageMultiplier=" + defaultSellStoplossPercentageMultiplier +
                ", orderBookRetryMilliseconds=" + orderBookRetryMilliseconds +
                ", percentBalanceUse=" + percentBalanceUse +
                ", numberOfStocksBuyLessForLong=" + numberOfStocksBuyLessForLong +
                ", numberOfStocksSellLessForShort=" + numberOfStocksSellLessForShort +
                ", leverageMultiplierToUse=" + leverageMultiplierToUse +
                ", fixedQuantityFlag=" + fixedQuantityFlag +
                ", fixedQuantity=" + fixedQuantity +
                ", tradingWindowEnable=" + tradingWindowEnable +
                ", tradingWindowStartTime=" + tradingWindowStartTime +
                ", tradingWindowEndTime=" + tradingWindowEndTime +
                ", tradingWindowTimeZone='" + tradingWindowTimeZone + '\'' +
                ", rmsAutoRefreshEnable=" + rmsAutoRefreshEnable +
                ", rmsFileOverwriteEnabled=" + rmsFileOverwriteEnabled +
                ", rmsRefreshIntervalMinutes=" + rmsRefreshIntervalMinutes +
                ", rmsBalanceFilePath='" + rmsBalanceFilePath + '\'' +
                ", scripmasterOnlyFnoStocks=" + scripmasterOnlyFnoStocks +
                ", scripmasterEnableFnoUniverse=" + scripmasterEnableFnoUniverse +
                ", scripmasterFnoListFilePath='" + scripmasterFnoListFilePath + '\'' +
                ", leverageEnable=" + leverageEnable +
                ", leverageUniverse=" + leverageUniverse +
                ", leverageExchange='" + leverageExchange + '\'' +
                ", leverageListFilePath='" + leverageListFilePath + '\'' +
                ", tradingStoplossPercent=" + tradingStoplossPercent +
                ", tradingStoplossBufferPercent=" + tradingStoplossBufferPercent +
                ", excelToSaveAlerts='" + excelToSaveAlerts + '\'' +
                ", growthAlertExcelPath='" + growthAlertExcelPath + '\'' +
                ", shortAlertExcelPath='" + shortAlertExcelPath + '\'' +
                '}';
    }
}
