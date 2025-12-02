package com.onepercentgrowth.local_to_smartapi.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    private final BrokerApiProperties properties;

    @Autowired
    public WebClientConfig(BrokerApiProperties properties) {
        this.properties = properties;
    }

    @Bean
    public WebClient brokerWebClient() {

        HttpClient httpClient = HttpClient.create()
                .compress(true)
                .followRedirect(true)
                .responseTimeout(Duration.ofMillis(properties.getTimeoutResponse()))
                .tcpConfiguration(tcp -> tcp
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getTimeoutConnect())
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}
