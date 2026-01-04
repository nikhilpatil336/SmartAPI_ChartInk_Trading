package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.swing.*;

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

    public double calculateProfitPrice(double executedPrice) {
        return executedPrice * applicationProperties.getProfitPercentageMultiplier();
    }

    public double calculateStopLossPrice(double executedPrice) {
        return executedPrice * applicationProperties.getStoplossPercentageMultiplier();
    }
}

