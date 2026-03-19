package com.onepercentgrowth.local_to_smartapi.marketdata;

import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.model.MarketQuote;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
public class SmartApiMarketDataService implements MarketDataService {

    private final BrokerApiClient brokerApiClient;
    private final TokenManager tokenManager;
    private final ApplicationProperties applicationProperties;

    public SmartApiMarketDataService(
            BrokerApiClient brokerApiClient,
            TokenManager tokenManager,
            ApplicationProperties applicationProperties
    ) {
        this.brokerApiClient = brokerApiClient;
        this.tokenManager = tokenManager;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public Mono<BigDecimal> getLastTradedPrice(String exchange, String symbolToken, String mode) {

        return brokerApiClient.getLtp(
                exchange,
                symbolToken,
                tokenManager.getValidJwtToken(),
                mode
//                applicationProperties.getExitLtpMode()
        );
    }

    @Override
    public Mono<MarketQuote> getQuote(
            String exchange,
            String symbolToken,
            String mode
    ) {

        return brokerApiClient.getQuote(
                exchange,
                symbolToken,
                tokenManager.getValidJwtToken(),
                mode
        );
    }
}
