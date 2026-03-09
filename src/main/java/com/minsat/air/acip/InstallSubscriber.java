package com.minsat.air.acip;

import com.minsat.air.model.AirResponse;
import com.minsat.air.transport.AirTransport;
import com.minsat.air.transport.AirTransportException;
import com.minsat.air.xml.XmlBuilder;
import com.minsat.air.xml.XmlParser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class InstallSubscriber {

    private final XmlBuilder xmlBuilder;
    private final AirTransport airTransport;
    private final XmlParser xmlParser;

    public InstallSubscriber(XmlBuilder xmlBuilder, AirTransport airTransport, XmlParser xmlParser) {
        this.xmlBuilder = xmlBuilder;
        this.airTransport = airTransport;
        this.xmlParser = xmlParser;
    }

    public AirResponse execute(Map<String, Object> params) {
        String subscriberNumber = (String) params.get("subscriberNumber");
        if (subscriberNumber == null || subscriberNumber.isBlank()) {
            throw new IllegalArgumentException("subscriberNumber is required");
        }

        Object serviceClassNew = params.get("serviceClassNew");
        if (serviceClassNew == null) {
            throw new IllegalArgumentException("serviceClassNew is required");
        }

        String transactionId = UUID.randomUUID().toString();
        try {
            java.util.Map<String, Object> airParams = new java.util.LinkedHashMap<>();
            airParams.put("subscriberNumber", subscriberNumber);
            airParams.put("serviceClassNew", serviceClassNew);
            airParams.put("temporaryBlockedFlag",
                params.getOrDefault("temporaryBlockedFlag", false));
            airParams.put("languageIDNew",
                params.getOrDefault("languageIDNew", 1));
            airParams.put("ussdEndOfCallNotificationID",
                params.getOrDefault("ussdEndOfCallNotificationID", 255));

            String xml = xmlBuilder.buildRequest("InstallSubscriber", airParams);
            String rawXml = airTransport.send(xml, "InstallSubscriber");
            return xmlParser.parseResponse(rawXml, transactionId);
        } catch (AirTransportException e) {
            return AirResponse.networkError(e.getMessage());
        } catch (Exception e) {
            return AirResponse.networkError(e.getMessage());
        }
    }
}
