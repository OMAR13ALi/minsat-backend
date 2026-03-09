package com.minsat.air.xml;

import com.minsat.air.model.AirResponse;
import com.minsat.air.util.DateUtils;
import com.minsat.air.util.ErrorCodes;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;

@Component
public class XmlParser {

    private final DocumentBuilderFactory factory;

    public XmlParser() {
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
    }

    /**
     * Parses a <methodResponse> XML string into an AirResponse.
     *
     * @param xml           Raw XML response string
     * @param transactionId The originTransactionID sent in the request
     * @return AirResponse record
     */
    public AirResponse parseResponse(String xml, String transactionId) {
        if (xml == null || xml.isBlank()) {
            return AirResponse.parseError("Empty response from server");
        }

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            // Check for fault
            NodeList faultList = root.getElementsByTagName("fault");
            if (faultList.getLength() > 0) {
                Map<String, Object> faultData = parseStruct((Element) findFirstByTag(faultList.item(0), "struct"));
                int faultCode = toInt(faultData.get("faultCode"), -1);
                String faultString = String.valueOf(faultData.getOrDefault("faultString", "Unknown fault"));
                return new AirResponse(false, faultCode, faultString, transactionId, null, faultData);
            }

            // Parse params
            NodeList paramsList = root.getElementsByTagName("params");
            if (paramsList.getLength() == 0) {
                return AirResponse.parseError("No params or fault in response");
            }

            Node structNode = findFirstByTag(paramsList.item(0), "struct");
            if (structNode == null) {
                return AirResponse.parseError("No struct in response params");
            }

            Map<String, Object> raw = parseStruct((Element) structNode);

            int responseCode = toInt(raw.get("responseCode"), 0);
            String responseMessage = responseCode == 0
                ? ErrorCodes.message(0)
                : String.valueOf(raw.getOrDefault("responseMessage", ErrorCodes.message(responseCode)));

            boolean success = responseCode == 0;

            return new AirResponse(success, responseCode, responseMessage, transactionId, raw, raw);

        } catch (Exception e) {
            return AirResponse.parseError(e.getMessage());
        }
    }

    /**
     * Recursively parses a <struct> element into a Map<String, Object>.
     */
    public Map<String, Object> parseStruct(Element struct) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (struct == null) return result;

        NodeList members = struct.getChildNodes();
        for (int i = 0; i < members.getLength(); i++) {
            Node node = members.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            if (!"member".equals(node.getNodeName())) continue;

            Element member = (Element) node;
            String name = getMemberName(member);
            Node valueNode = findFirstByTag(member, "value");
            if (name != null && valueNode != null) {
                result.put(name, parseValue((Element) valueNode));
            }
        }
        return result;
    }

    /**
     * Gets the member name — handles both <name> and <n> tags.
     */
    private String getMemberName(Element member) {
        NodeList nameNodes = member.getElementsByTagName("name");
        if (nameNodes.getLength() > 0) {
            return nameNodes.item(0).getTextContent().trim();
        }
        NodeList nNodes = member.getElementsByTagName("n");
        if (nNodes.getLength() > 0) {
            return nNodes.item(0).getTextContent().trim();
        }
        return null;
    }

    /**
     * Parses a <value> element into the appropriate Java type.
     */
    private Object parseValue(Element valueEl) {
        // Get the first child element (the type tag)
        Element typeEl = getFirstChildElement(valueEl);
        if (typeEl == null) {
            // Plain text value = string
            return valueEl.getTextContent().trim();
        }

        String tag = typeEl.getNodeName();
        String text = typeEl.getTextContent().trim();

        return switch (tag) {
            case "string" -> text;
            case "int", "i4", "i8" -> {
                try { yield Integer.parseInt(text); }
                catch (NumberFormatException e) {
                    try { yield Long.parseLong(text); }
                    catch (NumberFormatException e2) { yield text; }
                }
            }
            case "double" -> {
                try { yield Double.parseDouble(text); }
                catch (NumberFormatException e) { yield text; }
            }
            case "boolean" -> !"0".equals(text);
            case "dateTime.iso8601" -> {
                var instant = DateUtils.fromAirDate(text);
                yield instant != null ? instant.toString() : text;
            }
            case "nil" -> null;
            case "struct" -> parseStruct(typeEl);
            case "array" -> parseArray(typeEl);
            default -> text;
        };
    }

    private List<Object> parseArray(Element arrayEl) {
        List<Object> result = new ArrayList<>();
        NodeList dataList = arrayEl.getElementsByTagName("data");
        if (dataList.getLength() == 0) return result;

        Element data = (Element) dataList.item(0);
        NodeList children = data.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "value".equals(node.getNodeName())) {
                result.add(parseValue((Element) node));
            }
        }
        return result;
    }

    private Node findFirstByTag(Node parent, String tagName) {
        if (parent == null) return null;
        NodeList list = ((Element) parent).getElementsByTagName(tagName);
        return list.getLength() > 0 ? list.item(0) : null;
    }

    private Element getFirstChildElement(Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return (Element) children.item(i);
            }
        }
        return null;
    }

    private int toInt(Object value, int defaultValue) {
        if (value instanceof Integer i) return i;
        if (value instanceof Long l) return l.intValue();
        if (value instanceof String s) {
            try { return Integer.parseInt(s.trim()); }
            catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }
}
