package com.onepercentgrowth.local_to_smartapi.analytics;

public class AlertAnalyticsRow {

    // ─── Identity ────────────────────────────────────────────────────────────────
    private String tradeDate;
    private String stock;
    private String alertDirection;       // candle color: GREEN / RED / DOJI
    private int tradeTaken;              // 1 if volume condition met

    // ─── Alert timing ────────────────────────────────────────────────────────────
    private String firstAlertTime;
    private double firstAlertTriggerPrice;   // 1st alert candle OPEN
    private String secondAlertTime;
    private double secondAlertTriggerPrice;  // 2nd alert candle OPEN = entry price

    // ─── 1st alert candle OHLCV ──────────────────────────────────────────────────
    private double candleOpen;
    private double candleHigh;
    private double candleLow;
    private double candleClose;
    private long candleVolume;
    private String candleColor;          // GREEN / RED / DOJI
    private int isGreen;
    private int isRed;
    private int isDoji;
    private double candleBodyPct;        // |close−open| / open × 100
    private double candleRangePct;       // |high−low| / open × 100

    // ─── Previous candle ─────────────────────────────────────────────────────────
    private double prevCandleOpen;
    private double prevCandleHigh;
    private double prevCandleLow;
    private double prevCandleClose;
    private long prevCandleVolume;

    // ─── Volume analysis ─────────────────────────────────────────────────────────
    private double volumeSma10;
    private double volumeSma20;
    private double volVsSma10Ratio;      // volume / sma10
    private double volVsSma20Ratio;      // volume / sma20
    private int volGtSma10;
    private int volGtSma20;
    private int volLtSma10;
    private int volLtSma20;
    private int sma10LtSma20;

    // ─── Trade levels ────────────────────────────────────────────────────────────
    private double targetPrice;
    private double slPrice;
    private double riskRewardRatio;      // (target−entry) / (entry−sl)

    // ─── Outcome ─────────────────────────────────────────────────────────────────
    private int targetHit;
    private int slHit;
    private String hitFirst;             // TARGET / SL / SAME_CANDLE / SQUAREOFF / NONE
    private String squareoffReason;      // TRAILING_SL / SL_HIT / AUTO_SQUAREOFF / SAME_CANDLE / NONE
    private double squareoffPrice;       // OPEN of the 15:00 squareoff candle
    private int timeToHitMins;
    private String tradeOutcome;         // WIN / LOSS / SQUAREOFF_EXIT / NO_TRADE

    // ─── Contextual ──────────────────────────────────────────────────────────────
    private String timeOfDay;            // HH:mm of first alert
    private String dayOfWeek;            // MON / TUE / WED / THU / FRI

    // ─── Trailing SL analysis ────────────────────────────────────────────────────
    private int trailingActivated;       // 1 when candle.high >= target (LONG) or candle.low <= target (SHORT)
    private double trailingMaxFavorable; // max HIGH after activation (LONG) / min LOW (SHORT)
    private double trailingSlExitPrice;  // trailing SL price when triggered; 0 if not triggered
    private String trailingOutcome;      // TRAILING_EXIT / SQUAREOFF_AFTER_TRAILING / NOT_ACTIVATED / NO_TRADE

    // ─── Conditional win stats ───────────────────────────────────────────────────
    private int volGtSma10Win;
    private int volGtSma20Win;
    private int volLtSma10Win;
    private int volLtSma20Win;

    // ─── Getters / Setters ───────────────────────────────────────────────────────

    public String getTradeDate() { return tradeDate; }
    public void setTradeDate(String tradeDate) { this.tradeDate = tradeDate; }

    public String getStock() { return stock; }
    public void setStock(String stock) { this.stock = stock; }

    public String getAlertDirection() { return alertDirection; }
    public void setAlertDirection(String alertDirection) { this.alertDirection = alertDirection; }

    public int getTradeTaken() { return tradeTaken; }
    public void setTradeTaken(int tradeTaken) { this.tradeTaken = tradeTaken; }

