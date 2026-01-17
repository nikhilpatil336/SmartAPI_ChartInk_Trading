package com.onepercentgrowth.local_to_smartapi.model;

import java.math.BigDecimal;

public record StopLossPrice(
        BigDecimal triggerPrice,
        BigDecimal limitPrice
) {}

