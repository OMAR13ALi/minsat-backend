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
public class UpdateFaFList {

    private static final List<String> VALID_ACTIONS = List.of("ADD", "SET", "DELETE");

    private final XmlBuilder xmlBuilder;
    private final AirTransport airTransport;
    private final XmlParser xmlParser;

    public UpdateFaFList(XmlBuilder xmlBuilder, AirTransport airTransport, XmlParser xmlParser) {
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

        Object entriesObj = params.get("entries");
        if (!(entriesObj instanceof List)) {
            throw new IllegalArgumentException("entries must be an array");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) entriesObj;

        if (!"SET".equals(action) && entries.isEmpty()) {
            throw new IllegalArgumentException("entries cannot be empty for action: " + action);
        }

        String transactionId = UUID.randomUUID().toString();
        try {
            java.util.Map<String, Object> airParams = new java.util.LinkedHashMap<>();
            airParams.put("subscriberNumber", subscriberNumber);
            airParams.put("fafAction", action);

            List<Map<String, Object>> fafInfo = entries.stream().map(e -> {
                java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
                entry.put("fafNumber", e.get("fafNumber"));
                entry.put("owner", e.getOrDefault("owner", "Subscriber"));
                return entry;
            }).toList();
            airParams.put("fafInformation", fafInfo);

            String xml = xmlBuilder.buildRequest("UpdateFaFList", airParams);
            String rawXml = airTransport.send(xml, "UpdateFaFList");
            return xmlParser.parseResponse(rawXml, transactionId);
        } catch (AirTransportException e) {
            return AirResponse.networkError(e.getMessage());
        } catch (Exception e) {
            return AirResponse.networkError(e.getMessage());
        }
    }
}
