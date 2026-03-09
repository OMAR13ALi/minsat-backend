package com.minsat.air.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtils {

    private static final DateTimeFormatter AIR_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssZ").withZone(ZoneOffset.UTC);

    private static final java.util.regex.Pattern AIR_DATE_PATTERN =
        java.util.regex.Pattern.compile("^(\\d{8})T(\\d{6})([+-]\\d{4})$");

    private DateUtils() {}

    /**
     * Converts to AIR timestamp format: YYYYMMDDTHHmmss+0000
     */
    public static String toAirDate(Instant instant) {
        if (instant == null) {
            instant = Instant.now();
        }
        return AIR_FORMAT.format(instant);
    }

    public static String toAirDateNow() {
        return AIR_FORMAT.format(Instant.now());
    }

    /**
     * Parses an AIR timestamp string back to Instant. Returns null on failure.
     */
    public static Instant fromAirDate(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        try {
            var matcher = AIR_DATE_PATTERN.matcher(str.trim());
            if (matcher.matches()) {
                String isoStr = matcher.group(1).substring(0, 4) + "-"
                    + matcher.group(1).substring(4, 6) + "-"
                    + matcher.group(1).substring(6, 8) + "T"
                    + matcher.group(2).substring(0, 2) + ":"
                    + matcher.group(2).substring(2, 4) + ":"
                    + matcher.group(2).substring(4, 6)
                    + matcher.group(3).substring(0, 3) + ":"
                    + matcher.group(3).substring(3);
                return ZonedDateTime.parse(isoStr).toInstant();
            }
        } catch (DateTimeParseException e) {
            // fall through to null
        }
        return null;
    }
}
