package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.model.StopLossPrice;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.utility.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.onepercentgrowth.local_to_smartapi.utility.Utility.roundToTick;

@Service
public class OrderCalculationService {

    private static final Logger log = LoggerFactory.getLogger(OrderCalculationService.class);

    private final ApplicationProperties applicationProperties;

    public OrderCalculationService(ApplicationProperties props) {
        this.applicationProperties = props;
    }

    public int calculateQuantity(double availableCash, double triggerPrice) {
        double usableCash = availableCash * applicationProperties.getPercentBalanceUse();
        int qty = (int) Math.floor(usableCash / triggerPrice)
                - applicationProperties.getNumberOfStocksBuyLess();

        log.info("Calculated quantity based on available cash and stock price: {}", qty);

        if (qty <= applicationProperties.getStockBuyMinimumQuantityRequired()) {
            throw new IllegalStateException("Insufficient quantity");
        }
        return qty;
    }

//    public int calculateQuantity(double usableCash, double triggerPrice, int leverageMultiplier, int maxLeverage) {
////        double usableCash = availableCash * applicationProperties.getPercentBalanceUse();
////        int qty = (int) Math.floor(usableCash / triggerPrice)
////                - applicationProperties.getNumberOfStocksBuyLess();
//
//        double calculateQuantity = usableCash / triggerPrice;
//        double quantityAfterLeverage = calculateQuantity * leverageMultiplier;
//        int absQuantity = (int) Math.floor(Math.abs(quantityAfterLeverage));
//
//        log.info("Calculated quantity based on available cash and stock price: {}", absQuantity);
//
//        if (absQuantity <= applicationProperties.getStockBuyMinimumQuantityRequired()) {
//            throw new IllegalStateException("Insufficient quantity");
//        }
//        return absQuantity;
//    }

    public int calculateQuantity(
            BigDecimal usableCash,
            BigDecimal triggerPrice,
            int leverageMultiplier,
            int maxLeverage
    ) {
        // usableCash / triggerPrice
        BigDecimal baseQuantity =
                usableCash.divide(triggerPrice, 8, RoundingMode.FLOOR);

        // apply leverage
        BigDecimal leveragedQuantity =
                baseQuantity.multiply(BigDecimal.valueOf(leverageMultiplier));

        // absolute + floor + int
        int absQuantity =
                leveragedQuantity.abs().setScale(0, RoundingMode.FLOOR).intValueExact();

        log.info("Calculated quantity based on available cash and stock price: {}", absQuantity);

        if (absQuantity <= applicationProperties.getStockBuyMinimumQuantityRequired()) {
            throw new IllegalStateException("Insufficient quantity");
        }

        return absQuantity;
    }


//    public double calculateProfitPrice(double executedPrice) {
//        return Utility.roundUpToTick(executedPrice * applicationProperties.getProfitPercentageMultiplier());
//    }
//
//    public double calculateStopLossPrice(double executedPrice) {
//        return Utility.roundDownToTick(executedPrice * applicationProperties.getStoplossPercentageMultiplier());
//    }

    public BigDecimal calculateProfitPrice(BigDecimal executedPrice) {
        BigDecimal multiplier =
                BigDecimal.valueOf(applicationProperties.getProfitPercentageMultiplier());

        return Utility.roundUpToTick(executedPrice.multiply(multiplier));
    }

    public BigDecimal calculateStopLossPrice(BigDecimal executedPrice) {
        BigDecimal multiplier =
                BigDecimal.valueOf(applicationProperties.getStoplossPercentageMultiplier());

        return Utility.roundDownToTick(executedPrice.multiply(multiplier));
    }


    public StopLossPrice calculateStopLossPrice(
            BigDecimal buyPrice,
            BigDecimal slPercent,
            BigDecimal bufferPercent
    ) {
        BigDecimal logicalSL =
                buyPrice.multiply(BigDecimal.ONE.subtract(slPercent));

        BigDecimal limitPrice =
                logicalSL.multiply(BigDecimal.ONE.subtract(bufferPercent));

        return new StopLossPrice(
                roundToTick(logicalSL),
                roundToTick(limitPrice)
        );
    }

}

