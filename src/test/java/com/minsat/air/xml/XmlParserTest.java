package com.minsat.air.xml;

import com.minsat.air.model.AirResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XmlParserTest {

    private XmlParser xmlParser;

    @BeforeEach
    void setUp() {
        xmlParser = new XmlParser();
    }

    @Test
    void parsesSuccessResponse() {
        String xml = """
            <?xml version="1.0"?>
            <methodResponse>
              <params>
                <param>
                  <value>
                    <struct>
                      <member>
                        <name>responseCode</name>
                        <value><int>0</int></value>
                      </member>
                      <member>
                        <name>accountValue</name>
                        <value><string>50000</string></value>
                      </member>
                    </struct>
                  </value>
                </param>
              </params>
            </methodResponse>
            """;

        AirResponse response = xmlParser.parseResponse(xml, "txn-123");

        assertThat(response.success()).isTrue();
        assertThat(response.responseCode()).isEqualTo(0);
        assertThat(response.transactionId()).isEqualTo("txn-123");
        assertThat(response.data()).containsKey("accountValue");
        assertThat(response.data().get("accountValue")).isEqualTo("50000");
    }

    @Test
    void parsesNonZeroResponseCode() {
        String xml = """
            <?xml version="1.0"?>
            <methodResponse>
              <params>
                <param>
                  <value>
                    <struct>
                      <member>
                        <name>responseCode</name>
                        <value><int>102</int></value>
                      </member>
                    </struct>
                  </value>
                </param>
              </params>
            </methodResponse>
            """;

        AirResponse response = xmlParser.parseResponse(xml, "txn-456");

        assertThat(response.success()).isFalse();
        assertThat(response.responseCode()).isEqualTo(102);
    }

    @Test
    void handlesBothNameAndNTags() {
        String xml = """
            <?xml version="1.0"?>
            <methodResponse>
              <params>
                <param>
                  <value>
                    <struct>
                      <member>
                        <n>responseCode</n>
                        <value><int>0</int></value>
                      </member>
                      <member>
                        <n>accountValue</n>
                        <value><string>25000</string></value>
                      </member>
                    </struct>
                  </value>
                </param>
              </params>
            </methodResponse>
            """;

        AirResponse response = xmlParser.parseResponse(xml, "txn-789");

        assertThat(response.success()).isTrue();
        assertThat(response.data().get("accountValue")).isEqualTo("25000");
    }

    @Test
    void parsesBoolean0AsFalse() {
        String xml = """
            <?xml version="1.0"?>
            <methodResponse>
              <params>
                <param>
                  <value>
                    <struct>
                      <member>
                        <name>responseCode</name>
                        <value><int>0</int></value>
                      </member>
                      <member>
                        <name>temporaryBlockedFlag</name>
                        <value><boolean>0</boolean></value>
                      </member>
                    </struct>
                  </value>
                </param>
              </params>
            </methodResponse>
            """;

        AirResponse response = xmlParser.parseResponse(xml, "txn");

        assertThat(response.data().get("temporaryBlockedFlag")).isEqualTo(false);
    }

    @Test
    void parsesBoolean1AsTrue() {
        String xml = """
            <?xml version="1.0"?>
            <methodResponse>
              <params>
                <param>
                  <value>
                    <struct>
                      <member>
                        <name>responseCode</name>
                        <value><int>0</int></value>
                      </member>
                      <member>
                        <name>temporaryBlockedFlag</name>
                        <value><boolean>1</boolean></value>
                      </member>
                    </struct>
                  </value>
                </param>
              </params>
            </methodResponse>
            """;

        AirResponse response = xmlParser.parseResponse(xml, "txn");

        assertThat(response.data().get("temporaryBlockedFlag")).isEqualTo(true);
    }

    @Test
    void handlesFaultResponse() {
        String xml = """
            <?xml version="1.0"?>
            <methodResponse>
              <fault>
                <value>
                  <struct>
                    <member>
                      <name>faultCode</name>
                      <value><int>400</int></value>
                    </member>
                    <member>
                      <name>faultString</name>
                      <value><string>Bad Request</string></value>
                    </member>
                  </struct>
                </value>
              </fault>
            </methodResponse>
            """;

        AirResponse response = xmlParser.parseResponse(xml, "txn");

        assertThat(response.success()).isFalse();
        assertThat(response.responseCode()).isEqualTo(400);
        assertThat(response.responseMessage()).isEqualTo("Bad Request");
    }

    @Test
    void handlesEmptyResponse() {
        AirResponse response = xmlParser.parseResponse("", "txn");

        assertThat(response.success()).isFalse();
        assertThat(response.responseCode()).isEqualTo(-2);
    }

    @Test
    void handlesMalformedXml() {
        AirResponse response = xmlParser.parseResponse("not xml at all", "txn");

        assertThat(response.success()).isFalse();
        assertThat(response.responseCode()).isEqualTo(-2);
    }
}
