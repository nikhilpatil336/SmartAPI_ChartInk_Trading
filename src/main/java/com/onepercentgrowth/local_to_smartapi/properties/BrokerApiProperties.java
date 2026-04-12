package com.onepercentgrowth.local_to_smartapi.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "broker.api")
public class BrokerApiProperties {

    private String directBaseUrl;
    private String proxyBaseUrl;
    private int timeoutConnect;
    private int timeoutResponse;

    public String getDirectBaseUrl() {
        return directBaseUrl;
    }

    public void setDirectBaseUrl(String directBaseUrl) {
        this.directBaseUrl = directBaseUrl;
    }

    public String getProxyBaseUrl() {
        return proxyBaseUrl;
    }

    public void setProxyBaseUrl(String proxyBaseUrl) {
        this.proxyBaseUrl = proxyBaseUrl;
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
