package com.onepercentgrowth.local_to_smartapi.model;

public class OrderResult {

    public static final OrderResult NOT_FOUND = new OrderResult(-1, true);

    private final double result;
    private final boolean terminal;

    private OrderResult(double result, boolean terminal) {
        this.result = result;
        this.terminal = terminal;
    }

    public static OrderResult notFound() {
        return new OrderResult(-1, true);
    }

    public static OrderResult rejected() {
        return new OrderResult(-1, true);
    }

    public static OrderResult completed(double avgPrice) {
        return new OrderResult(avgPrice, true);
    }

    public static OrderResult pending() {
        return new OrderResult(0, false);
    }

    public boolean isTerminal() {
        return terminal;
    }

    public double getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "OrderResult{" +
                "result=" + result +
                ", terminal=" + terminal +
                '}';
    }
}
