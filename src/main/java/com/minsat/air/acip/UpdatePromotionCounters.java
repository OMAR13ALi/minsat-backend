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
public class UpdatePromotionCounters {

    private final XmlBuilder xmlBuilder;
    private final AirTransport airTransport;
    private final XmlParser xmlParser;

    public UpdatePromotionCounters(XmlBuilder xmlBuilder, AirTransport airTransport, XmlParser xmlParser) {
        this.xmlBuilder = xmlBuilder;
        this.airTransport = airTransport;
        this.xmlParser = xmlParser;
    }

    public AirResponse execute(Map<String, Object> params) {
        String subscriberNumber = (String) params.get("subscriberNumber");
        if (subscriberNumber == null || subscriberNumber.isBlank()) {
            throw new IllegalArgumentException("subscriberNumber is required");
        }

        String promotionRefillAmountRelative = (String) params.get("promotionRefillAmountRelative");
        String transactionCurrency = (String) params.get("transactionCurrency");

        if (promotionRefillAmountRelative != null && (transactionCurrency == null || transactionCurrency.isBlank())) {
            throw new IllegalArgumentException("transactionCurrency is required when promotionRefillAmountRelative is provided");
        }

        String transactionId = UUID.randomUUID().toString();
        try {
            java.util.Map<String, Object> airParams = new java.util.LinkedHashMap<>();
            airParams.put("subscriberNumber", subscriberNumber);
            if (transactionCurrency != null) airParams.put("transactionCurrency", transactionCurrency);
            if (promotionRefillAmountRelative != null)
                airParams.put("promotionRefillAmountRelative", promotionRefillAmountRelative);

            String xml = xmlBuilder.buildRequest("UpdatePromotionCounters", airParams);
            String rawXml = airTransport.send(xml, "UpdatePromotionCounters");
            return xmlParser.parseResponse(rawXml, transactionId);
        } catch (AirTransportException e) {
            return AirResponse.networkError(e.getMessage());
        } catch (Exception e) {
            return AirResponse.networkError(e.getMessage());
        }
    }
}
