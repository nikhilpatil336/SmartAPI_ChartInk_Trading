package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.model.RmsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BalanceService {

    private BigDecimal currentBalance = BigDecimal.ZERO;
//
//    /** Called on app start OR RMS refresh */
//    public synchronized void syncFromRms(RmsData rmsData) {
//        if (rmsData == null) return;
//        this.currentBalance = new BigDecimal(rmsData.getAvailablecash());
//    }
//
//    public synchronized void onBuy(BigDecimal price, int quantity) {
//        currentBalance = currentBalance.subtract(
//                price.multiply(BigDecimal.valueOf(quantity))
//        );
//    }
//
//    public synchronized void onSell(BigDecimal price, int quantity) {
//        currentBalance = currentBalance.add(
//                price.multiply(BigDecimal.valueOf(quantity))
//        );
//    }
//
//    public BigDecimal getCurrentBalance() {
//        return currentBalance;
//    }

    private static final Logger log = LoggerFactory.getLogger(BalanceService.class);

    public synchronized void syncFromRms(RmsData rmsData) {
        if (rmsData == null || rmsData.getAvailablecash() == null) {
            log.warn("RMS sync skipped: data is null");
            return;
        }

        this.currentBalance =
                new BigDecimal(rmsData.getAvailablecash())
                        .setScale(2, RoundingMode.DOWN);

        log.info("Balance synced from RMS: {}", currentBalance);
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

        return currentBalance
                .multiply(BigDecimal.valueOf(usagePercent))
                .setScale(2, RoundingMode.DOWN);
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

        if (required.compareTo(currentBalance) > 0) {
            throw new IllegalStateException(
                    "Insufficient funds. Required="
                            + required + ", Available=" + currentBalance
            );
        }
    }

/** ================= ORDER EVENTS ================= */

    /**
     * Called ONLY when BUY is CONFIRMED (filled)
     */
    public synchronized void onBuy(BigDecimal price, int quantity, BigDecimal oldBalance) {
        BigDecimal cost =
                price.multiply(BigDecimal.valueOf(quantity));

        currentBalance = currentBalance.subtract(cost);

        log.info("BUY executed. Cost={}, Quantity = {}, Old balance = {}, New balance={}",
                cost, quantity, oldBalance, currentBalance);
    }

    /**
     * Called ONLY when SELL or SL is CONFIRMED (filled)
     */
    public synchronized void onSell(BigDecimal price, int quantity, BigDecimal oldBalance) {
        BigDecimal credit =
                price.multiply(BigDecimal.valueOf(quantity));

        currentBalance = currentBalance.add(credit);

        log.info("SELL executed. Credit={}, Quantity = {}, Old balance = {}, New balance={}",
                credit, quantity, oldBalance, currentBalance);
    }

    /**
     * ================= READ =================
     */

    public synchronized BigDecimal getCurrentBalance() {
        return currentBalance;
    }
}



