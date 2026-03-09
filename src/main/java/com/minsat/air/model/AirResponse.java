package com.minsat.air.model;

import java.util.Map;

public record AirResponse(
    boolean success,
    int responseCode,
    String responseMessage,
    String transactionId,
    Map<String, Object> data,
    Map<String, Object> raw
) {

    public static AirResponse networkError(String message) {
        return new AirResponse(false, -1, "Network error: " + message, null, null, null);
    }

    public static AirResponse parseError(String message) {
        return new AirResponse(false, -2, "XML parse error: " + message, null, null, null);
    }

    public static AirResponse validationError(String message) {
        return new AirResponse(false, -3, "Validation error: " + message, null, null, null);
    }
}
