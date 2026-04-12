package com.onepercentgrowth.local_to_smartapi.config;

import com.onepercentgrowth.local_to_smartapi.properties.BrokerApiProperties;
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Qualifier;
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

//    @Bean
//    public WebClient brokerWebClient() {
//
//        HttpClient httpClient = HttpClient.create()
//                .compress(true)
//                .followRedirect(true)
//                .responseTimeout(Duration.ofMillis(properties.getTimeoutResponse()))
//                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getTimeoutConnect());
//
//        return WebClient.builder()
//                .baseUrl(properties.getDirectBaseUrl())
//                .clientConnector(new ReactorClientHttpConnector(httpClient))
////                .filter(authRetryFilter())
//                .build();
//    }

    private HttpClient createHttpClient() {
        return HttpClient.create()
                .compress(true)
                .followRedirect(true)
                .responseTimeout(Duration.ofMillis(properties.getTimeoutResponse()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getTimeoutConnect());
    }

    @Bean
    @Qualifier("directBrokerWebClient")
    public WebClient directBrokerWebClient() {

        HttpClient httpClient = HttpClient.create()
                .compress(true)
                .followRedirect(true)
                .responseTimeout(Duration.ofMillis(properties.getTimeoutResponse()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getTimeoutConnect());

        return WebClient.builder()
                .baseUrl(properties.getDirectBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    @Qualifier("proxyBrokerWebClient")
    public WebClient proxyBrokerWebClient() {

        HttpClient httpClient = HttpClient.create()
                .compress(true)
                .followRedirect(true)
                .responseTimeout(Duration.ofMillis(properties.getTimeoutResponse()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getTimeoutConnect());

        return WebClient.builder()
                .baseUrl(properties.getProxyBaseUrl())
                .defaultHeader("X-API-KEY", "SecKeyFor1Percent")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}


