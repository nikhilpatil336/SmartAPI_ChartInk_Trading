package com.onepercentgrowth.local_to_smartapi.model;

public class TradeBookEntry {

    private String exchange;
    private String producttype;
    private String tradingsymbol;
    private String transactiontype;

    private String fillprice;
    private String fillsize;

    private String orderid;
    private String fillid;
    private String filltime;

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getProducttype() { return producttype; }
    public void setProducttype(String producttype) { this.producttype = producttype; }

    public String getTradingsymbol() { return tradingsymbol; }
    public void setTradingsymbol(String tradingsymbol) { this.tradingsymbol = tradingsymbol; }

    public String getTransactiontype() { return transactiontype; }
    public void setTransactiontype(String transactiontype) { this.transactiontype = transactiontype; }

    public String getFillprice() { return fillprice; }
    public void setFillprice(String fillprice) { this.fillprice = fillprice; }

    public String getFillsize() { return fillsize; }
    public void setFillsize(String fillsize) { this.fillsize = fillsize; }

    public String getOrderid() { return orderid; }
    public void setOrderid(String orderid) { this.orderid = orderid; }

    public String getFillid() { return fillid; }
    public void setFillid(String fillid) { this.fillid = fillid; }

    public String getFilltime() { return filltime; }
    public void setFilltime(String filltime) { this.filltime = filltime; }
}
