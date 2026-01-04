package com.onepercentgrowth.local_to_smartapi.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "broker.api")
public class BrokerApiProperties {

    private String baseUrl;
    private int timeoutConnect;
    private int timeoutResponse;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeoutConnect() {
        return timeoutConnect;
    }

    public void setTimeoutConnect(int timeoutConnect) {
        this.timeoutConnect = timeoutConnect;
    }

    public int getTimeoutResponse() {
        return timeoutResponse;
    }

    public void setTimeoutResponse(int timeoutResponse) {
        this.timeoutResponse = timeoutResponse;
    }
}
