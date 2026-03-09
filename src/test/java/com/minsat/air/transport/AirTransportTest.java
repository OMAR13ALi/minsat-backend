package com.minsat.air.transport;

import com.minsat.config.AirConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests AirTransport with a non-existent host to verify exception handling.
 * Real integration tests require a live AIR server.
 */
class AirTransportTest {

    private AirTransport airTransport;

    @BeforeEach
    void setUp() {
        AirConfig config = new AirConfig();
        config.setHost("127.0.0.1");
        config.setPort(19999); // Nothing listening here
        config.setUser("test");
        config.setPassword("test");
        config.setTimeoutMs(1000);
        config.setDebug(false);
        airTransport = new AirTransport(config);
    }

    @Test
    void throwsAirTransportExceptionOnConnectionRefused() {
        assertThatThrownBy(() ->
            airTransport.send("<?xml version=\"1.0\"?><methodCall></methodCall>", "TestMethod")
        ).isInstanceOf(AirTransportException.class);
    }
}