    public String getFirstAlertTime() { return firstAlertTime; }
    public void setFirstAlertTime(String firstAlertTime) { this.firstAlertTime = firstAlertTime; }

    public double getFirstAlertTriggerPrice() { return firstAlertTriggerPrice; }
    public void setFirstAlertTriggerPrice(double firstAlertTriggerPrice) { this.firstAlertTriggerPrice = firstAlertTriggerPrice; }

    public String getSecondAlertTime() { return secondAlertTime; }
    public void setSecondAlertTime(String secondAlertTime) { this.secondAlertTime = secondAlertTime; }

    public double getSecondAlertTriggerPrice() { return secondAlertTriggerPrice; }
    public void setSecondAlertTriggerPrice(double secondAlertTriggerPrice) { this.secondAlertTriggerPrice = secondAlertTriggerPrice; }

    public double getCandleOpen() { return candleOpen; }
    public void setCandleOpen(double candleOpen) { this.candleOpen = candleOpen; }

    public double getCandleHigh() { return candleHigh; }
    public void setCandleHigh(double candleHigh) { this.candleHigh = candleHigh; }

    public double getCandleLow() { return candleLow; }
    public void setCandleLow(double candleLow) { this.candleLow = candleLow; }

    public double getCandleClose() { return candleClose; }
    public void setCandleClose(double candleClose) { this.candleClose = candleClose; }

    public long getCandleVolume() { return candleVolume; }
    public void setCandleVolume(long candleVolume) { this.candleVolume = candleVolume; }

    public String getCandleColor() { return candleColor; }
    public void setCandleColor(String candleColor) { this.candleColor = candleColor; }

    public int getIsGreen() { return isGreen; }
    public void setIsGreen(int isGreen) { this.isGreen = isGreen; }

    public int getIsRed() { return isRed; }
    public void setIsRed(int isRed) { this.isRed = isRed; }

    public int getIsDoji() { return isDoji; }
    public void setIsDoji(int isDoji) { this.isDoji = isDoji; }

    public double getCandleBodyPct() { return candleBodyPct; }
    public void setCandleBodyPct(double candleBodyPct) { this.candleBodyPct = candleBodyPct; }

    public double getCandleRangePct() { return candleRangePct; }
    public void setCandleRangePct(double candleRangePct) { this.candleRangePct = candleRangePct; }

    public double getPrevCandleOpen() { return prevCandleOpen; }
    public void setPrevCandleOpen(double prevCandleOpen) { this.prevCandleOpen = prevCandleOpen; }

    public double getPrevCandleHigh() { return prevCandleHigh; }
    public void setPrevCandleHigh(double prevCandleHigh) { this.prevCandleHigh = prevCandleHigh; }

    public double getPrevCandleLow() { return prevCandleLow; }
    public void setPrevCandleLow(double prevCandleLow) { this.prevCandleLow = prevCandleLow; }

    public double getPrevCandleClose() { return prevCandleClose; }
    public void setPrevCandleClose(double prevCandleClose) { this.prevCandleClose = prevCandleClose; }

    public long getPrevCandleVolume() { return prevCandleVolume; }
    public void setPrevCandleVolume(long prevCandleVolume) { this.prevCandleVolume = prevCandleVolume; }

    public double getVolumeSma10() { return volumeSma10; }
    public void setVolumeSma10(double volumeSma10) { this.volumeSma10 = volumeSma10; }

    public double getVolumeSma20() { return volumeSma20; }
    public void setVolumeSma20(double volumeSma20) { this.volumeSma20 = volumeSma20; }

    public double getVolVsSma10Ratio() { return volVsSma10Ratio; }
    public void setVolVsSma10Ratio(double volVsSma10Ratio) { this.volVsSma10Ratio = volVsSma10Ratio; }

    public double getVolVsSma20Ratio() { return volVsSma20Ratio; }
    public void setVolVsSma20Ratio(double volVsSma20Ratio) { this.volVsSma20Ratio = volVsSma20Ratio; }

