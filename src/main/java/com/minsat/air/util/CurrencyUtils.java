package com.minsat.air.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CurrencyUtils {

    private CurrencyUtils() {}

    /**
     * Converts AIR's smallest-unit integer string to a decimal string.
     * e.g. toDecimal("25000", 2) → "250.00"
     */
    public static String toDecimal(String smallestUnit, int decimals) {
        if (smallestUnit == null || smallestUnit.isBlank()) {
            return "0." + "0".repeat(decimals);
        }
        BigDecimal value = new BigDecimal(smallestUnit.trim());
        BigDecimal divisor = BigDecimal.TEN.pow(decimals);
        return value.divide(divisor, decimals, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Converts a decimal amount string to AIR's smallest-unit integer string.
     * e.g. toSmallestUnit("10.00", 2) → "1000"
     */
    public static String toSmallestUnit(String decimal, int decimals) {
        if (decimal == null || decimal.isBlank()) {
            return "0";
        }
        BigDecimal value = new BigDecimal(decimal.trim());
        BigDecimal multiplier = BigDecimal.TEN.pow(decimals);
        return value.multiply(multiplier).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
