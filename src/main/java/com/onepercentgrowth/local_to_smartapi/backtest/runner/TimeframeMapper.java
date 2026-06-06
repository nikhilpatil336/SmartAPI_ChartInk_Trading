package com.onepercentgrowth.local_to_smartapi.backtest.runner;

import java.util.Map;

public class TimeframeMapper {

    private static final Map<String, String> CODE_TO_API = Map.of(
        "1M",  "ONE_MINUTE",
        "3M",  "THREE_MINUTE",
        "5M",  "FIVE_MINUTE",
        "10M", "TEN_MINUTE",
        "15M", "FIFTEEN_MINUTE",
        "30M", "THIRTY_MINUTE",
        "60M", "ONE_HOUR",
        "1D",  "ONE_DAY"
    );

    public static String toApiInterval(String tfCode) {
        String result = CODE_TO_API.get(tfCode.toUpperCase());
        if (result == null) throw new IllegalArgumentException("Unknown timeframe code: " + tfCode);
        return result;
    }

    public static String toCacheSegment(String date, String tfCode) {
        return date + "_" + tfCode.toUpperCase();
    }
}
