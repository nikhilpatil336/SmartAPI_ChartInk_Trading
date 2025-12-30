package com.onepercentgrowth.local_to_smartapi.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderStatusData {

    @JsonProperty("orderid")
    private String orderid;

    @JsonProperty("status")
    private String status;

    @JsonProperty("orderstatus")
    private String orderstatus;

    @JsonProperty("tradingsymbol")
    private String tradingsymbol;

    @JsonProperty("transactiontype")
    private String transactiontype;

    @JsonProperty("exchange")
    private String exchange;

    @JsonProperty("price")
    private String price;

    @JsonProperty("quantity")
    private String quantity;

    @JsonProperty("filledshares")
    private String filledshares;

    @JsonProperty("unfilledshares")
    private String unfilledshares;

    @JsonProperty("updatetime")
    private String updatetime;

    public String getOrderid() {
        return orderid;
    }

    public void setOrderid(String orderid) {
        this.orderid = orderid;
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

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
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

    public String getUpdatetime() {
        return updatetime;
    }

    public void setUpdatetime(String updatetime) {
        this.updatetime = updatetime;
    }

    // getters & setters
}
