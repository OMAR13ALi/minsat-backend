package com.minsat.air.ucip;

import com.minsat.air.model.AirResponse;
import com.minsat.air.transport.AirTransport;
import com.minsat.air.transport.AirTransportException;
import com.minsat.air.xml.XmlBuilder;
import com.minsat.air.xml.XmlParser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class UpdateAccountDetails {

    private final XmlBuilder xmlBuilder;
    private final AirTransport airTransport;
    private final XmlParser xmlParser;

    public UpdateAccountDetails(XmlBuilder xmlBuilder, AirTransport airTransport, XmlParser xmlParser) {
        this.xmlBuilder = xmlBuilder;
        this.airTransport = airTransport;
        this.xmlParser = xmlParser;
    }

    public AirResponse execute(Map<String, Object> params) {
        String subscriberNumber = (String) params.get("subscriberNumber");
        if (subscriberNumber == null || subscriberNumber.isBlank()) {
            throw new IllegalArgumentException("subscriberNumber is required");
        }

        Object eocnId = params.get("ussdEndOfCallNotificationId");
        if (eocnId == null) {
            throw new IllegalArgumentException("At least one field to update must be provided");
        }

        String transactionId = UUID.randomUUID().toString();
        try {
            java.util.Map<String, Object> airParams = new java.util.LinkedHashMap<>();
            airParams.put("subscriberNumber", subscriberNumber);
            if (eocnId != null) airParams.put("ussdEndOfCallNotificationID", eocnId);

            String xml = xmlBuilder.buildRequest("UpdateAccountDetails", airParams);
            String rawXml = airTransport.send(xml, "UpdateAccountDetails");
            return xmlParser.parseResponse(rawXml, transactionId);
        } catch (AirTransportException e) {
            return AirResponse.networkError(e.getMessage());
        } catch (Exception e) {
            return AirResponse.networkError(e.getMessage());
        }
    }
}
