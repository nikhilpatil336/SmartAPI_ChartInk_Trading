package com.onepercentgrowth.local_to_smartapi.model;


public class LoginRequest {

    private String clientcode;
    private String password;
    private String totp;   // optional, if 2FA is enabled
    private String state;  // optional

    // Getters and Setters
    public String getClientcode() { return clientcode; }
    public void setClientcode(String clientcode) { this.clientcode = clientcode; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTotp() { return totp; }
    public void setTotp(String totp) { this.totp = totp; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "clientcode='" + clientcode + '\'' +
                ", password='" + password + '\'' +
                ", totp='" + totp + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}
