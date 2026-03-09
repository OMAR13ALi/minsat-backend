package com.minsat.air.transport;

public class AirTransportException extends RuntimeException {

    public AirTransportException(String message) {
        super(message);
    }

    public AirTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
