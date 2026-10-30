package com.releasepilot;

import java.net.URI;import java.net.http.*;import java.time.Duration;import java.util.*;
import org.springframework.beans.factory.annotation.Value;import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.HttpStatus;import org.springframework.http.MediaType;import org.springframework.http.ResponseEntity;import org.springframework.web.server.ResponseStatusException;

@Service public class RolloutService {
 private final JdbcTemplate db;private final String stable,canary;private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
 public record Probe(boolean healthy,int status,long latencyMs){}
 private volatile Probe stableProbe=new Probe(false,0,0),canaryProbe=new Probe(false,0,0);
 private volatile boolean injectCanaryError=false;
 public boolean isInjectCanaryError(){return injectCanaryError;}
 public void setInjectCanaryError(boolean inject){this.injectCanaryError=inject;}
 public RolloutService(JdbcTemplate db,@Value("${app.stable-url}")String stable,@Value("${app.canary-url}")String canary){this.db=db;this.stable=stable;this.canary=canary;}
 public int percent(){return db.queryForObject("SELECT percent FROM rollout WHERE id=1",Integer.class);}
 public synchronized void change(int percent){
  if(!List.of(0,10,50,100).contains(percent))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose 0, 10, 50 or 100 percent");
  if(percent>0){
   stableProbe=probe(stable);canaryProbe=probe(canary);
   if(!stableProbe.healthy()||!canaryProbe.healthy())throw new ResponseStatusException(HttpStatus.CONFLICT,"Both releases must pass the quote probe first");
   int current=percent();
   if(percent>10){
    if(current==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Start with a 10 percent canary");
    long since=db.queryForObject("SELECT changed_at FROM rollout WHERE id=1",Long.class);
    int good=db.queryForObject("SELECT COUNT(*) FROM traffic WHERE track='canary' AND status=200 AND created_at>=?",Integer.class,since);
    int bad=db.queryForObject("SELECT COUNT(*) FROM traffic WHERE track='canary' AND status<>200 AND created_at>=?",Integer.class,since);
    if(good<5||bad>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Promotion needs at least 5 successful canary requests and zero errors in the current stage");
   }
  }
  set(percent,percent==0?"ROLLBACK":percent==100?"PROMOTED":"CANARY_STARTED","Canary allocation changed to "+percent+"%");
 }
 private void set(int percent,String action,String detail){db.update("UPDATE rollout SET percent=?,changed_at=? WHERE id=1",percent,System.currentTimeMillis());db.update("INSERT INTO release_events VALUES(?,?,?,?)",UUID.randomUUID().toString(),action,detail,System.currentTimeMillis());}
 public ResponseEntity<String> forward(String visitor,int quantity){
  if(quantity<1||quantity>20)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quantity must be between 1 and 20");
  String track=RoutingPolicy.select(visitor,percent());long start=System.nanoTime();int status;String body;
  if(track.equals("canary")&&injectCanaryError){
   status=503;body="{\"message\":\"Injected canary regression (lab simulation)\"}";
   long latency=(System.nanoTime()-start)/1000000;
   db.update("INSERT INTO traffic VALUES(?,?,?,?,?)",UUID.randomUUID().toString(),track,status,latency,System.currentTimeMillis());
   return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).header("X-Release-Track",track).body(body);
  }
  status=502;body="{\"message\":\"Upstream unavailable\"}";
  try{var response=client.send(HttpRequest.newBuilder(URI.create((track.equals("canary")?canary:stable)+"/api/quote?quantity="+quantity)).timeout(Duration.ofSeconds(3)).GET().build(),HttpResponse.BodyHandlers.ofString());status=response.statusCode();body=response.body();}
  catch(Exception ex){if(ex instanceof InterruptedException)Thread.currentThread().interrupt();}
  long latency=(System.nanoTime()-start)/1000000;
  db.update("INSERT INTO traffic VALUES(?,?,?,?,?)",UUID.randomUUID().toString(),track,status,latency,System.currentTimeMillis());
  return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).header("X-Release-Track",track).body(body);
 }
 private Probe probe(String url){long start=System.nanoTime();try{var r=client.send(HttpRequest.newBuilder(URI.create(url+"/api/quote?quantity=2")).timeout(Duration.ofSeconds(3)).GET().build(),HttpResponse.BodyHandlers.discarding());return new Probe(r.statusCode()==200,r.statusCode(),(System.nanoTime()-start)/1000000);}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();return new Probe(false,0,(System.nanoTime()-start)/1000000);}}
 @Scheduled(fixedDelay=5000) public synchronized void monitor(){
  stableProbe=probe(stable);canaryProbe=probe(canary);
  if(percent()>0){long since=db.queryForObject("SELECT changed_at FROM rollout WHERE id=1",Long.class);int errors=db.queryForObject("SELECT COUNT(*) FROM traffic WHERE track='canary' AND status<>200 AND created_at>=?",Integer.class,since);
   if(!canaryProbe.healthy()||errors>0)set(0,"AUTO_ROLLBACK","Canary health or routed request failed; stable traffic restored");
  }
 }
 public Map<String,Object> snapshot(){long since=db.queryForObject("SELECT changed_at FROM rollout WHERE id=1",Long.class);return Map.of("percent",percent(),"changedAt",since,"stable",stableProbe,"canary",canaryProbe,"injectCanaryError",injectCanaryError,
  "traffic",db.queryForList("SELECT track,COUNT(*) AS requests,SUM(CASE WHEN status<>200 THEN 1 ELSE 0 END) AS errors,AVG(latency_ms) AS latency FROM traffic WHERE created_at>=? GROUP BY track",since),
  "events",db.queryForList("SELECT * FROM release_events ORDER BY created_at DESC LIMIT 30"));}
}
