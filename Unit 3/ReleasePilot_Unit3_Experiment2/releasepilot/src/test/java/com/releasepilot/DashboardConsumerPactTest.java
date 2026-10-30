package com.releasepilot;
import au.com.dius.pact.consumer.MockServer;import au.com.dius.pact.consumer.dsl.*;
import au.com.dius.pact.consumer.junit5.*;import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;import au.com.dius.pact.core.model.PactSpecVersion;import org.junit.jupiter.api.Test;import org.junit.jupiter.api.extension.ExtendWith;
import java.net.URI;import java.net.http.*;import com.fasterxml.jackson.databind.ObjectMapper;import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(PactConsumerTestExt.class) @PactTestFor(providerName="quote-api",pactVersion=PactSpecVersion.V3)
class DashboardConsumerPactTest {
 @Pact(consumer="release-dashboard") public RequestResponsePact quote(PactDslWithProvider builder){return builder.uponReceiving("the dashboard requests a two-item quote").path("/api/quote").query("quantity=2").method("GET").willRespondWith().status(200).headers(java.util.Map.of("Content-Type","application/json")).body(new PactDslJsonBody().stringValue("sku","LAB-KIT").numberValue("quantity",2).numberValue("unitPrice",499).numberValue("total",998).stringValue("currency","INR")).toPact();}
 @Test @PactTestFor(pactMethod="quote",pactVersion=PactSpecVersion.V3) void dashboardCanReadQuote(MockServer mock)throws Exception{var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(mock.getUrl()+"/api/quote?quantity=2")).GET().build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,response.statusCode());var body=new ObjectMapper().readTree(response.body());assertEquals(998,body.get("total").asInt());assertEquals("INR",body.get("currency").asText());}
}
