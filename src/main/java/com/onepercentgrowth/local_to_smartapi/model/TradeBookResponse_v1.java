package com.onepercentgrowth.local_to_smartapi.model;

import java.util.List;

public class TradeBookResponse_v1 {

    private boolean status;
    private String message;
    private String errorcode;
    private List<TradeBookEntry> data;

    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getErrorcode() { return errorcode; }
    public void setErrorcode(String errorcode) { this.errorcode = errorcode; }

    public List<TradeBookEntry> getData() { return data; }
    public void setData(List<TradeBookEntry> data) { this.data = data; }
}
