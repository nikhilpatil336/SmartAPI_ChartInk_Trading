package com.onepercentgrowth.local_to_smartapi.backtest.runner;

public record AlertEntry(String triggeredAt, String stock, double firstAlertPrice) {}
