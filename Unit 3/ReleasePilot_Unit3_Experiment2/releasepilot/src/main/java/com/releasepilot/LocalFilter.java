package com.releasepilot;
import java.io.IOException;import jakarta.servlet.*;import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;import org.springframework.web.filter.OncePerRequestFilter;
@Component public class LocalFilter extends OncePerRequestFilter {
 protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws IOException,ServletException{
  res.setHeader("Cache-Control","no-store");res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("X-Frame-Options","DENY");res.setHeader("Content-Security-Policy","default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'");
  String path=req.getServletPath();boolean isAuthEndpoint=path.equals("/api/login")||path.equals("/api/logout");  if(req.getMethod().equals("POST")&&!isAuthEndpoint&&!"releasepilot".equals(req.getHeader("X-Lab-Request"))){res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"message\":\"X-Lab-Request: releasepilot is required\"}");return;}chain.doFilter(req,res);
 }
}
