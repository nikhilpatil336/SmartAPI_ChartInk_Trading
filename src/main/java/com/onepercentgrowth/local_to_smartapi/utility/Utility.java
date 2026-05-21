package com.onepercentgrowth.local_to_smartapi.utility;

import com.onepercentgrowth.local_to_smartapi.service.ScripMasterService;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Utility {

    private Utility() {}

    public static String normalize(String tradingSymbol) {
        int idx = tradingSymbol.indexOf('-');
        return (idx == -1) ? tradingSymbol : tradingSymbol.substring(0, idx);
    }

    public static String ensureEqSuffix(String symbol) {
        return symbol.matches(".+-[A-Z]+$") ? symbol : symbol + "-EQ";
    }

//    public static double roundToTick(double price) {
////        return Math.round(price / 0.05) * 0.05;
//        return BigDecimal
//                .valueOf(Math.round(price / 0.05) * 0.05)
//                .setScale(2, RoundingMode.HALF_UP)
//                .doubleValue();
//    }

    public static BigDecimal roundToTick(BigDecimal price, String name, BigDecimal tickSize) {
//        BigDecimal tickSize = new BigDecimal("0.05");
//        BigDecimal tickSize = scripMasterService
//                .getNseEquityMap()
//                .get(name)
//                .getTickSize()
//                .divide(BigDecimal.valueOf(100));

        BigDecimal adjustedTickSize = tickSize.divide(
                BigDecimal.valueOf(100),
                10,
                RoundingMode.HALF_UP
        );

        return price
                .divide(adjustedTickSize, 0, RoundingMode.HALF_UP)
                .multiply(adjustedTickSize)
                .setScale(2, RoundingMode.HALF_UP);
    }



//    public static double roundDownToTick(double price) {
////        return Math.floor(price / 0.05) * 0.05;
//        return BigDecimal
//                .valueOf(Math.floor(price / 0.05) * 0.05)
//                .setScale(2, RoundingMode.HALF_UP)
//                .doubleValue();
//    }
//    public static double roundUpToTick(double price) {
//        return BigDecimal
//                .valueOf(Math.ceil(price / 0.05) * 0.05)
//                .setScale(2, RoundingMode.HALF_UP)
//                .doubleValue();
//    }


    public static BigDecimal roundDownToTick(BigDecimal price, String name, BigDecimal tickSize) {
//        BigDecimal tickSize = new BigDecimal("0.05");
//        BigDecimal tickSize = scripMasterService
//                .getNseEquityMap()
//                .get(name)
//                .getTickSize()
//                .divide(BigDecimal.valueOf(100));

        BigDecimal adjustedTickSize = tickSize.divide(
                BigDecimal.valueOf(100),
                10,
                RoundingMode.HALF_UP
        );

        return price
                .divide(adjustedTickSize, 0, RoundingMode.FLOOR)
                .multiply(adjustedTickSize)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal roundUpToTick(BigDecimal price, String name, BigDecimal tickSize) {
//        BigDecimal tickSize = new BigDecimal("0.05");
//        BigDecimal tickSize = scripMasterService
//                .getNseEquityMap()
//                .get(name)
//                .getTickSize()
//                .divide(BigDecimal.valueOf(100));

        BigDecimal adjustedTickSize = tickSize.divide(
                BigDecimal.valueOf(100),
                10,
                RoundingMode.HALF_UP
        );

        return price
                .divide(adjustedTickSize, 0, RoundingMode.CEILING)
                .multiply(adjustedTickSize)
                .setScale(2, RoundingMode.HALF_UP);
    }


    public static LocalDateTime parseTriggeredAt(
            String triggeredAt
    ) {

        // remove extra spaces
        String cleaned = triggeredAt
                .trim()
                .replaceAll("\\s+", " ")
                .replace(": ", ":")
                .toUpperCase();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "h:mm a",
                        Locale.ENGLISH
                );

        LocalTime time =
                LocalTime.parse(
                        cleaned,
                        formatter
                );

        return LocalDate.now(ZoneId.of("Asia/Kolkata"))
                .atTime(time)
                .withSecond(0)
                .withNano(0);
    }

    public static LocalDateTime normalizeToCandleTime(
            LocalDateTime triggerTime
    ) {

        int minute = triggerTime.getMinute();

        int normalizedMinute = (minute / 5) * 5;

        return triggerTime
                .withMinute(normalizedMinute)
                .withSecond(0)
                .withNano(0);
    }

}
