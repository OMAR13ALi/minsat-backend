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
public class UpdateCommunityList {

    private final XmlBuilder xmlBuilder;
    private final AirTransport airTransport;
    private final XmlParser xmlParser;

    public UpdateCommunityList(XmlBuilder xmlBuilder, AirTransport airTransport, XmlParser xmlParser) {
        this.xmlBuilder = xmlBuilder;
        this.airTransport = airTransport;
        this.xmlParser = xmlParser;
    }

    public AirResponse execute(Map<String, Object> params) {
        String subscriberNumber = (String) params.get("subscriberNumber");
        if (subscriberNumber == null || subscriberNumber.isBlank()) {
            throw new IllegalArgumentException("subscriberNumber is required");
        }

        Object communityIdsObj = params.get("communityIds");
        if (!(communityIdsObj instanceof List)) {
            throw new IllegalArgumentException("communityIds must be an array");
        }

        @SuppressWarnings("unchecked")
        List<Object> communityIds = (List<Object>) communityIdsObj;

        String transactionId = UUID.randomUUID().toString();
        try {
            java.util.Map<String, Object> airParams = new java.util.LinkedHashMap<>();
            airParams.put("subscriberNumber", subscriberNumber);

            List<Map<String, Object>> communityInfo = communityIds.stream()
                .map(id -> (Map<String, Object>) Map.of("communityID", id))
                .toList();
            airParams.put("communityInformationNew", communityInfo);

            String xml = xmlBuilder.buildRequest("UpdateCommunityList", airParams);
            String rawXml = airTransport.send(xml, "UpdateCommunityList");
            return xmlParser.parseResponse(rawXml, transactionId);
        } catch (AirTransportException e) {
            return AirResponse.networkError(e.getMessage());
        } catch (Exception e) {
            return AirResponse.networkError(e.getMessage());
        }
    }
}
