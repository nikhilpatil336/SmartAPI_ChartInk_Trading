package com.onepercentgrowth.local_to_smartapi.model;

public class WebhookRequest {

    private String stocks;
    private String trigger_prices;
    private String triggered_at;
    private String scan_name;
    private String scan_url;
    private String alert_name;
    private String webhook_url;

    public String getStocks() {
        return stocks;
    }

    public void setStocks(String stocks) {
        this.stocks = stocks;
    }

    public String getTrigger_prices() {
        return trigger_prices;
    }

    public void setTrigger_prices(String trigger_prices) {
        this.trigger_prices = trigger_prices;
    }

    public String getTriggered_at() {
        return triggered_at;
    }

    public void setTriggered_at(String triggered_at) {
        this.triggered_at = triggered_at;
    }

    public String getScan_name() {
        return scan_name;
    }

    public void setScan_name(String scan_name) {
        this.scan_name = scan_name;
    }

    public String getScan_url() {
        return scan_url;
    }

    public void setScan_url(String scan_url) {
        this.scan_url = scan_url;
    }

    public String getAlert_name() {
        return alert_name;
    }

    public void setAlert_name(String alert_name) {
        this.alert_name = alert_name;
    }

    public String getWebhook_url() {
        return webhook_url;
    }

    public void setWebhook_url(String webhook_url) {
        this.webhook_url = webhook_url;
    }

    @Override
    public String toString() {
        return "WebhookRequest{" +
                "stocks='" + stocks + '\'' +
                ", trigger_prices='" + trigger_prices + '\'' +
                ", triggered_at='" + triggered_at + '\'' +
                ", scan_name='" + scan_name + '\'' +
                ", scan_url='" + scan_url + '\'' +
                ", alert_name='" + alert_name + '\'' +
                ", webhook_url='" + webhook_url + '\'' +
                '}';
    }
}
