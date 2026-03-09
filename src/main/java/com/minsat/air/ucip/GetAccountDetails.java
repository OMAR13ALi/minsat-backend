package com.minsat.air.ucip;

import com.minsat.air.model.AirResponse;
import com.minsat.air.transport.AirTransport;
import com.minsat.air.transport.AirTransportException;
import com.minsat.air.xml.XmlBuilder;
import com.minsat.air.xml.XmlParser;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class GetAccountDetails {

    private final XmlBuilder xmlBuilder;
    private final AirTransport airTransport;
    private final XmlParser xmlParser;

    public GetAccountDetails(XmlBuilder xmlBuilder, AirTransport airTransport, XmlParser xmlParser) {
        this.xmlBuilder = xmlBuilder;
        this.airTransport = airTransport;
        this.xmlParser = xmlParser;
    }

    public AirResponse execute(Map<String, Object> params) {
        String subscriberNumber = (String) params.get("subscriberNumber");
        if (subscriberNumber == null || subscriberNumber.isBlank()) {
            throw new IllegalArgumentException("subscriberNumber is required");
        }

        String transactionId = UUID.randomUUID().toString();
        try {
            Map<String, Object> allParams = new HashMap<>(params);
            Boolean includeLocation = (Boolean) params.get("includeLocation");
            if (Boolean.TRUE.equals(includeLocation)) {
                allParams.put("requestedInformationFlags",
                    Map.of("requestLocationInformationFlag", true));
            }
            allParams.remove("includeLocation");

            String xml = xmlBuilder.buildRequest("GetAccountDetails", allParams);
            String rawXml = airTransport.send(xml, "GetAccountDetails");
            return xmlParser.parseResponse(rawXml, transactionId);
        } catch (AirTransportException e) {
            return AirResponse.networkError(e.getMessage());
        } catch (Exception e) {
            return AirResponse.networkError(e.getMessage());
        }
    }
}
