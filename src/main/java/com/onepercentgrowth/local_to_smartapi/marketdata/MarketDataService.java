package com.onepercentgrowth.local_to_smartapi.marketdata;

import com.onepercentgrowth.local_to_smartapi.model.MarketQuote;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface MarketDataService {
    Mono<BigDecimal> getLastTradedPrice(String exchange, String symbolToken, String mode);

    public Mono<MarketQuote> getQuote(
            String exchange,
            String symbolToken,
            String mode
    );
}

