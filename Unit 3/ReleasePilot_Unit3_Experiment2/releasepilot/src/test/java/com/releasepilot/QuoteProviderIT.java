package com.releasepilot;
import au.com.dius.pact.provider.junit5.*;import au.com.dius.pact.provider.junitsupport.Provider;import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.test.web.server.LocalServerPort;
@Provider("quote-api") @PactFolder("target/pacts")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="spring.datasource.url=jdbc:h2:mem:provider;DB_CLOSE_DELAY=-1")
class QuoteProviderIT {
 @LocalServerPort int port;
 @BeforeEach void target(PactVerificationContext context){context.setTarget(new HttpTestTarget("localhost",port));}
 @TestTemplate @ExtendWith(PactVerificationInvocationContextProvider.class) void verify(PactVerificationContext context){context.verifyInteraction();}
}
