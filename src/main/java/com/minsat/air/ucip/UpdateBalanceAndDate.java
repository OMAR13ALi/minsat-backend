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
public class UpdateBalanceAndDate {

    private final XmlBuilder xmlBuilder;
    private final AirTransport airTransport;
    private final XmlParser xmlParser;

    public UpdateBalanceAndDate(XmlBuilder xmlBuilder, AirTransport airTransport, XmlParser xmlParser) {
        this.xmlBuilder = xmlBuilder;
        this.airTransport = airTransport;
        this.xmlParser = xmlParser;
    }

    public AirResponse execute(Map<String, Object> params) {
        String subscriberNumber = (String) params.get("subscriberNumber");
        if (subscriberNumber == null || subscriberNumber.isBlank()) {
            throw new IllegalArgumentException("subscriberNumber is required");
        }

        // Validate: currency required if adjustmentAmount or dedicatedAccounts present
        String adjustmentAmount = (String) params.get("adjustmentAmount");
        Object dedicatedAccounts = params.get("dedicatedAccounts");
        String currency = (String) params.get("currency");

        if (adjustmentAmount != null && (currency == null || currency.isBlank())) {
            throw new IllegalArgumentException("currency is required when adjustmentAmount is provided");
        }
        if (dedicatedAccounts != null && (currency == null || currency.isBlank())) {
            throw new IllegalArgumentException("currency is required when dedicatedAccounts is provided");
        }

        String transactionId = UUID.randomUUID().toString();
        try {
            // Build the AIR-specific params map
            java.util.Map<String, Object> airParams = new java.util.LinkedHashMap<>();
            airParams.put("subscriberNumber", subscriberNumber);
            putIfNotNull(airParams, "transactionCurrency", currency);
            putIfNotNull(airParams, "adjustmentAmountRelative", adjustmentAmount);
            putIfNotNull(airParams, "supervisionExpiryDate", params.get("supervisionExpiryDate"));
            putIfNotNull(airParams, "serviceFeeExpiryDate", params.get("serviceFeeExpiryDate"));
            putIfNotNull(airParams, "creditClearancePeriod", params.get("creditClearancePeriod"));
            putIfNotNull(airParams, "serviceRemovalPeriod", params.get("serviceRemovalPeriod"));

            if (dedicatedAccounts instanceof List<?> daList && !daList.isEmpty()) {
                List<Map<String, Object>> mapped = daList.stream()
                    .filter(e -> e instanceof Map)
                    .map(e -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> da = (Map<String, Object>) e;
                        java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
                        putIfNotNull(entry, "dedicatedAccountID", da.get("accountId"));
                        putIfNotNull(entry, "adjustmentAmountRelative", da.get("relativeAmount"));
                        putIfNotNull(entry, "dedicatedAccountValueNew", da.get("absoluteValue"));
                        return entry;
                    })
                    .toList();
                airParams.put("dedicatedAccountUpdateInformation", mapped);
            }

            String xml = xmlBuilder.buildRequest("UpdateBalanceAndDate", airParams);
            String rawXml = airTransport.send(xml, "UpdateBalanceAndDate");
            return xmlParser.parseResponse(rawXml, transactionId);
        } catch (AirTransportException e) {
            return AirResponse.networkError(e.getMessage());
        } catch (Exception e) {
            return AirResponse.networkError(e.getMessage());
        }
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }
}