    public int getVolGtSma10() { return volGtSma10; }
    public void setVolGtSma10(int volGtSma10) { this.volGtSma10 = volGtSma10; }

    public int getVolGtSma20() { return volGtSma20; }
    public void setVolGtSma20(int volGtSma20) { this.volGtSma20 = volGtSma20; }

    public int getVolLtSma10() { return volLtSma10; }
    public void setVolLtSma10(int volLtSma10) { this.volLtSma10 = volLtSma10; }

    public int getVolLtSma20() { return volLtSma20; }
    public void setVolLtSma20(int volLtSma20) { this.volLtSma20 = volLtSma20; }

    public int getSma10LtSma20() { return sma10LtSma20; }
    public void setSma10LtSma20(int sma10LtSma20) { this.sma10LtSma20 = sma10LtSma20; }

    public double getTargetPrice() { return targetPrice; }
    public void setTargetPrice(double targetPrice) { this.targetPrice = targetPrice; }

    public double getSlPrice() { return slPrice; }
    public void setSlPrice(double slPrice) { this.slPrice = slPrice; }

    public double getRiskRewardRatio() { return riskRewardRatio; }
    public void setRiskRewardRatio(double riskRewardRatio) { this.riskRewardRatio = riskRewardRatio; }

    public int getTargetHit() { return targetHit; }
    public void setTargetHit(int targetHit) { this.targetHit = targetHit; }

    public int getSlHit() { return slHit; }
    public void setSlHit(int slHit) { this.slHit = slHit; }

    public String getHitFirst() { return hitFirst; }
    public void setHitFirst(String hitFirst) { this.hitFirst = hitFirst; }

    public String getSquareoffReason() { return squareoffReason; }
    public void setSquareoffReason(String squareoffReason) { this.squareoffReason = squareoffReason; }

    public double getSquareoffPrice() { return squareoffPrice; }
    public void setSquareoffPrice(double squareoffPrice) { this.squareoffPrice = squareoffPrice; }

    public int getTimeToHitMins() { return timeToHitMins; }
    public void setTimeToHitMins(int timeToHitMins) { this.timeToHitMins = timeToHitMins; }

    public String getTradeOutcome() { return tradeOutcome; }
    public void setTradeOutcome(String tradeOutcome) { this.tradeOutcome = tradeOutcome; }

    public String getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public int getTrailingActivated() { return trailingActivated; }
    public void setTrailingActivated(int trailingActivated) { this.trailingActivated = trailingActivated; }

    public double getTrailingMaxFavorable() { return trailingMaxFavorable; }
    public void setTrailingMaxFavorable(double trailingMaxFavorable) { this.trailingMaxFavorable = trailingMaxFavorable; }

    public double getTrailingSlExitPrice() { return trailingSlExitPrice; }
    public void setTrailingSlExitPrice(double trailingSlExitPrice) { this.trailingSlExitPrice = trailingSlExitPrice; }

    public String getTrailingOutcome() { return trailingOutcome; }
    public void setTrailingOutcome(String trailingOutcome) { this.trailingOutcome = trailingOutcome; }

    public int getVolGtSma10Win() { return volGtSma10Win; }
    public void setVolGtSma10Win(int volGtSma10Win) { this.volGtSma10Win = volGtSma10Win; }

    public int getVolGtSma20Win() { return volGtSma20Win; }
    public void setVolGtSma20Win(int volGtSma20Win) { this.volGtSma20Win = volGtSma20Win; }

    public int getVolLtSma10Win() { return volLtSma10Win; }
    public void setVolLtSma10Win(int volLtSma10Win) { this.volLtSma10Win = volLtSma10Win; }

    public int getVolLtSma20Win() { return volLtSma20Win; }
    public void setVolLtSma20Win(int volLtSma20Win) { this.volLtSma20Win = volLtSma20Win; }
}
