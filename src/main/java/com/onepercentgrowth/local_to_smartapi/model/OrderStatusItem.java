package com.onepercentgrowth.local_to_smartapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderStatusItem {

    @JsonProperty("orderid")
    private String orderid;

    private String uniqueorderid;
    private String tradingsymbol;
    private String transactiontype;
    private String exchange;

    private String producttype;
    private String ordertype;
    private String variety;

    private String quantity;
    private String filledshares;
    private String unfilledshares;

    private String price;
    private String averageprice;
    private String triggerprice;

    private String status;
    private String orderstatus;
    private String updatetime;

    public String getOrderid() {
        return orderid;
    }

    public void setOrderid(String orderid) {
        this.orderid = orderid;
    }

    public String getUniqueorderid() {
        return uniqueorderid;
    }

    public void setUniqueorderid(String uniqueorderid) {
        this.uniqueorderid = uniqueorderid;
    }

    public String getTradingsymbol() {
        return tradingsymbol;
    }

    public void setTradingsymbol(String tradingsymbol) {
        this.tradingsymbol = tradingsymbol;
    }

    public String getTransactiontype() {
        return transactiontype;
    }

    public void setTransactiontype(String transactiontype) {
        this.transactiontype = transactiontype;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getProducttype() {
        return producttype;
    }

    public void setProducttype(String producttype) {
        this.producttype = producttype;
    }

    public String getOrdertype() {
        return ordertype;
    }

    public void setOrdertype(String ordertype) {
        this.ordertype = ordertype;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getFilledshares() {
        return filledshares;
    }

    public void setFilledshares(String filledshares) {
        this.filledshares = filledshares;
    }

    public String getUnfilledshares() {
        return unfilledshares;
    }

    public void setUnfilledshares(String unfilledshares) {
        this.unfilledshares = unfilledshares;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getAverageprice() {
        return averageprice;
    }

    public void setAverageprice(String averageprice) {
        this.averageprice = averageprice;
    }

    public String getTriggerprice() {
        return triggerprice;
    }

    public void setTriggerprice(String triggerprice) {
        this.triggerprice = triggerprice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrderstatus() {
        return orderstatus;
    }

    public void setOrderstatus(String orderstatus) {
        this.orderstatus = orderstatus;
    }

    public String getUpdatetime() {
        return updatetime;
    }

    public void setUpdatetime(String updatetime) {
        this.updatetime = updatetime;
    }
}
