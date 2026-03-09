package com.minsat.air.ucip;

import com.minsat.air.model.AirResponse;
import com.minsat.air.transport.AirTransport;
import com.minsat.air.transport.AirTransportException;
import com.minsat.air.xml.XmlBuilder;
import com.minsat.air.xml.XmlParser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UpdateServiceClass {

    private static final List<String> VALID_ACTIONS = List.of("SetOriginal", "SetTemporary", "DeleteTemporary");

    private final XmlBuilder xmlBuilder;
    private final AirTransport airTransport;
    private final XmlParser xmlParser;

    public UpdateServiceClass(XmlBuilder xmlBuilder, AirTransport airTransport, XmlParser xmlParser) {
        this.xmlBuilder = xmlBuilder;
        this.airTransport = airTransport;
        this.xmlParser = xmlParser;
    }

    public AirResponse execute(Map<String, Object> params) {
        String subscriberNumber = (String) params.get("subscriberNumber");
        if (subscriberNumber == null || subscriberNumber.isBlank()) {
            throw new IllegalArgumentException("subscriberNumber is required");
        }

        String action = (String) params.get("action");
        if (action == null || !VALID_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("action must be one of: " + VALID_ACTIONS);
        }

        Object serviceClassNew = params.get("serviceClassNew");
        if (!"DeleteTemporary".equals(action) && serviceClassNew == null) {
            throw new IllegalArgumentException("serviceClassNew is required for action: " + action);
        }

        String transactionId = UUID.randomUUID().toString();
        try {
            java.util.Map<String, Object> airParams = new java.util.LinkedHashMap<>();
            airParams.put("subscriberNumber", subscriberNumber);
            airParams.put("serviceClassAction", action);
            if (serviceClassNew != null) airParams.put("serviceClassNew", serviceClassNew);

            String xml = xmlBuilder.buildRequest("UpdateServiceClass", airParams);
            String rawXml = airTransport.send(xml, "UpdateServiceClass");
            return xmlParser.parseResponse(rawXml, transactionId);
        } catch (AirTransportException e) {
            return AirResponse.networkError(e.getMessage());
        } catch (Exception e) {
            return AirResponse.networkError(e.getMessage());
        }
    }
}
