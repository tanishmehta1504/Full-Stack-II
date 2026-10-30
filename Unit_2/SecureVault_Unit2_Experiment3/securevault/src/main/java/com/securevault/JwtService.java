package com.securevault;

import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    final long lifetime;
    public JwtService(KeyMaterial keys, @Value("${app.access-seconds}") long lifetime) {
        this.lifetime = lifetime;
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(keys.jwt));
        decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(keys.jwt,"HmacSHA256")).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains("securevault-api") && "access".equals(jwt.getClaimAsString("token_use"))
            ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        JwtTimestampValidator timestamp = new JwtTimestampValidator(Duration.ZERO);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp, new JwtIssuerValidator("securevault"), audience));
    }
    public String issue(Store.User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("securevault").subject(user.id()).audience(List.of("securevault-api"))
            .issuedAt(now).expiresAt(now.plusSeconds(lifetime)).id(UUID.randomUUID().toString())
            .claim("role",user.role()).claim("token_use","access").build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
    }
    public Jwt validate(String token) { return decoder.decode(token); }
}
