package com.postflow;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
/** Local-only lab. Custom mutation header prevents cross-origin HTML form submissions. No CORS is enabled. */
@Component
public class LocalApiFilter extends OncePerRequestFilter {
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws IOException,ServletException{
  res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("X-Frame-Options","DENY");
  res.setHeader("Content-Security-Policy","default-src 'self'; style-src 'self'; script-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
  if(req.getRequestURI().startsWith("/api/")){
   res.setHeader("Cache-Control","no-store");
   if(!"GET".equals(req.getMethod())&&!"postflow".equals(req.getHeader("X-Lab-Request"))){res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"message\":\"X-Lab-Request: postflow header required\"}");return;}
  }
  chain.doFilter(req,res);
 }
}
