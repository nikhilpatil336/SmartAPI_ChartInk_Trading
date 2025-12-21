package com.onepercentgrowth.local_to_smartapi.model;
//
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

//@Data
//@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderStatusData {

    private String orderid;
    private String orderstatus;

//    @JsonProperty("avgprice")
    private Double avgprice;

    private Integer quantity;

    public boolean isCompleted() {
        return "COMPLETE".equalsIgnoreCase(orderstatus);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(orderstatus)
                || "CANCELLED".equalsIgnoreCase(orderstatus);
    }

    public double getExecutedPrice() {
        return avgprice != null ? avgprice : 0.0;
    }


    public String getOrderid() {
        return orderid;
    }

    public void setOrderid(String orderid) {
        this.orderid = orderid;
    }

    public String getOrderstatus() {
        return orderstatus;
    }

    public void setOrderstatus(String orderstatus) {
        this.orderstatus = orderstatus;
    }

    public Double getAvgprice() {
        return avgprice;
    }

    public void setAvgprice(Double avgprice) {
        this.avgprice = avgprice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
