package com.onepercentgrowth.local_to_smartapi.model.chartink_request;

public class ChartinkCancelOrderRequest implements IOrderRequest{
    private String variety;   // e.g. "NORMAL"
    private String orderid;

    public ChartinkCancelOrderRequest(String variety, String orderid) {
        this.variety = variety;
        this.orderid = orderid;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getOrderid() {
        return orderid;
    }

    public void setOrderid(String orderid) {
        this.orderid = orderid;
    }
}
