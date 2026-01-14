package com.onepercentgrowth.local_to_smartapi.config;

import com.onepercentgrowth.local_to_smartapi.exceptions.AuthExpiredException;
import com.onepercentgrowth.local_to_smartapi.properties.BrokerApiProperties;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

//@Configuration
//public class WebClientConfig {
//
//    private final BrokerApiProperties properties;
//
//    public WebClientConfig(BrokerApiProperties properties) {
//        this.properties = properties;
//    }
//
//    @Bean
//    public WebClient brokerWebClient() {
//
//        HttpClient httpClient = HttpClient.create()
//                .compress(true)
//                .followRedirect(true)
//                .responseTimeout(Duration.ofMillis(properties.getTimeoutResponse()))
//                .tcpConfiguration(tcp -> tcp
//                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getTimeoutConnect())
//                );
//
//        return WebClient.builder()
//                .clientConnector(new ReactorClientHttpConnector(httpClient))
//                .baseUrl(properties.getBaseUrl())
//                .build();
//    }
//}

//@Configuration
//public class WebClientConfig {
//
//    private final BrokerApiProperties properties;
//    private final TokenManager tokenManager;
//
//    public WebClientConfig(
//            BrokerApiProperties properties,
//            TokenManager tokenManager
//    ) {
//        this.properties = properties;
//        this.tokenManager = tokenManager;
//    }
//
//    @Bean
//    public WebClient brokerWebClient() {
//
//        HttpClient httpClient = HttpClient.create()
//                .compress(true)
//                .followRedirect(true)
//                .responseTimeout(Duration.ofMillis(properties.getTimeoutResponse()))
//                .tcpConfiguration(tcp -> tcp
//                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
//                                properties.getTimeoutConnect())
//                );
//
//        return WebClient.builder()
//                .baseUrl(properties.getBaseUrl())
//                .clientConnector(new ReactorClientHttpConnector(httpClient))
//
//                // 🔐 Inject JWT automatically
//                .filter(authHeaderFilter())
//
//                // ♻ Retry once on auth failure
//                .filter(authRetryFilter())
//
//                .build();
//    }
//
//
//    private ExchangeFilterFunction authHeaderFilter() {
//        return (request, next) -> {
//            ClientRequest newRequest = ClientRequest.from(request)
//                    .header("Authorization",
//                            "Bearer " + tokenManager.getValidJwtToken())
//                    .build();
//            return next.exchange(newRequest);
//        };
//    }
//
//    private ExchangeFilterFunction authRetryFilter() {
//        return (request, next) ->
//                next.exchange(request)
//                        .flatMap(response -> {
//
//                            // 401 / 403 → force refresh
//                            if (response.statusCode().value() == 401 ||
//                                    response.statusCode().value() == 403) {
//
//                                return tokenManager.refreshTokens()
//                                        .then(next.exchange(request));
//                            }
//
//                            return Mono.just(response);
//                        });
//    }
//}

@Configuration
public class WebClientConfig {

    private final BrokerApiProperties properties;
//    private final TokenManager tokenManager;

//    public WebClientConfig(
//            BrokerApiProperties properties,
//            TokenManager tokenManager
//    ) {
//        this.properties = properties;
//        this.tokenManager = tokenManager;
//    }

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

//    private ExchangeFilterFunction authRetryFilter() {
//        return (request, next) ->
//                next.exchange(request)
//                        .onErrorResume(
//                                AuthExpiredException.class,
//                                ex -> tokenManager.refreshTokens()
//                                        .then(next.exchange(request))
//                        );
//    }
}


