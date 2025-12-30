package com.onepercentgrowth.local_to_smartapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

//@Data
public class OrderStatusResponse {

    private boolean status;
    private String message;
    private String errorcode;
    private OrderStatusData data;

    public OrderStatusResponse() {
    }

    public OrderStatusResponse(boolean status, String message, String errorcode, OrderStatusData data) {
        this.status = status;
        this.message = message;
        this.errorcode = errorcode;
        this.data = data;
    }

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

    public String getErrorcode() {
        return errorcode;
    }

    public void setErrorcode(String errorcode) {
        this.errorcode = errorcode;
    }

    public OrderStatusData getData() {
        return data;
    }

    public void setData(OrderStatusData data) {
        this.data = data;
    }
}

