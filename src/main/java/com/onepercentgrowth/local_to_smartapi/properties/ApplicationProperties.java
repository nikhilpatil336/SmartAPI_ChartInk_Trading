package com.onepercentgrowth.local_to_smartapi.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "myapp")
public class ApplicationProperties {

    private String tokenFilePath;
    private String scripmasterFilePath;
    private String filteredScripmasterFilePath;
    private String slOrderstoreFilePath;
    private String orderContextFilePath;
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
    private int leverageMultiplierToUseForLong;
    private int leverageMultiplierToUseForShort;
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
    private boolean tradingAllowDoubleExit;
    private String exitLtpMode;
    private int exitStratWaitTimeAfterCancel;

    private String squareoffCron;
    private String squareoffZone;
    private int exitMaxAttempt;
    private double ticksizeToReduce;
    private int waitTimeBetweenPartialExits;
    private int historicDataDays;
    private boolean fallbackAlertEnable;
    private long fallbackAlertWaitTimeMs;
    private boolean fallbackLtpMockEnable;
    private double fallbackLtpMockPrice;

    private String fallbackLtpMockExchange;
    private String fallbackLtpMockSymbol;
    private String fallbackLtpMockType;
    private String fallbackLtpMockProduct;
    private String fallbackLtpMockSegment;
    private String fallbackLtpMockInstrumentType;

    private BigDecimal defaultTickSize;

    private String eodAnalyticsInputAlertPath;
    private String eodAnalyticsOutputFile;
    private String eodAnalyticsCron;
    private String eodMarketCloseTime;
    private String analyticsSquareoffTime;
    private long ordercontextSaveIntervalMs;

    private String backtestInputAlertPath;
    private String backtestCachePath;
    private String backtestStrategiesPath;
    private String backtestOutputPath;
    private long backtestRequestSleepMs;
    private double backtestBrokeragePct;  // per order both sides; 0.0 = zero brokerage
    private String niftySymbolToken;      // AngelOne NSE token for Nifty 50 (e.g. "26000")
    private String alertTriggerLogPath;
    private List<Long> backtestCapitalAmounts;
    private String liveStrategy = "BASELINE_VOL_SMA10"; // "BASELINE_VOL_SMA10" or "EMA_CROSS_V1"
    private int liveEmaFastPeriod = 9;
    private int liveEmaSlowPeriod = 21;

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

    public int getLeverageMultiplierToUseForLong() {
        return leverageMultiplierToUseForLong;
    }

    public void setLeverageMultiplierToUseForLong(int leverageMultiplierToUseForLong) {
        this.leverageMultiplierToUseForLong = leverageMultiplierToUseForLong;
    }

    public int getLeverageMultiplierToUseForShort() {
        return leverageMultiplierToUseForShort;
    }

