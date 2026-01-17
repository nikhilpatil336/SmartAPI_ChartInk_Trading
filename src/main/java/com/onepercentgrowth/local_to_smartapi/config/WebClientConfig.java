package com.onepercentgrowth.local_to_smartapi.config;

import com.onepercentgrowth.local_to_smartapi.properties.BrokerApiProperties;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    private final BrokerApiProperties properties;

    public WebClientConfig(
            BrokerApiProperties properties
    ) {
        this.properties = properties;
    }

    @Bean
    public WebClient brokerWebClient() {

        HttpClient httpClient = HttpClient.create()
                .compress(true)
                .followRedirect(true)
                .responseTimeout(Duration.ofMillis(properties.getTimeoutResponse()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getTimeoutConnect());

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
//                .filter(authRetryFilter())
                .build();
    }
}


