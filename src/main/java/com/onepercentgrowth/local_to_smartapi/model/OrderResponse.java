package com.onepercentgrowth.local_to_smartapi.model;

public class OrderResponse {

    private boolean status;
    private String message;
    private Data data;

    public static class Data {
        private String orderid;

        public String getOrderid() { return orderid; }
        public void setOrderid(String orderid) { this.orderid = orderid; }

        @Override
        public String toString() {
            return "Data{" +
                    "orderid='" + orderid + '\'' +
                    '}';
        }
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

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "OrderResponse{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
