package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.model.LoginResponse;
import org.springframework.stereotype.Service;

@Service
public class TokenStorageService {

    private String jwtToken;
    private String refreshToken;
    private String feedToken;

    public synchronized void storeTokens(LoginResponse.Data data) {
        this.jwtToken = data.getJwtToken();
        this.refreshToken = data.getRefreshToken();
        this.feedToken = data.getFeedToken();
    }

    public synchronized String getJwtToken() { return jwtToken; }
    public synchronized String getRefreshToken() { return refreshToken; }
    public synchronized String getFeedToken() { return feedToken; }
}
