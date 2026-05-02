package com.onepercentgrowth.local_to_smartapi.historicdata;


import java.util.List;

public class HistoricalDataResponse {
    private boolean status;
    private String message;
    private String errorcode;
    private List<List<Object>> data; // raw data

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

    public List<List<Object>> getData() {
        return data;
    }

    public void setData(List<List<Object>> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "HistoricalDataResponse{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", errorcode='" + errorcode + '\'' +
                ", data=" + data +
                '}';
    }
}
