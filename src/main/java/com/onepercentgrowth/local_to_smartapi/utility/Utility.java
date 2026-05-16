package com.onepercentgrowth.local_to_smartapi.utility;

import com.onepercentgrowth.local_to_smartapi.service.ScripMasterService;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

        return price
                .divide(tickSize, 0, RoundingMode.HALF_UP)
                .multiply(tickSize)
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

        return price
                .divide(tickSize, 0, RoundingMode.FLOOR)
                .multiply(tickSize)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal roundUpToTick(BigDecimal price, String name, BigDecimal tickSize) {
//        BigDecimal tickSize = new BigDecimal("0.05");
//        BigDecimal tickSize = scripMasterService
//                .getNseEquityMap()
//                .get(name)
//                .getTickSize()
//                .divide(BigDecimal.valueOf(100));

        return price
                .divide(tickSize, 0, RoundingMode.CEILING)
                .multiply(tickSize)
                .setScale(2, RoundingMode.HALF_UP);
    }

}
