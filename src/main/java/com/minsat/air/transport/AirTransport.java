package com.minsat.air.transport;

import com.minsat.config.AirConfig;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
public class AirTransport {

    private final AirConfig config;

    public AirTransport(AirConfig config) {
        this.config = config;
    }

    /**
     * Sends an XML-RPC request over raw TCP and returns the XML response body.
     *
     * @param xmlBody    The complete XML methodCall body
     * @param methodName Used for debug logging
     * @return Raw XML response (methodResponse body only, HTTP headers stripped)
     * @throws AirTransportException on timeout or network error
     */
    public String send(String xmlBody, String methodName) {
        long startMs = System.currentTimeMillis();
        debug("→ " + methodName, xmlBody);

        byte[] bodyBytes = xmlBody.getBytes(StandardCharsets.UTF_8);
        String credentials = Base64.getEncoder().encodeToString(
            (config.getUser() + ":" + config.getPassword()).getBytes(StandardCharsets.UTF_8)
        );

        String httpRequest = "POST /Air HTTP/2.0\r\n"
            + "Accept: text/xml\r\n"
            + "Connection: keep-alive\r\n"
            + "Content-Length: " + bodyBytes.length + "\r\n"
            + "Content-Type: text/xml\r\n"
            + "Date: " + Instant.now().toString() + "\r\n"
            + "Host: " + config.getHost() + "\r\n"
            + "User-Agent: UGw Server/4.3/1.0\r\n"
            + "Authorization: Basic " + credentials + "\r\n"
            + "\r\n"
            + xmlBody;

        byte[] requestBytes = httpRequest.getBytes(StandardCharsets.UTF_8);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(config.getHost(), config.getPort()), config.getTimeoutMs());
            socket.setSoTimeout(config.getTimeoutMs());

            OutputStream out = socket.getOutputStream();
            out.write(requestBytes);
            out.flush();

            String response = readUntilMethodResponse(socket.getInputStream());
            long elapsed = System.currentTimeMillis() - startMs;
            debug("← " + methodName + " (" + elapsed + "ms)", response);
            return response;

        } catch (SocketTimeoutException e) {
            String msg = "Timeout after " + config.getTimeoutMs() + "ms for " + methodName;
            debugError(methodName, msg);
            throw new AirTransportException(msg, e);
        } catch (IOException e) {
            String msg = "Connection error to " + config.getHost() + ":" + config.getPort() + " — " + e.getMessage();
            debugError(methodName, msg);
            throw new AirTransportException(msg, e);
        }
    }

    private String readUntilMethodResponse(InputStream in) throws IOException {
        StringBuilder accumulator = new StringBuilder(4096);
        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = in.read(buffer)) != -1) {
            accumulator.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            if (accumulator.indexOf("</methodResponse>") != -1) {
                break;
            }
        }

        String full = accumulator.toString();

        // Strip HTTP headers — find the double CRLF separator
        int headerEnd = full.indexOf("\r\n\r\n");
        if (headerEnd != -1) {
            return full.substring(headerEnd + 4).trim();
        }
        // Fallback: return everything (may already be just XML)
        return full.trim();
    }

    private void debug(String label, String data) {
        if (config.isDebug()) {
            System.out.println("[AIR " + Instant.now() + "] " + label);
            if (data != null && !data.isBlank()) {
                System.out.println(data);
            }
        }
    }

    private void debugError(String methodName, String error) {
        if (config.isDebug()) {
            System.out.println("[AIR " + Instant.now() + "] ✗ " + methodName + " error=" + error);
        }
    }
}
