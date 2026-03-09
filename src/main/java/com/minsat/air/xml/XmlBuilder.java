package com.minsat.air.xml;

import com.minsat.air.util.DateUtils;
import com.minsat.config.AirConfig;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class XmlBuilder {

    private final AirConfig config;

    public XmlBuilder(AirConfig config) {
        this.config = config;
    }

    /**
     * Builds an XML-RPC methodCall string.
     * Auto-injects: originNodeType, originHostName, originTransactionID, originTimeStamp.
     * subscriberNumber is taken from params if present and injected in canonical position.
     *
     * @param methodName AIR method name (e.g. "GetBalanceAndDate")
     * @param params     Method-specific parameters (subscriberNumber must be included)
     * @return Complete XML-RPC methodCall string
     */
    public String buildRequest(String methodName, Map<String, Object> params) {
        // Build ordered params with mandatory fields first
        Map<String, Object> allParams = new LinkedHashMap<>();
        allParams.put("originNodeType", config.getNodeType());
        allParams.put("originHostName", config.getHostName());
        allParams.put("originTransactionID", UUID.randomUUID().toString());
        // Use caller-supplied timestamp if provided, otherwise generate current UTC time
        String originTimeStamp = params.containsKey("originTimeStamp")
            ? (String) params.get("originTimeStamp")
            : DateUtils.toAirDateNow();
        allParams.put("originTimeStamp", originTimeStamp);

        // subscriberNumber must come from caller params
        if (params.containsKey("subscriberNumber")) {
            allParams.put("subscriberNumber", params.get("subscriberNumber"));
        }

        // Add remaining method-specific params (skip mandatory fields already added)
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("subscriberNumber") && !key.equals("originTimeStamp") && entry.getValue() != null) {
                allParams.put(key, entry.getValue());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n");
        sb.append("<methodCall>\n");
        sb.append("  <methodName>").append(escapeXml(methodName)).append("</methodName>\n");
        sb.append("  <params>\n");
        sb.append("    <param>\n");
        sb.append("      <value>\n");
        sb.append("        <struct>\n");
        buildStruct(sb, allParams, "          ");
        sb.append("        </struct>\n");
        sb.append("      </value>\n");
        sb.append("    </param>\n");
        sb.append("  </params>\n");
        sb.append("</methodCall>");

        return sb.toString();
    }

    private void buildStruct(StringBuilder sb, Map<String, Object> map, String indent) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value == null) continue;
            sb.append(indent).append("<member>");
            sb.append("<name>").append(escapeXml(entry.getKey())).append("</name>");
            sb.append("<value>");
            appendValue(sb, value, indent);
            sb.append("</value>");
            sb.append("</member>\n");
        }
    }

    @SuppressWarnings("unchecked")
    private void appendValue(StringBuilder sb, Object value, String indent) {
        if (value instanceof String s) {
            // Detect AIR date format: starts with 8 digits then T then 6 digits
            if (s.matches("^\\d{8}T\\d{6}.*")) {
                sb.append("<dateTime.iso8601>").append(escapeXml(s)).append("</dateTime.iso8601>");
            } else {
                sb.append("<string>").append(escapeXml(s)).append("</string>");
            }
        } else if (value instanceof Boolean b) {
            sb.append("<boolean>").append(b ? "1" : "0").append("</boolean>");
        } else if (value instanceof Integer || value instanceof Long) {
            sb.append("<int>").append(value).append("</int>");
        } else if (value instanceof Double || value instanceof Float) {
            sb.append("<double>").append(value).append("</double>");
        } else if (value instanceof Instant instant) {
            sb.append("<dateTime.iso8601>").append(DateUtils.toAirDate(instant)).append("</dateTime.iso8601>");
        } else if (value instanceof List<?> list) {
            sb.append("<array><data>");
            for (Object item : list) {
                sb.append("<value>");
                if (item instanceof Map) {
                    sb.append("<struct>");
                    buildStruct(sb, (Map<String, Object>) item, indent + "  ");
                    sb.append("</struct>");
                } else {
                    appendValue(sb, item, indent + "  ");
                }
                sb.append("</value>");
            }
            sb.append("</data></array>");
        } else if (value instanceof Map<?, ?> map) {
            sb.append("<struct>");
            buildStruct(sb, (Map<String, Object>) map, indent + "  ");
            sb.append("</struct>");
        } else {
            // Fallback: treat as string
            sb.append("<string>").append(escapeXml(value.toString())).append("</string>");
        }
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
