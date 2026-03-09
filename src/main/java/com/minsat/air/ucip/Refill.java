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
public class Refill {

    private final XmlBuilder xmlBuilder;
    private final AirTransport airTransport;
    private final XmlParser xmlParser;

    public Refill(XmlBuilder xmlBuilder, AirTransport airTransport, XmlParser xmlParser) {
        this.xmlBuilder = xmlBuilder;
        this.airTransport = airTransport;
        this.xmlParser = xmlParser;
    }

    public AirResponse execute(Map<String, Object> params) {
        String subscriberNumber = (String) params.get("subscriberNumber");
        if (subscriberNumber == null || subscriberNumber.isBlank()) {
            throw new IllegalArgumentException("subscriberNumber is required");
        }

        String voucherCode = (String) params.get("voucherCode");
        String amount = (String) params.get("amount");
        String currency = (String) params.get("currency");
        String profileId = (String) params.get("profileId");

        boolean hasVoucher = voucherCode != null && !voucherCode.isBlank();
        boolean hasVoucherless = amount != null || currency != null || profileId != null;

        if (hasVoucher && hasVoucherless) {
            throw new IllegalArgumentException(
                "refill: provide either (amount+currency+profileId) or voucherCode, not both");
        }
        if (!hasVoucher && !hasVoucherless) {
            throw new IllegalArgumentException(
                "refill: provide either (amount+currency+profileId) or voucherCode");
        }

        String transactionId = UUID.randomUUID().toString();
        try {
            java.util.Map<String, Object> airParams = new java.util.LinkedHashMap<>();
            airParams.put("subscriberNumber", subscriberNumber);

            Object transactionType = params.get("transactionType");
            Object transactionCode = params.get("transactionCode");
            if (transactionType != null) airParams.put("transactionType", transactionType);
            if (transactionCode != null) airParams.put("transactionCode", transactionCode);

            if (hasVoucher) {
                airParams.put("voucherActivationCode", voucherCode);
            } else {
                airParams.put("transactionAmount", amount);
                airParams.put("transactionCurrency", currency);
                airParams.put("refillProfileID", profileId);
            }

            String xml = xmlBuilder.buildRequest("Refill", airParams);
            String rawXml = airTransport.send(xml, "Refill");
            return xmlParser.parseResponse(rawXml, transactionId);
        } catch (AirTransportException e) {
            return AirResponse.networkError(e.getMessage());
        } catch (Exception e) {
            return AirResponse.networkError(e.getMessage());
        }
    }
}