    public void setLeverageMultiplierToUseForShort(int leverageMultiplierToUseForShort) {
        this.leverageMultiplierToUseForShort = leverageMultiplierToUseForShort;
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

    public boolean isTradingAllowDoubleExit() {
        return tradingAllowDoubleExit;
    }

    public void setTradingAllowDoubleExit(boolean tradingAllowDoubleExit) {
        this.tradingAllowDoubleExit = tradingAllowDoubleExit;
    }

    public String getExitLtpMode() {
        return exitLtpMode;
    }

    public void setExitLtpMode(String exitLtpMode) {
        this.exitLtpMode = exitLtpMode;
    }

    public int getExitStratWaitTimeAfterCancel() {
        return exitStratWaitTimeAfterCancel;
    }

    public void setExitStratWaitTimeAfterCancel(int exitStratWaitTimeAfterCancel) {
        this.exitStratWaitTimeAfterCancel = exitStratWaitTimeAfterCancel;
    }

    public String getSquareoffCron() {
        return squareoffCron;
    }

    public void setSquareoffCron(String squareoffCron) {
        this.squareoffCron = squareoffCron;
    }

    public String getSquareoffZone() {
        return squareoffZone;
    }

    public void setSquareoffZone(String squareoffZone) {
        this.squareoffZone = squareoffZone;
    }

    public int getExitMaxAttempt() {
        return exitMaxAttempt;
    }

    public void setExitMaxAttempt(int exitMaxAttempt) {
        this.exitMaxAttempt = exitMaxAttempt;
    }

    public double getTicksizeToReduce() {
        return ticksizeToReduce;
    }

    public void setTicksizeToReduce(double ticksizeToReduce) {
        this.ticksizeToReduce = ticksizeToReduce;
    }

    public int getWaitTimeBetweePartialExits() {
        return waitTimeBetweenPartialExits;
    }

    public void setWaitTimeBetweePartialExits(int waitTimeBetweePartialExits) {
        this.waitTimeBetweenPartialExits = waitTimeBetweePartialExits;
    }

    public String getOrderContextFilePath() {
        return orderContextFilePath;
    }

    public void setOrderContextFilePath(String orderContextFilePath) {
        this.orderContextFilePath = orderContextFilePath;
    }

    public int getWaitTimeBetweenPartialExits() {
        return waitTimeBetweenPartialExits;
    }

    public void setWaitTimeBetweenPartialExits(int waitTimeBetweenPartialExits) {
        this.waitTimeBetweenPartialExits = waitTimeBetweenPartialExits;
    }

    public int getHistoricDataDays() {
        return historicDataDays;
    }

    public void setHistoricDataDays(int historicDataDays) {
        this.historicDataDays = historicDataDays;
    }

    public boolean isFallbackAlertEnable() {
        return fallbackAlertEnable;
    }

    public void setFallbackAlertEnable(boolean fallbackAlertEnable) {
        this.fallbackAlertEnable = fallbackAlertEnable;
    }

    public long getFallbackAlertWaitTimeMs() {
        return fallbackAlertWaitTimeMs;
    }

    public void setFallbackAlertWaitTimeMs(long fallbackAlertWaitTimeMs) {
        this.fallbackAlertWaitTimeMs = fallbackAlertWaitTimeMs;
    }

    public boolean isFallbackLtpMockEnable() {
        return fallbackLtpMockEnable;
    }

    public void setFallbackLtpMockEnable(boolean fallbackLtpMockEnable) {
        this.fallbackLtpMockEnable = fallbackLtpMockEnable;
    }

    public double getFallbackLtpMockPrice() {
        return fallbackLtpMockPrice;
    }

    public void setFallbackLtpMockPrice(double fallbackLtpMockPrice) {
        this.fallbackLtpMockPrice = fallbackLtpMockPrice;
    }

    public BigDecimal getDefaultTickSize() {
        return defaultTickSize;
    }

    public void setDefaultTickSize(BigDecimal defaultTickSize) {
        this.defaultTickSize = defaultTickSize;
    }

    public String getFallbackLtpMockExchange() {
        return fallbackLtpMockExchange;
    }

    public void setFallbackLtpMockExchange(String fallbackLtpMockExchange) {
        this.fallbackLtpMockExchange = fallbackLtpMockExchange;
    }

    public String getFallbackLtpMockSymbol() {
        return fallbackLtpMockSymbol;
    }

    public void setFallbackLtpMockSymbol(String fallbackLtpMockSymbol) {
        this.fallbackLtpMockSymbol = fallbackLtpMockSymbol;
    }

    public String getFallbackLtpMockType() {
        return fallbackLtpMockType;
    }

    public void setFallbackLtpMockType(String fallbackLtpMockType) {
        this.fallbackLtpMockType = fallbackLtpMockType;
    }

    public String getFallbackLtpMockProduct() {
        return fallbackLtpMockProduct;
    }

    public void setFallbackLtpMockProduct(String fallbackLtpMockProduct) {
        this.fallbackLtpMockProduct = fallbackLtpMockProduct;
    }

    public String getFallbackLtpMockSegment() {
        return fallbackLtpMockSegment;
    }

    public void setFallbackLtpMockSegment(String fallbackLtpMockSegment) {
        this.fallbackLtpMockSegment = fallbackLtpMockSegment;
    }

    public String getFallbackLtpMockInstrumentType() {
        return fallbackLtpMockInstrumentType;
    }

    public void setFallbackLtpMockInstrumentType(String fallbackLtpMockInstrumentType) {
        this.fallbackLtpMockInstrumentType = fallbackLtpMockInstrumentType;
    }

    public String getEodAnalyticsInputAlertPath() {
        return eodAnalyticsInputAlertPath;
    }

    public void setEodAnalyticsInputAlertPath(String eodAnalyticsInputAlertPath) {
        this.eodAnalyticsInputAlertPath = eodAnalyticsInputAlertPath;
    }

    public String getEodAnalyticsOutputFile() {
        return eodAnalyticsOutputFile;
    }

    public void setEodAnalyticsOutputFile(String eodAnalyticsOutputFile) {
        this.eodAnalyticsOutputFile = eodAnalyticsOutputFile;
    }

    public String getEodAnalyticsCron() {
        return eodAnalyticsCron;
    }

    public void setEodAnalyticsCron(String eodAnalyticsCron) {
        this.eodAnalyticsCron = eodAnalyticsCron;
    }

    public String getEodMarketCloseTime() {
        return eodMarketCloseTime;
    }

    public void setEodMarketCloseTime(String eodMarketCloseTime) {
        this.eodMarketCloseTime = eodMarketCloseTime;
    }

    public String getAnalyticsSquareoffTime() {
        return analyticsSquareoffTime;
    }

    public void setAnalyticsSquareoffTime(String analyticsSquareoffTime) {
        this.analyticsSquareoffTime = analyticsSquareoffTime;
    }

    public long getOrdercontextSaveIntervalMs() {
        return ordercontextSaveIntervalMs;
    }

    public void setOrdercontextSaveIntervalMs(long ordercontextSaveIntervalMs) {
        this.ordercontextSaveIntervalMs = ordercontextSaveIntervalMs;
    }

    public String getBacktestInputAlertPath() {
        return backtestInputAlertPath;
    }

    public void setBacktestInputAlertPath(String backtestInputAlertPath) {
        this.backtestInputAlertPath = backtestInputAlertPath;
    }

    public String getBacktestCachePath() { return backtestCachePath; }
    public void setBacktestCachePath(String backtestCachePath) { this.backtestCachePath = backtestCachePath; }

    public String getBacktestStrategiesPath() { return backtestStrategiesPath; }
    public void setBacktestStrategiesPath(String backtestStrategiesPath) { this.backtestStrategiesPath = backtestStrategiesPath; }

    public String getBacktestOutputPath() { return backtestOutputPath; }
    public void setBacktestOutputPath(String backtestOutputPath) { this.backtestOutputPath = backtestOutputPath; }

    public long getBacktestRequestSleepMs() { return backtestRequestSleepMs; }
    public void setBacktestRequestSleepMs(long backtestRequestSleepMs) { this.backtestRequestSleepMs = backtestRequestSleepMs; }

    public double getBacktestBrokeragePct() { return backtestBrokeragePct; }
    public void setBacktestBrokeragePct(double backtestBrokeragePct) { this.backtestBrokeragePct = backtestBrokeragePct; }

    public String getNiftySymbolToken() { return niftySymbolToken; }
    public void setNiftySymbolToken(String niftySymbolToken) { this.niftySymbolToken = niftySymbolToken; }

    public String getAlertTriggerLogPath() { return alertTriggerLogPath; }
    public void setAlertTriggerLogPath(String alertTriggerLogPath) { this.alertTriggerLogPath = alertTriggerLogPath; }

    public List<Long> getBacktestCapitalAmounts() { return backtestCapitalAmounts; }
    public void setBacktestCapitalAmounts(List<Long> backtestCapitalAmounts) { this.backtestCapitalAmounts = backtestCapitalAmounts; }

    public String getLiveStrategy() { return liveStrategy; }
    public void setLiveStrategy(String liveStrategy) { this.liveStrategy = liveStrategy; }
    public int getLiveEmaFastPeriod() { return liveEmaFastPeriod; }
    public void setLiveEmaFastPeriod(int liveEmaFastPeriod) { this.liveEmaFastPeriod = liveEmaFastPeriod; }
    public int getLiveEmaSlowPeriod() { return liveEmaSlowPeriod; }
    public void setLiveEmaSlowPeriod(int liveEmaSlowPeriod) { this.liveEmaSlowPeriod = liveEmaSlowPeriod; }

    @Override
    public String toString() {
        return "ApplicationProperties{" +
                "tokenFilePath='" + tokenFilePath + '\'' +
                ", scripmasterFilePath='" + scripmasterFilePath + '\'' +
                ", filteredScripmasterFilePath='" + filteredScripmasterFilePath + '\'' +
                ", slOrderstoreFilePath='" + slOrderstoreFilePath + '\'' +
                ", orderContextFilePath='" + orderContextFilePath + '\'' +
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
                ", leverageMultiplierToUseForLong=" + leverageMultiplierToUseForLong +
                ", leverageMultiplierToUseForShort=" + leverageMultiplierToUseForShort +
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
                ", tradingAllowDoubleExit=" + tradingAllowDoubleExit +
                ", exitLtpMode='" + exitLtpMode + '\'' +
                ", exitStratWaitTimeAfterCancel=" + exitStratWaitTimeAfterCancel +
                ", squareoffCron='" + squareoffCron + '\'' +
                ", squareoffZone='" + squareoffZone + '\'' +
                ", exitMaxAttempt=" + exitMaxAttempt +
                ", ticksizeToReduce=" + ticksizeToReduce +
                ", waitTimeBetweenPartialExits=" + waitTimeBetweenPartialExits +
                ", historicDataDays=" + historicDataDays +
                ", fallbackAlertEnable=" + fallbackAlertEnable +
                ", fallbackAlertWaitTimeMs=" + fallbackAlertWaitTimeMs +
                ", fallbackLtpMockEnable=" + fallbackLtpMockEnable +
                ", fallbackLtpMockPrice=" + fallbackLtpMockPrice +
                ", fallbackLtpMockExchange='" + fallbackLtpMockExchange + '\'' +
                ", fallbackLtpMockSymbol='" + fallbackLtpMockSymbol + '\'' +
                ", fallbackLtpMockType='" + fallbackLtpMockType + '\'' +
                ", fallbackLtpMockProduct='" + fallbackLtpMockProduct + '\'' +
                ", fallbackLtpMockSegment='" + fallbackLtpMockSegment + '\'' +
                ", fallbackLtpMockInstrumentType='" + fallbackLtpMockInstrumentType + '\'' +
                ", defaultTickSize=" + defaultTickSize +
                ", eodAnalyticsInputAlertPath='" + eodAnalyticsInputAlertPath + '\'' +
                ", eodAnalyticsOutputFile='" + eodAnalyticsOutputFile + '\'' +
                ", eodAnalyticsCron='" + eodAnalyticsCron + '\'' +
                ", eodMarketCloseTime='" + eodMarketCloseTime + '\'' +
                ", analyticsSquareoffTime='" + analyticsSquareoffTime + '\'' +
                ", ordercontextSaveIntervalMs=" + ordercontextSaveIntervalMs +
                ", backtestInputAlertPath='" + backtestInputAlertPath + '\'' +
                '}';
    }
}

