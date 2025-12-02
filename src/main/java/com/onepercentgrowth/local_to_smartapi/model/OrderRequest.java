package com.onepercentgrowth.local_to_smartapi.model;

public class OrderRequest {

    public String disclosedquantity;

    public String duration;

    public String tradingsymbol;

    public String variety;

    public String ordertype;

    public String producttype;

    public String exchange;

    public String transactiontype;

    public String quantity;

    public String symboltoken;

    public String scripconsent;

    public String getDisclosedquantity() {
        return disclosedquantity;
    }

    public void setDisclosedquantity(String disclosedquantity) {
        this.disclosedquantity = disclosedquantity;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getTradingsymbol() {
        return tradingsymbol;
    }

    public void setTradingsymbol(String tradingsymbol) {
        this.tradingsymbol = tradingsymbol;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getOrdertype() {
        return ordertype;
    }

    public void setOrdertype(String ordertype) {
        this.ordertype = ordertype;
    }

    public String getProducttype() {
        return producttype;
    }

    public void setProducttype(String producttype) {
        this.producttype = producttype;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getTransactiontype() {
        return transactiontype;
    }

    public void setTransactiontype(String transactiontype) {
        this.transactiontype = transactiontype;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getSymboltoken() {
        return symboltoken;
    }

    public void setSymboltoken(String symboltoken) {
        this.symboltoken = symboltoken;
    }

    public String getScripconsent() {
        return scripconsent;
    }

    public void setScripconsent(String scripconsent) {
        this.scripconsent = scripconsent;
    }

    @Override
    public String toString() {
        return "OrderRequest{" +
                "disclosedquantity='" + disclosedquantity + '\'' +
                ", duration='" + duration + '\'' +
                ", tradingsymbol='" + tradingsymbol + '\'' +
                ", variety='" + variety + '\'' +
                ", ordertype='" + ordertype + '\'' +
                ", producttype='" + producttype + '\'' +
                ", exchange='" + exchange + '\'' +
                ", transactiontype='" + transactiontype + '\'' +
                ", quantity='" + quantity + '\'' +
                ", symboltoken='" + symboltoken + '\'' +
                '}';
    }
}
