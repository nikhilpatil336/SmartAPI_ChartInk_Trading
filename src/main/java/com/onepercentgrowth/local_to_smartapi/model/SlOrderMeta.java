package com.onepercentgrowth.local_to_smartapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//@Data
//@AllArgsConstructor
//@NoArgsConstructor
public class SlOrderMeta {
    private String buyOrderId;
    private String slOrderId;
    private int quantity;
    private double triggerPrice;
    private String symbolToken;

    public SlOrderMeta() {
    }

    public SlOrderMeta(String buyOrderId, String slOrderId, int quantity, double triggerPrice, String symbolToken) {
        this.buyOrderId = buyOrderId;
        this.slOrderId = slOrderId;
        this.quantity = quantity;
        this.triggerPrice = triggerPrice;
        this.symbolToken = symbolToken;
    }

    public String getBuyOrderId() {
        return buyOrderId;
    }

    public void setBuyOrderId(String buyOrderId) {
        this.buyOrderId = buyOrderId;
    }

    public String getSlOrderId() {
        return slOrderId;
    }

    public void setSlOrderId(String slOrderId) {
        this.slOrderId = slOrderId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTriggerPrice() {
        return triggerPrice;
    }

    public void setTriggerPrice(double triggerPrice) {
        this.triggerPrice = triggerPrice;
    }

    public String getSymbolToken() {
        return symbolToken;
    }

    public void setSymbolToken(String symbolToken) {
        this.symbolToken = symbolToken;
    }

    @Override
    public String toString() {
        return "SlOrderMeta{" +
                "buyOrderId='" + buyOrderId + '\'' +
                ", slOrderId='" + slOrderId + '\'' +
                ", quantity=" + quantity +
                ", triggerPrice=" + triggerPrice +
                ", symbolToken='" + symbolToken + '\'' +
                '}';
    }
}
