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
public class LinkSubordinateSubscriber {

    private final XmlBuilder xmlBuilder;
    private final AirTransport airTransport;
    private final XmlParser xmlParser;

    public LinkSubordinateSubscriber(XmlBuilder xmlBuilder, AirTransport airTransport, XmlParser xmlParser) {
        this.xmlBuilder = xmlBuilder;
        this.airTransport = airTransport;
        this.xmlParser = xmlParser;
    }

    public AirResponse execute(Map<String, Object> params) {
        String subscriberNumber = (String) params.get("subscriberNumber");
        if (subscriberNumber == null || subscriberNumber.isBlank()) {
            throw new IllegalArgumentException("subscriberNumber is required");
        }

        String masterAccountNumber = (String) params.get("masterAccountNumber");
        if (masterAccountNumber == null || masterAccountNumber.isBlank()) {
            throw new IllegalArgumentException("masterAccountNumber is required");
        }

        if (subscriberNumber.equals(masterAccountNumber)) {
            throw new IllegalArgumentException("subscriberNumber and masterAccountNumber must be different");
        }

        String transactionId = UUID.randomUUID().toString();
        try {
            java.util.Map<String, Object> airParams = new java.util.LinkedHashMap<>();
            airParams.put("subscriberNumber", subscriberNumber);
            airParams.put("masterAccountNumber", masterAccountNumber);

            Object originOperatorId = params.get("originOperatorID");
            if (originOperatorId != null) airParams.put("originOperatorID", originOperatorId);

            String xml = xmlBuilder.buildRequest("LinkSubordinateSubscriber", airParams);
            String rawXml = airTransport.send(xml, "LinkSubordinateSubscriber");
            return xmlParser.parseResponse(rawXml, transactionId);
        } catch (AirTransportException e) {
            return AirResponse.networkError(e.getMessage());
        } catch (Exception e) {
            return AirResponse.networkError(e.getMessage());
        }
    }
}
