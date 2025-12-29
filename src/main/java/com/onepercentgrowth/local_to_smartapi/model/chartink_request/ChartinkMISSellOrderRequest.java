package com.onepercentgrowth.local_to_smartapi.model.chartink_request;

public class ChartinkMISSellOrderRequest implements IOrderRequest{

    public String variety;

    public String tradingsymbol;

    public String symboltoken;

    public String transactiontype;

    public String exchange;

    public String ordertype;

    public String producttype;

    public String duration;

    public String price;

    public String squareoff;

    public String stoploss;

    public String quantity;


    public String disclosedquantity;

    public String scripconsent;

    public String triggerprice;
//
//    public String orderid;
//
//    public String symbol;
//
//    public String trailingstoploss;
//
//    public String instrumenttype;
//
//    public String strikeprice;
//
//    public String optiontype;
//
//    public String expirydate;
//
//    public String lotsize;

    public ChartinkMISSellOrderRequest() {
    }

    public ChartinkMISSellOrderRequest(String disclosedquantity, String duration, String tradingsymbol, String variety, String ordertype, String triggerprice, String text, String price, String status, String producttype, String exchange, String orderid, String symbol, String updatetime, String exchangetimestamp, String exchangeupdatetimestamp, String averageprice, String transactiontype, String quantity, String squareoff, String stoploss, String trailingstoploss, String symboltoken, String instrumenttype, String strikeprice, String optiontype, String expirydate, String lotsize, String cancelsize, String filledshares, String orderstatus, String unfilledshares, String fillid, String filltime, String uniqueorderid, String scripconsent) {
        this.disclosedquantity = disclosedquantity;
        this.duration = duration;
        this.tradingsymbol = tradingsymbol;
        this.variety = variety;
        this.ordertype = ordertype;
        this.producttype = producttype;
        this.exchange = exchange;
        this.transactiontype = transactiontype;
        this.quantity = quantity;
        this.symboltoken = symboltoken;
        this.scripconsent = scripconsent;
        this.triggerprice = triggerprice;
        this.price = price;
//        this.symbol = symbol;
        this.squareoff = squareoff;
        this.stoploss = stoploss;
//        this.orderid = orderid;
//        this.trailingstoploss = trailingstoploss;
//        this.instrumenttype = instrumenttype;
//        this.strikeprice = strikeprice;
//        this.optiontype = optiontype;
//        this.expirydate = expirydate;
//        this.lotsize = lotsize;
    }

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

    public String getSquareoff() {
        return squareoff;
    }

    public void setSquareoff(String squareoff) {
        this.squareoff = squareoff;
    }

    public String getStoploss() {
        return stoploss;
    }

    public void setStoploss(String stoploss) {
        this.stoploss = stoploss;
    }

    public String getTriggerprice() {
        return triggerprice;
    }

    public void setTriggerprice(String triggerprice) {
        this.triggerprice = triggerprice;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

//    public String getOrderid() {
//        return orderid;
//    }
//
//    public void setOrderid(String orderid) {
//        this.orderid = orderid;
//    }

    //
//    public String getSymbol() {
//        return symbol;
//    }
//
//    public void setSymbol(String symbol) {
//        this.symbol = symbol;
//    }
//
//    public String getTrailingstoploss() {
//        return trailingstoploss;
//    }
//
//    public void setTrailingstoploss(String trailingstoploss) {
//        this.trailingstoploss = trailingstoploss;
//    }
//
//    public String getInstrumenttype() {
//        return instrumenttype;
//    }
//
//    public void setInstrumenttype(String instrumenttype) {
//        this.instrumenttype = instrumenttype;
//    }
//
//    public String getStrikeprice() {
//        return strikeprice;
//    }
//
//    public void setStrikeprice(String strikeprice) {
//        this.strikeprice = strikeprice;
//    }
//
//    public String getOptiontype() {
//        return optiontype;
//    }
//
//    public void setOptiontype(String optiontype) {
//        this.optiontype = optiontype;
//    }
//
//    public String getExpirydate() {
//        return expirydate;
//    }
//
//    public void setExpirydate(String expirydate) {
//        this.expirydate = expirydate;
//    }
//
//    public String getLotsize() {
//        return lotsize;
//    }
//
//    public void setLotsize(String lotsize) {
//        this.lotsize = lotsize;
//    }


    @Override
    public String toString() {
        return "ChartinkMISSellOrderRequest{" +
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
                ", scripconsent='" + scripconsent + '\'' +
                ", triggerprice='" + triggerprice + '\'' +
                ", price='" + price + '\'' +
//                ", symbol='" + symbol + '\'' +
                ", squareoff='" + squareoff + '\'' +
                ", stoploss='" + stoploss + '\'' +
//                ", orderid='" + orderid + '\'' +
//                ", trailingstoploss='" + trailingstoploss + '\'' +
//                ", instrumenttype='" + instrumenttype + '\'' +
//                ", strikeprice='" + strikeprice + '\'' +
//                ", optiontype='" + optiontype + '\'' +
//                ", expirydate='" + expirydate + '\'' +
//                ", lotsize='" + lotsize + '\'' +
                '}';
    }
}
