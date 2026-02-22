package com.onepercentgrowth.local_to_smartapi.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.onepercentgrowth.local_to_smartapi.deserializer.RmsDataDeserializer;

public class RmsResponse {

    private String status;
    private String message;
    @JsonDeserialize(using = RmsDataDeserializer.class)
    private RmsData data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public RmsData getData() {
        return data;
    }

    public void setData(RmsData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "RmsResponse [status=" + status + ", message=" + message + ", data=" + data + "]";
    }
}
