package com.securevault;

import org.springframework.context.annotation.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    // Register exactly once, inside the Spring Security chain.
    @Bean FilterRegistrationBean<JwtAuthenticationFilter> registration(JwtAuthenticationFilter filter) {
        var bean = new FilterRegistrationBean<>(filter); bean.setEnabled(false); return bean;
    }
    @Bean SecurityFilterChain security(HttpSecurity http, JwtAuthenticationFilter filter) throws Exception {
        // Bearer authentication is per request; do not run session-login strategies on it.
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(c -> c.sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()).csrfTokenRepository(new CookieCsrfTokenRepository()).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(a -> a.requestMatchers("/", "/index.html", "/style.css", "/app.js", "/favicon.svg", "/api/csrf", "/api/auth/**", "/error").permitAll().anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> {
                res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Sign in to access this API\"}");
            }).accessDeniedHandler((req,res,ex) -> {
                res.setStatus(403); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Access denied: permission or CSRF check failed\"}");
            }))
            .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
