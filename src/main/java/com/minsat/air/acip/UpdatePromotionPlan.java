package com.minsat.air.acip;

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
public class UpdatePromotionPlan {

    private static final List<String> VALID_ACTIONS = List.of("ADD", "SET", "DELETE");

    private final XmlBuilder xmlBuilder;
    private final AirTransport airTransport;
    private final XmlParser xmlParser;

    public UpdatePromotionPlan(XmlBuilder xmlBuilder, AirTransport airTransport, XmlParser xmlParser) {
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

        // Action-specific validation
        switch (action) {
            case "ADD" -> {
                requireParam(params, "planId", "UpdatePromotionPlan ADD");
                requireParam(params, "startDate", "UpdatePromotionPlan ADD");
                requireParam(params, "endDate", "UpdatePromotionPlan ADD");
            }
            case "SET" -> {
                requireParam(params, "oldStartDate", "UpdatePromotionPlan SET");
                requireParam(params, "oldEndDate", "UpdatePromotionPlan SET");
                requireParam(params, "startDate", "UpdatePromotionPlan SET");
                requireParam(params, "endDate", "UpdatePromotionPlan SET");
            }
            case "DELETE" -> {
                requireParam(params, "oldStartDate", "UpdatePromotionPlan DELETE");
                requireParam(params, "oldEndDate", "UpdatePromotionPlan DELETE");
            }
        }

        String transactionId = UUID.randomUUID().toString();
        try {
            java.util.Map<String, Object> airParams = new java.util.LinkedHashMap<>();
            airParams.put("subscriberNumber", subscriberNumber);
            airParams.put("promotionPlanAction", action);

            putIfNotNull(airParams, "promotionPlanID", params.get("planId"));
            putIfNotNull(airParams, "promotionOldStartDate", params.get("oldStartDate"));
            putIfNotNull(airParams, "promotionOldEndDate", params.get("oldEndDate"));
            putIfNotNull(airParams, "promotionStartDate", params.get("startDate"));
            putIfNotNull(airParams, "promotionEndDate", params.get("endDate"));

            String xml = xmlBuilder.buildRequest("UpdatePromotionPlan", airParams);
            String rawXml = airTransport.send(xml, "UpdatePromotionPlan");
            return xmlParser.parseResponse(rawXml, transactionId);
        } catch (AirTransportException e) {
            return AirResponse.networkError(e.getMessage());
        } catch (Exception e) {
            return AirResponse.networkError(e.getMessage());
        }
    }

    private void requireParam(Map<String, Object> params, String key, String context) {
        if (params.get(key) == null) {
            throw new IllegalArgumentException(context + ": " + key + " is required");
        }
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }
}
