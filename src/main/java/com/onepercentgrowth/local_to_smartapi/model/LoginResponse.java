package com.onepercentgrowth.local_to_smartapi.model;

public class LoginResponse {

    private boolean status;
    private String message;
    private Data data;

    public static class Data {
        private String jwtToken;
        private String refreshToken;
        private String feedToken;

        public String getJwtToken() { return jwtToken; }
        public void setJwtToken(String jwtToken) { this.jwtToken = jwtToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String getFeedToken() { return feedToken; }
        public void setFeedToken(String feedToken) { this.feedToken = feedToken; }
    }

    // Getters and setters
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }
}
