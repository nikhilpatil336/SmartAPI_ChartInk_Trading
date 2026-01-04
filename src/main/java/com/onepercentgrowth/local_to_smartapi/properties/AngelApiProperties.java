package com.onepercentgrowth.local_to_smartapi.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "angel")
public class AngelApiProperties {

    private String baseUrl;
    private String privateKey;
    private String clientLocalIp;
    private String clientPublicIp;
    private String clientMacAddress;
    private String sourceId;
    private String userType;
    private String totpCode;
    private String clientId;
    private String password;

    // Getters and Setters
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }

    public String getClientLocalIp() { return clientLocalIp; }
    public void setClientLocalIp(String clientLocalIp) { this.clientLocalIp = clientLocalIp; }

    public String getClientPublicIp() { return clientPublicIp; }
    public void setClientPublicIp(String clientPublicIp) { this.clientPublicIp = clientPublicIp; }

    public String getClientMacAddress() { return clientMacAddress; }
    public void setClientMacAddress(String clientMacAddress) { this.clientMacAddress = clientMacAddress; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getTotpCode() { return totpCode; }
    public void setTotpCode(String totpCode) { this.totpCode = totpCode; }

    public String getClientId() {
        return clientId;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
