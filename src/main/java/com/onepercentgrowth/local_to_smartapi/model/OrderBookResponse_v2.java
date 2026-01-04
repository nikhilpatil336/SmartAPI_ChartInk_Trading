package com.onepercentgrowth.local_to_smartapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

//@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderBookResponse_v2 {

    private boolean status;
    private String message;

    private List<OrderStatusItem> data;

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<OrderStatusItem> getData() {
        return data;
    }

    public void setData(List<OrderStatusItem> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "OrderBookResponse_v2{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
