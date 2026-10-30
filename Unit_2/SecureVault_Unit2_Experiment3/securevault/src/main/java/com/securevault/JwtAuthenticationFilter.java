package com.securevault;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Validates Bearer JWTs before Spring's authorization filters execute. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final Store store;
    public JwtAuthenticationFilter(JwtService jwt, Store store) { this.jwt=jwt; this.store=store; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) { return !request.getRequestURI().startsWith("/api/") || request.getRequestURI().startsWith("/api/auth/") || request.getRequestURI().equals("/api/csrf"); }
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null) {
            try {
                if (!header.startsWith("Bearer ")) throw new IllegalArgumentException("Invalid authorization scheme");
                var claims = jwt.validate(header.substring(7));
                var user = store.byId(claims.getSubject()).orElseThrow(() -> new IllegalArgumentException("Unknown account"));
                var auth = new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_"+user.role())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                res.setStatus(401); res.setContentType("application/json");
                res.getWriter().write("{\"message\":\"Invalid or expired access token\"}"); return;
            }
        }
        chain.doFilter(req,res);
    }
}
