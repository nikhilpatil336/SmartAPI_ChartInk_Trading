package com.onepercentgrowth.local_to_smartapi.model;

public class RmsData {

    private String net;
    private String availablecash;
    private String availableintradaypayin;
    private String availablelimitmargin;
    private String collateral;
    private String m2munrealized;
    private String m2mrealized;
    private String utiliseddebits;
    private String utilisedspan;
    private String utilisedoptionpremium;
    private String utilisedholdingsales;
    private String utilisedexposure;
    private String utilisedturnover;
    private String utilisedpayout;

    public String getNet() {
        return net;
    }

    public void setNet(String net) {
        this.net = net;
    }

    public String getAvailablecash() {
        return availablecash;
    }

    public void setAvailablecash(String availablecash) {
        this.availablecash = availablecash;
    }

    public String getAvailableintradaypayin() {
        return availableintradaypayin;
    }

    public void setAvailableintradaypayin(String availableintradaypayin) {
        this.availableintradaypayin = availableintradaypayin;
    }

    public String getAvailablelimitmargin() {
        return availablelimitmargin;
    }

    public void setAvailablelimitmargin(String availablelimitmargin) {
        this.availablelimitmargin = availablelimitmargin;
    }

    public String getCollateral() {
        return collateral;
    }

    public void setCollateral(String collateral) {
        this.collateral = collateral;
    }

    public String getM2munrealized() {
        return m2munrealized;
    }

    public void setM2munrealized(String m2munrealized) {
        this.m2munrealized = m2munrealized;
    }

    public String getM2mrealized() {
        return m2mrealized;
    }

    public void setM2mrealized(String m2mrealized) {
        this.m2mrealized = m2mrealized;
    }

    public String getUtiliseddebits() {
        return utiliseddebits;
    }

    public void setUtiliseddebits(String utiliseddebits) {
        this.utiliseddebits = utiliseddebits;
    }

    public String getUtilisedspan() {
        return utilisedspan;
    }

    public void setUtilisedspan(String utilisedspan) {
        this.utilisedspan = utilisedspan;
    }

    public String getUtilisedoptionpremium() {
        return utilisedoptionpremium;
    }

    public void setUtilisedoptionpremium(String utilisedoptionpremium) {
        this.utilisedoptionpremium = utilisedoptionpremium;
    }

    public String getUtilisedholdingsales() {
        return utilisedholdingsales;
    }

    public void setUtilisedholdingsales(String utilisedholdingsales) {
        this.utilisedholdingsales = utilisedholdingsales;
    }

    public String getUtilisedexposure() {
        return utilisedexposure;
    }

    public void setUtilisedexposure(String utilisedexposure) {
        this.utilisedexposure = utilisedexposure;
    }

    public String getUtilisedturnover() {
        return utilisedturnover;
    }

    public void setUtilisedturnover(String utilisedturnover) {
        this.utilisedturnover = utilisedturnover;
    }

    public String getUtilisedpayout() {
        return utilisedpayout;
    }

    public void setUtilisedpayout(String utilisedpayout) {
        this.utilisedpayout = utilisedpayout;
    }

    @Override
    public String toString() {
        return "RmsData{" +
                "net='" + net + '\'' +
                ", availablecash='" + availablecash + '\'' +
                ", availableintradaypayin='" + availableintradaypayin + '\'' +
                ", availablelimitmargin='" + availablelimitmargin + '\'' +
                ", collateral='" + collateral + '\'' +
                ", m2munrealized='" + m2munrealized + '\'' +
                ", m2mrealized='" + m2mrealized + '\'' +
                ", utiliseddebits='" + utiliseddebits + '\'' +
                ", utilisedspan='" + utilisedspan + '\'' +
                ", utilisedoptionpremium='" + utilisedoptionpremium + '\'' +
                ", utilisedholdingsales='" + utilisedholdingsales + '\'' +
                ", utilisedexposure='" + utilisedexposure + '\'' +
                ", utilisedturnover='" + utilisedturnover + '\'' +
                ", utilisedpayout='" + utilisedpayout + '\'' +
                '}';
    }
}
