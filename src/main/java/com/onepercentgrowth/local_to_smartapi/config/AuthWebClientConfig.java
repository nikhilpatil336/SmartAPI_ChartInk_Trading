package com.onepercentgrowth.local_to_smartapi.config;

import com.onepercentgrowth.local_to_smartapi.properties.BrokerApiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AuthWebClientConfig {

    private final BrokerApiProperties properties;

    public AuthWebClientConfig(BrokerApiProperties properties) {
        this.properties = properties;
    }

    @Bean
    public WebClient authWebClient() {
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}

