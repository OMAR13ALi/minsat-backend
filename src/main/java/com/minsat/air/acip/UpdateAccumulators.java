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
public class UpdateAccumulators {

    private final XmlBuilder xmlBuilder;
    private final AirTransport airTransport;
    private final XmlParser xmlParser;

    public UpdateAccumulators(XmlBuilder xmlBuilder, AirTransport airTransport, XmlParser xmlParser) {
        this.xmlBuilder = xmlBuilder;
        this.airTransport = airTransport;
        this.xmlParser = xmlParser;
    }

    public AirResponse execute(Map<String, Object> params) {
        String subscriberNumber = (String) params.get("subscriberNumber");
        if (subscriberNumber == null || subscriberNumber.isBlank()) {
            throw new IllegalArgumentException("subscriberNumber is required");
        }

        Object accumulatorsObj = params.get("accumulators");
        if (!(accumulatorsObj instanceof List) || ((List<?>) accumulatorsObj).isEmpty()) {
            throw new IllegalArgumentException("accumulators must be a non-empty array");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> accumulators = (List<Map<String, Object>>) accumulatorsObj;

        for (Map<String, Object> acc : accumulators) {
            if (acc.get("accumulatorId") == null) {
                throw new IllegalArgumentException("Each accumulator entry must have accumulatorId");
            }
            boolean hasRelative = acc.get("relativeValue") != null;
            boolean hasAbsolute = acc.get("absoluteValue") != null;
            if (!hasRelative && !hasAbsolute) {
                throw new IllegalArgumentException("Each accumulator entry must have relativeValue or absoluteValue");
            }
        }

        String transactionId = UUID.randomUUID().toString();
        try {
            java.util.Map<String, Object> airParams = new java.util.LinkedHashMap<>();
            airParams.put("subscriberNumber", subscriberNumber);

            Object originOperatorId = params.get("originOperatorID");
            if (originOperatorId != null) airParams.put("originOperatorID", originOperatorId);

            Object serviceClassCurrent = params.get("serviceClassCurrent");
            if (serviceClassCurrent != null) airParams.put("serviceClassCurrent", serviceClassCurrent);

            List<Map<String, Object>> accInfo = accumulators.stream().map(a -> {
                java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
                entry.put("accumulatorID", a.get("accumulatorId"));
                if (a.get("relativeValue") != null) entry.put("accumulatorValueRelative", a.get("relativeValue"));
                if (a.get("absoluteValue") != null) entry.put("accumulatorValueAbsolute", a.get("absoluteValue"));
                if (a.get("startDate") != null) entry.put("accumulatorStartDate", a.get("startDate"));
                return entry;
            }).toList();
            airParams.put("accumulatorInformation", accInfo);

            String xml = xmlBuilder.buildRequest("UpdateAccumulators", airParams);
            String rawXml = airTransport.send(xml, "UpdateAccumulators");
            return xmlParser.parseResponse(rawXml, transactionId);
        } catch (AirTransportException e) {
            return AirResponse.networkError(e.getMessage());
        } catch (Exception e) {
            return AirResponse.networkError(e.getMessage());
        }
    }
}
