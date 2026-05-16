package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.model.StopLossPrice;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.utility.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.onepercentgrowth.local_to_smartapi.utility.Utility.roundToTick;

@Service
public class OrderCalculationService {

    private static final Logger log = LoggerFactory.getLogger(OrderCalculationService.class);

    private final ApplicationProperties applicationProperties;

    @Autowired
    private ScripMasterService scripMasterService;

    public OrderCalculationService(ApplicationProperties props) {
        this.applicationProperties = props;
    }

    public int calculateQuantityForLongBuy(double availableCash, double triggerPrice) {
        double usableCash = availableCash * applicationProperties.getPercentBalanceUse();
        int qty = (int) Math.floor(usableCash / triggerPrice)
                - applicationProperties.getNumberOfStocksBuyLessForLong();

        log.info("Calculated quantity based on available cash and stock price: {}", qty);

        if (qty <= applicationProperties.getStockBuyMinimumQuantityRequired()) {
            throw new IllegalStateException("Insufficient quantity");
        }
        return qty;
    }

    public int calculateQuantityForShortSell(double availableCash, double triggerPrice) {
        double usableCash = availableCash * applicationProperties.getPercentBalanceUse();
        int qty = (int) Math.floor(usableCash / triggerPrice)
                - applicationProperties.getNumberOfStocksSellLessForShort();

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

    public int calculateBuyQuantity(
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

    public int calculateSellQuantity(
            BigDecimal usableCash,
            BigDecimal maxBuyPrice,
            int leverageMultiplier,
            int maxLeverage
    ) {
        // usableCash / triggerPrice
        BigDecimal baseQuantity =
                usableCash.divide(maxBuyPrice, 8, RoundingMode.FLOOR);

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

    public BigDecimal calculateBuyProfitPrice(BigDecimal executedPrice, String stockName) {
        double buyProfitPercentageMultipler = applicationProperties.getBuyProfitPercentageMultiplier();

        if(buyProfitPercentageMultipler < 1)
        {
            log.error("buyProfitPercentageMultipler is less than 1, which is wrong so choosing the default value from properties");
            buyProfitPercentageMultipler = applicationProperties.getDefaultBuyProfitPercentageMultiplier();
        }

        BigDecimal multiplier =
                BigDecimal.valueOf(buyProfitPercentageMultipler);

        return Utility.roundUpToTick(executedPrice.multiply(multiplier), stockName, scripMasterService.getNseEquityMap().get(stockName).getTickSize());
    }

    public BigDecimal calculateBuyStopLossPrice(BigDecimal executedPrice, String stockName) {
        double buyStoplossPercentageMultiplier = applicationProperties.getBuyStoplossPercentageMultiplier();

        if(buyStoplossPercentageMultiplier > 1)
        {
            log.error("buyStoplossPercentageMultiplier is greater than 1, which is wrong so choosing the default value from properties");
            buyStoplossPercentageMultiplier = applicationProperties.getDefaultBuyStoplossPercentageMultiplier();
        }

        BigDecimal multiplier =
                BigDecimal.valueOf(buyStoplossPercentageMultiplier);

        return Utility.roundDownToTick(executedPrice.multiply(multiplier), stockName, scripMasterService.getNseEquityMap().get(stockName).getTickSize());
    }

    public BigDecimal calculateSellProfitPrice(BigDecimal executedPrice, String stockName) {
        double sellProfitPercentageMultiplier = applicationProperties.getSellProfitPercentageMultiplier();

        if(sellProfitPercentageMultiplier > 1) {
            log.error("sellProfitPercentageMultiplier is greater than 1, which is wrong so choosing the default value from properties");
            sellProfitPercentageMultiplier = applicationProperties.getDefaultSellProfitPercentageMultiplier();
        }

        BigDecimal multiplier =
                BigDecimal.valueOf(sellProfitPercentageMultiplier);

        return Utility.roundDownToTick(executedPrice.multiply(multiplier), stockName, scripMasterService.getNseEquityMap().get(stockName).getTickSize());
    }

    public BigDecimal calculateSellStopLossPrice(BigDecimal executedPrice, String stockName) {
        double sellStoplossPercentageMultiplier = applicationProperties.getSellStoplossPercentageMultiplier();

        if(sellStoplossPercentageMultiplier < 1) {
            log.error("sellStoplossPercentageMultiplier is less than 1, which is wrong so choosing the default value from properties");
            sellStoplossPercentageMultiplier = applicationProperties.getDefaultSellStoplossPercentageMultiplier();
        }

        BigDecimal multiplier =
                BigDecimal.valueOf(sellStoplossPercentageMultiplier);

        return Utility.roundUpToTick(executedPrice.multiply(multiplier), stockName, scripMasterService.getNseEquityMap().get(stockName).getTickSize());
    }


    public StopLossPrice calculateStopLossPrice(
            BigDecimal buyPrice,
            BigDecimal slPercent,
            BigDecimal bufferPercent,
            String stockName
    ) {
        BigDecimal logicalSL =
                buyPrice.multiply(BigDecimal.ONE.subtract(slPercent));

        BigDecimal limitPrice =
                logicalSL.multiply(BigDecimal.ONE.subtract(bufferPercent));

        return new StopLossPrice(
                roundToTick(logicalSL, stockName, scripMasterService.getNseEquityMap().get(stockName).getTickSize()),
                roundToTick(limitPrice, stockName, scripMasterService.getNseEquityMap().get(stockName).getTickSize())
        );
    }

    public StopLossPrice calculateShortStopLossPrice(
            BigDecimal buyPrice,
            BigDecimal slPercent,
            BigDecimal bufferPercent,
            String stockName
    ) {
        BigDecimal logicalSL =
                buyPrice.multiply(BigDecimal.ONE.add(slPercent));

        BigDecimal limitPrice =
                logicalSL.multiply(BigDecimal.ONE.add(bufferPercent));

        return new StopLossPrice(
                roundToTick(logicalSL, stockName, scripMasterService.getNseEquityMap().get(stockName).getTickSize()),
                roundToTick(limitPrice, stockName, scripMasterService.getNseEquityMap().get(stockName).getTickSize())
        );
    }

}

