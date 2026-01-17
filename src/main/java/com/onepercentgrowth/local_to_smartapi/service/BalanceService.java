package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.model.RmsData;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BalanceService {

    @Autowired
    private ApplicationProperties applicationProperties;

    private BigDecimal morningAvailableBalance = BigDecimal.ZERO;
    private BigDecimal usableBalance = BigDecimal.ZERO;
    private BigDecimal rmsBalanceAtBrokerSide = BigDecimal.ZERO;

    // Profit made intraday but NOT settled yet
    private BigDecimal unsettledPnL = BigDecimal.ZERO;

    // (Optional) For logging / reconciliation
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    private static final Logger log = LoggerFactory.getLogger(BalanceService.class);

    public synchronized void syncFromRms(RmsData rmsData) {
        if (rmsData == null || rmsData.getAvailablecash() == null) {
            log.warn("RMS sync skipped: data is null");
            return;
        }

        this.morningAvailableBalance =
                new BigDecimal(rmsData.getAvailablecash())
                        .setScale(2, RoundingMode.DOWN);

        this.usableBalance = getUsableBalance(applicationProperties.getPercentBalanceUse());

        this.rmsBalanceAtBrokerSide = morningAvailableBalance;

        log.info("Balance synced from RMS: {}", morningAvailableBalance);
    }

/** ================= RISK ================= */

    /**
     * How much cash strategy is allowed to use
     */
    public synchronized BigDecimal getUsableBalance(double usagePercent) {
        if (usagePercent <= 0 || usagePercent > 1) {
            throw new IllegalArgumentException(
                    "usagePercent must be between 0 and 1"
            );
        }

        usableBalance = morningAvailableBalance
                .multiply(BigDecimal.valueOf(usagePercent))
                .setScale(2, RoundingMode.DOWN);

        return usableBalance;
    }

    /**
     * HARD RISK GATE — blocks bad orders
     */
    public synchronized void assertSufficientFunds(
            BigDecimal price,
            int quantity
    ) {
        BigDecimal required =
                price.multiply(BigDecimal.valueOf(quantity));

        if (required.compareTo(usableBalance) > 0) {
            throw new IllegalStateException(
                    "Insufficient funds. Required="
                            + required + ", Available=" + usableBalance
            );
        }
    }

/** ================= ORDER EVENTS ================= */

    /**
     * Called ONLY when BUY is CONFIRMED (filled)
     */
//    public synchronized void onBuy(BigDecimal buyPrice, int quantity, BigDecimal oldBalance, double maxLeverage) {
//        BigDecimal cost =
//                buyPrice.multiply(BigDecimal.valueOf(quantity));
//
////        BigDecimal cost = price.multiply(BigDecimal.valueOf(quantity / applicationProperties.getLeverageMultiplierToUse()));
//        usableBalance = usableBalance.subtract(buyPrice.multiply(BigDecimal.valueOf((double) quantity / applicationProperties.getLeverageMultiplierToUse())));
//        rmsBalanceAtBrokerSide = rmsBalanceAtBrokerSide.subtract(buyPrice.multiply(BigDecimal.valueOf(quantity / maxLeverage)));
////        currentBalance = currentBalance.subtract(cost);
//
//        log.info("BUY executed. Cost={}, Quantity = {}, Old balance = {}, New balance={}",
//                cost, quantity, oldBalance, usableBalance);
//    }

    public synchronized void onBuy(
            BigDecimal buyPrice,
            int quantity,
            double leverage,
            BigDecimal oldBalance,
            double maxLeverage
    ) {
        BigDecimal positionValue =
                buyPrice.multiply(BigDecimal.valueOf(quantity));

        BigDecimal marginUsed =
                positionValue.divide(
                        BigDecimal.valueOf(leverage),
                        RoundingMode.HALF_UP
                );

        if (usableBalance.compareTo(marginUsed) < 0) {
            throw new IllegalStateException("Insufficient usable balance");
        }

        usableBalance = usableBalance.subtract(marginUsed);

        log.info(
                "BUY executed | BuyPrice={} Qty={} MarginUsed={} UsableBalance={}",
                buyPrice, quantity, marginUsed, usableBalance
        );
    }


    /**
     * Called ONLY when SELL or SL is CONFIRMED (filled)
     */
//    public synchronized void onSell(BigDecimal price, int quantity, BigDecimal oldBalance, double maxLeverage) {
//        BigDecimal credit =
//                price.multiply(BigDecimal.valueOf(quantity));
//
////        BigDecimal credit = price.multiply(BigDecimal.valueOf(quantity / applicationProperties.getLeverageMultiplierToUse()));
////        usableBalance = usableBalance.add(price.multiply(BigDecimal.valueOf((double) quantity / applicationProperties.getLeverageMultiplierToUse())));
////        rmsBalanceAtBrokerSide = rmsBalanceAtBrokerSide.add(price.multiply(BigDecimal.valueOf(quantity / maxLeverage)));
//
//        usableBalance = morningAvailableBalance;
//        rmsBalanceAtBrokerSide = morningAvailableBalance;
//
////        currentBalance = currentBalance.add(credit);
//
//        log.info("SELL executed. Credit={}, Quantity = {}, Old balance = {}, New balance={}",
//                credit, quantity, oldBalance, usableBalance);
//    }

//    public synchronized void onSLSell(BigDecimal sellPrice, int quantity, BigDecimal oldBalance, double maxLeverage, double buyPrice) {
////        BigDecimal credit =
////                price.multiply(BigDecimal.valueOf(quantity));
////
//////        BigDecimal credit = price.multiply(BigDecimal.valueOf(quantity / applicationProperties.getLeverageMultiplierToUse()));
//////        usableBalance = usableBalance.add(price.multiply(BigDecimal.valueOf((double) quantity / applicationProperties.getLeverageMultiplierToUse())));
//////        rmsBalanceAtBrokerSide = rmsBalanceAtBrokerSide.add(price.multiply(BigDecimal.valueOf(quantity / maxLeverage)));
////
////        BigDecimal loss = morningAvailableBalance.subtract(price.multiply(BigDecimal.valueOf((double) quantity / applicationProperties.getLeverageMultiplierToUse())));
////        BigDecimal totalLoss = loss.multiply(BigDecimal.valueOf(applicationProperties.getLeverageMultiplierToUse()));
////        morningAvailableBalance = morningAvailableBalance.subtract(totalLoss);
////
//////        currentBalance = currentBalance.add(credit);
////
////        log.info("SELL executed. Loss={}, Quantity = {}, Old balance = {}, New balance={}",
////                totalLoss, quantity, oldBalance, morningAvailableBalance);
//
//        BigDecimal buy = BigDecimal.valueOf(buyPrice);
//
//        // (SellPrice - BuyPrice)
//        BigDecimal priceDifference = sellPrice.subtract(buy);
//
//        // PnL = price difference × quantity
//        BigDecimal pnl = priceDifference.multiply(BigDecimal.valueOf(quantity));
//
//        // New balance
//        BigDecimal newBalance = oldBalance.add(pnl);
//
//        morningAvailableBalance = newBalance;
//
//        log.info(
//                "SELL executed | BuyPrice={} SellPrice={} Quantity={} PnL={} OldBalance={} NewBalance={}",
//                buy, sellPrice, quantity, pnl, oldBalance, newBalance
//        );
//    }

    public synchronized void onSell(
            BigDecimal sellPrice,
            BigDecimal buyPrice,
            int quantity,
            double leverage,
            BigDecimal oldBalance,
            double maxLeverage
    ) {
        BigDecimal pnl =
                sellPrice.subtract(buyPrice)
                        .multiply(BigDecimal.valueOf(quantity));

        BigDecimal positionValue =
                buyPrice.multiply(BigDecimal.valueOf(quantity));

        BigDecimal releasedMargin =
                positionValue.divide(
                        BigDecimal.valueOf(leverage),
                        RoundingMode.HALF_UP
                );

        // Margin is ALWAYS released
        usableBalance = usableBalance.add(releasedMargin);

        if (pnl.signum() > 0) {
            // PROFIT → NOT usable today
            unsettledPnL = unsettledPnL.add(pnl);
        } else {
            // LOSS → immediately usable balance reduced
            usableBalance = usableBalance.add(pnl); // pnl is negative
        }

        log.info(
                "SELL executed | Buy={} Sell={} Qty={} PnL={} UsableBalance={} UnsettledPnL={}",
                buyPrice, sellPrice, quantity, pnl, usableBalance, unsettledPnL
        );
    }


    /**
     * ================= READ =================
     */

//    public synchronized BigDecimal getCurrentBalance() {
//        return currentBalance;
//    }


    public BigDecimal getMorningAvailableBalance() {
        return morningAvailableBalance;
    }

    public BigDecimal getUsableBalance() {
        return usableBalance;
    }

    public BigDecimal getRmsBalanceAtBrokerSide() {
        return rmsBalanceAtBrokerSide;
    }
}