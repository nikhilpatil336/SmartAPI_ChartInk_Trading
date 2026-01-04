package com.onepercentgrowth.local_to_smartapi.model;

import java.util.List;

public class OrderBookResponse {

    private boolean status;
    private String message;
    private List<Object> data; // structure varies; keep as Object

    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Object> getData() { return data; }
    public void setData(List<Object> data) { this.data = data; }

    @Override
    public String toString() {
        return "OrderBookResponse{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
