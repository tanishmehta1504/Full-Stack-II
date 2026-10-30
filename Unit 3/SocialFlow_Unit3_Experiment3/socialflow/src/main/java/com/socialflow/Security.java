package com.socialflow;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class Security {

  final byte[] key;
  final JdbcTemplate db;

  public Security(
    JdbcTemplate db,
    @Value("${app.jwt-secret}") String secret,
    @Value("${app.demo}") boolean demo
  ) throws Exception {
    this.db = db;
    if (secret.isBlank()) {
      if (!demo) throw new IllegalStateException(
        "JWT_SECRET required outside demo mode"
      );
      Path p = Path.of(".data/jwt.key");
      Files.createDirectories(p.getParent());
      if (!Files.exists(p)) {
        byte[] b = new byte[48];
        new java.security.SecureRandom().nextBytes(b);
        Files.writeString(p, Base64.getEncoder().encodeToString(b));
        try {
          Files.setPosixFilePermissions(
            p,
            java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")
          );
        } catch (UnsupportedOperationException ignored) {}
      }
      secret = Files.readString(p).trim();
    }
    key = Base64.getDecoder().decode(secret);
    if (key.length < 32) throw new IllegalArgumentException(
      "JWT key must contain at least 32 random bytes"
    );
  }

  @Bean
  BCryptPasswordEncoder passwords() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  JwtEncoder encoder() {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  JwtDecoder decoder() {
    var d = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256"))
      .macAlgorithm(MacAlgorithm.HS256)
      .build();
    d.setJwtValidator(JwtValidators.createDefaultWithIssuer("socialflow"));
    return d;
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity h) throws Exception {
    return h
      .csrf(c -> c.disable())
      .sessionManagement(s ->
        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .authorizeHttpRequests(a ->
        a
          .requestMatchers(
            "/api/login",
            "/api/health",
            "/",
            "/index.html",
            "/assets/**",
            "/favicon.svg",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
          )
          .permitAll()
          .anyRequest()
          .authenticated()
      )
      .oauth2ResourceServer(o ->
        o.jwt(j ->
          j.jwtAuthenticationConverter(jwt -> {
            var rows = db.queryForList(
              "SELECT role FROM members WHERE id=? AND active=TRUE",
              jwt.getSubject()
            );
            if (
              rows.isEmpty()
            ) throw new org.springframework.security.authentication.BadCredentialsException(
              "Account unavailable"
            );
            return new JwtAuthenticationToken(
              jwt,
              List.of(
                new SimpleGrantedAuthority("ROLE_" + rows.get(0).get("role"))
              ),
              jwt.getSubject()
            );
          })
        )
      )
      .headers(x ->
        x.contentSecurityPolicy(c ->
          c.policyDirectives(
            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'"
          )
        )
      )
      .build();
  }

  String token(String id) {
    var now = Instant.now();
    return encoder()
      .encode(
        JwtEncoderParameters.from(
          JwsHeader.with(MacAlgorithm.HS256).build(),
          JwtClaimsSet.builder()
            .issuer("socialflow")
            .subject(id)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(1800))
            .build()
        )
      )
      .getTokenValue();
  }
}
