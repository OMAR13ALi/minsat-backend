package com.minsat.air.xml;

import com.minsat.config.AirConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class XmlBuilderTest {

    private XmlBuilder xmlBuilder;

    @BeforeEach
    void setUp() {
        AirConfig config = new AirConfig();
        config.setNodeType("EXT");
        config.setHostName("minsat");
        xmlBuilder = new XmlBuilder(config);
    }

    @Test
    void buildsBasicRequest() {
        Map<String, Object> params = Map.of("subscriberNumber", "21369000001");

        String xml = xmlBuilder.buildRequest("GetBalanceAndDate", params);

        assertThat(xml).contains("<?xml version=\"1.0\"?>");
        assertThat(xml).contains("<methodName>GetBalanceAndDate</methodName>");
        assertThat(xml).contains("<n>subscriberNumber</n>");
        assertThat(xml).contains("<string>21369000001</string>");
        assertThat(xml).contains("<n>originNodeType</n>");
        assertThat(xml).contains("<string>EXT</string>");
        assertThat(xml).contains("<n>originHostName</n>");
        assertThat(xml).contains("<n>originTransactionID</n>");
        assertThat(xml).contains("<n>originTimeStamp</n>");
    }

    @Test
    void mapsBooleansCorrectly() {
        Map<String, Object> params = Map.of(
            "subscriberNumber", "21369000001",
            "temporaryBlockedFlag", true
        );

        String xml = xmlBuilder.buildRequest("InstallSubscriber", params);

        assertThat(xml).contains("<boolean>1</boolean>");
    }

    @Test
    void mapsFalseBooleansCorrectly() {
        Map<String, Object> params = Map.of(
            "subscriberNumber", "21369000001",
            "temporaryBlockedFlag", false
        );

        String xml = xmlBuilder.buildRequest("InstallSubscriber", params);

        assertThat(xml).contains("<boolean>0</boolean>");
    }

    @Test
    void mapsIntegersCorrectly() {
        Map<String, Object> params = Map.of(
            "subscriberNumber", "21369000001",
            "serviceClassNew", 201
        );

        String xml = xmlBuilder.buildRequest("InstallSubscriber", params);

        assertThat(xml).contains("<int>201</int>");
    }

    @Test
    void mapsArraysCorrectly() {
        Map<String, Object> params = Map.of(
            "subscriberNumber", "21369000001",
            "fafInformation", List.of(
                Map.of("fafNumber", "21369000002", "owner", "Subscriber")
            )
        );

        String xml = xmlBuilder.buildRequest("UpdateFaFList", params);

        assertThat(xml).contains("<array>");
        assertThat(xml).contains("<data>");
        assertThat(xml).contains("21369000002");
    }

    @Test
    void detectsAirDateFormat() {
        Map<String, Object> params = Map.of(
            "subscriberNumber", "21369000001",
            "supervisionExpiryDate", "20240101T000000+0000"
        );

        String xml = xmlBuilder.buildRequest("UpdateBalanceAndDate", params);

        assertThat(xml).contains("<dateTime.iso8601>20240101T000000+0000</dateTime.iso8601>");
    }

    @Test
    void escapesXmlSpecialChars() {
        Map<String, Object> params = Map.of(
            "subscriberNumber", "21369000001",
            "note", "a & b < c > d"
        );

        String xml = xmlBuilder.buildRequest("GetBalanceAndDate", params);

        assertThat(xml).contains("a &amp; b &lt; c &gt; d");
    }
}
