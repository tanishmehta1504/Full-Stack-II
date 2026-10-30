package com.releasepilot;
import java.util.*;import java.io.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;import jakarta.servlet.http.HttpSession;import jakarta.validation.Valid;import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController public class ApiController {
 private final QuoteService quotes;private final JdbcTemplate db;private final RolloutService rollout;private final ObjectMapper json;
 private final String version,track,commit;private final boolean fail;
 public record Order(@Min(1) @Max(20) int quantity){} public record Allocation(int percent){} public record LoginRequest(String username, String password){}
 // Demo credentials — classroom lab only
 private static final String DEMO_USER = "admin";
 private static final String DEMO_PASS = "pilot123";
 public ApiController(QuoteService quotes,JdbcTemplate db,RolloutService rollout,ObjectMapper json,@Value("${app.version}")String version,@Value("${app.track}")String track,@Value("${app.commit}")String commit,@Value("${app.fail-quotes}")boolean fail){this.quotes=quotes;this.db=db;this.rollout=rollout;this.json=json;this.version=version;this.track=track;this.commit=commit;this.fail=fail;}
 @GetMapping("/api/info") public Map<String,Object> info(){return Map.of("name","ReleasePilot","version",version,"track",track,"commit",commit,"time",System.currentTimeMillis());}
 @PostMapping("/api/login") public Map<String,Object> login(@RequestBody LoginRequest req, HttpSession session){if(DEMO_USER.equals(req.username())&&DEMO_PASS.equals(req.password())){session.setAttribute("rp_user",req.username());return Map.of("ok",true,"username",req.username());}throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");}
 @PostMapping("/api/logout") public Map<String,Object> logout(HttpSession session){session.invalidate();return Map.of("ok",true);}
 @GetMapping("/api/health")public Map<String,String> health(){db.queryForObject("SELECT 1",Integer.class);return Map.of("status","UP");}
 @GetMapping("/api/quote")public QuoteService.Quote quote(@RequestParam(defaultValue="2")int quantity){if(fail)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Injected canary regression");return quotes.quote(quantity);}
 @PostMapping("/api/orders") @ResponseStatus(HttpStatus.CREATED)public Map<String,Object> order(@Valid @RequestBody Order request){var q=quote(request.quantity());String id=UUID.randomUUID().toString();db.update("INSERT INTO orders VALUES(?,?,?,?)",id,q.quantity(),q.total(),System.currentTimeMillis());return Map.of("id",id,"quantity",q.quantity(),"total",q.total(),"currency",q.currency());}
 @GetMapping("/api/evidence")public Object evidence()throws IOException{var file=new ClassPathResource("build-evidence.json");if(!file.exists())return Map.of("status","NOT_RECORDED","suites",List.of());try(var in=file.getInputStream()){return json.readValue(in,Object.class);}}
 @GetMapping("/api/rollout")public Map<String,Object> rollout(){return rollout.snapshot();}
 @PostMapping("/api/rollout")public Map<String,Object> change(@RequestBody Allocation a){rollout.change(a.percent());return rollout.snapshot();}
 @GetMapping("/api/canary/inject")public Map<String,Object> getInject(){return Map.of("inject",rollout.isInjectCanaryError());}
 @PostMapping("/api/canary/inject")public Map<String,Object> setInject(@RequestBody Map<String,Boolean> body){boolean inject=Boolean.TRUE.equals(body.get("inject"));rollout.setInjectCanaryError(inject);return Map.of("inject",inject,"message",inject?"Canary error injection ENABLED — canary-routed requests will return 503.":"Canary error injection DISABLED — canary is healthy.");}
 @GetMapping("/traffic/api/quote")public ResponseEntity<String> route(@RequestHeader(name="X-Canary-Key",required=false)String visitor,@RequestParam(defaultValue="2")int quantity,HttpServletRequest req){return rollout.forward(visitor==null?req.getRemoteAddr()+":"+req.getHeader("User-Agent"):visitor,quantity);}
 @ExceptionHandler(IllegalArgumentException.class)ResponseEntity<?> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(ResponseStatusException.class)ResponseEntity<?> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.requireNonNullElse(e.getReason(),"Request rejected")));}
}
