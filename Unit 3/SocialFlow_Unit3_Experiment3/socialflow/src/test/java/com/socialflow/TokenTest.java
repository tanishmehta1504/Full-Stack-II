package com.socialflow;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

class TokenTest {

  final String key = "c29jaWFsZmxvdy10ZXN0LW9ubHktcmFuZG9tLWtleS1kb250LXVzZQ==";

  @Test
  void rejectsMissingProductionKey() {
    assertThrows(IllegalStateException.class, () ->
      new Security(new JdbcTemplate(), "", false)
    );
  }

  @Test
  void rejectsShortSigningKey() {
    assertThrows(IllegalArgumentException.class, () ->
      new Security(new JdbcTemplate(), "YWJj", false)
    );
  }

  @Test
  void rejectsExpiredAndWrongIssuerTokens() throws Exception {
    var security = new Security(new JdbcTemplate(), key, false);
    for (String issuer : new String[] { "socialflow", "attacker" }) {
      var claims = JwtClaimsSet.builder()
        .issuer(issuer)
        .subject("test")
        .issuedAt(Instant.now().minusSeconds(3600))
        .expiresAt(
          issuer.equals("socialflow")
            ? Instant.now().minusSeconds(300)
            : Instant.now().plusSeconds(300)
        )
        .build();
      String token = security
        .encoder()
        .encode(
          JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(),
            claims
          )
        )
        .getTokenValue();
      assertThrows(JwtValidationException.class, () ->
        security.decoder().decode(token)
      );
    }
  }
}
